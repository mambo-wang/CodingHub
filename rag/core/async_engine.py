"""Async document processing engine with concurrency control.

Uses asyncio.Task + Semaphore to process multiple files in parallel
while limiting resource usage. Each file goes through the pipeline:
CONVERTING → CHUNKING → EMBEDDING → READY/FAILED.
"""

import asyncio
import logging
import os
from typing import Callable, Optional

from core.database import (
    Database,
    STATUS_CONVERTING,
    STATUS_CHUNKING,
    STATUS_EMBEDDING,
    STATUS_READY,
    STATUS_FAILED,
)

logger = logging.getLogger(__name__)

# Max concurrent file processing tasks
# Default 1: on CPU-only deployments, concurrent embedding causes resource
# contention and potential zvec deadlocks. Set higher only with GPU.
MAX_CONCURRENT = int(os.getenv("RAG_MAX_CONCURRENT", "1"))

# Per-document processing timeout in seconds (default 10 minutes)
PROCESS_TIMEOUT = int(os.getenv("RAG_PROCESS_TIMEOUT", "600"))


class AsyncEngine:
    """Async document processing engine with Semaphore-based concurrency control.

    Manages asyncio.Tasks for parallel file processing, limiting the number
    of simultaneous tasks to avoid resource exhaustion (OOM, CPU saturation).
    """

    def __init__(self, db: Database, max_concurrent: int = MAX_CONCURRENT):
        self.db = db
        self.semaphore = asyncio.Semaphore(max_concurrent)
        self._tasks: dict[int, asyncio.Task] = {}
        logger.info(f"AsyncEngine initialized with max_concurrent={max_concurrent}")

    def submit_tasks(self, doc_entries: list[dict]):
        """Submit multiple documents for async processing.

        Args:
            doc_entries: List of dicts with keys:
                id: document ID in SQLite
                filepath: path to the uploaded file
                collection: target collection name
                filename: original filename
                file_size: file size in bytes
        """
        for entry in doc_entries:
            task = asyncio.create_task(
                self._process_with_semaphore(entry),
                name=f"process-doc-{entry['id']}-{entry['filename']}",
            )
            self._tasks[entry["id"]] = task
            task.add_done_callback(
                lambda t, doc_id=entry["id"]: self._on_task_done(doc_id, t)
            )

        logger.info(
            f"Submitted {len(doc_entries)} tasks "
            f"(active: {len([t for t in self._tasks.values() if not t.done()])})"
        )

    def _on_task_done(self, doc_id: int, task: asyncio.Task):
        """Callback when a processing task completes."""
        self._tasks.pop(doc_id, None)
        if task.exception():
            logger.error(f"Task for doc {doc_id} failed: {task.exception()}")

    async def _process_with_semaphore(self, entry: dict):
        """Process a single file within the semaphore concurrency limit."""
        doc_id = entry["id"]
        filename = entry.get("filename", "?")
        async with self.semaphore:
            try:
                await asyncio.wait_for(
                    self._process_single_file(entry),
                    timeout=PROCESS_TIMEOUT,
                )
            except asyncio.TimeoutError:
                logger.error(
                    f"Document {doc_id} ({filename}) timed out after "
                    f"{PROCESS_TIMEOUT}s"
                )
                self.db.update_status(
                    doc_id, STATUS_FAILED,
                    error_message=f"处理超时（{PROCESS_TIMEOUT}s），文件过大或系统资源不足",
                )

    async def _process_single_file(self, entry: dict):
        """Process a single file through the full pipeline.

        Pipeline stages:
        1. CONVERTING: Read and convert file content (markitdown for binary)
        2. CHUNKING: Split text into chunks
        3. EMBEDDING: Generate vectors and store in zvec
        4. READY or FAILED: Final status
        """
        doc_id = entry["id"]
        filepath = entry["filepath"]
        collection = entry["collection"]
        filename = entry["filename"]

        try:
            # Stage 1: CONVERTING — read file and convert to text
            self.db.update_status(doc_id, STATUS_CONVERTING)

            content, error = await asyncio.to_thread(
                _read_file_content, filepath
            )
            if error:
                self.db.update_status(
                    doc_id, STATUS_FAILED, error_message=f"格式转换失败：{error}"
                )
                return

            # Stage 2: CHUNKING — split text into chunks
            self.db.update_status(doc_id, STATUS_CHUNKING)

            chunks = await asyncio.to_thread(
                _chunk_text, content, filepath, collection
            )
            if not chunks:
                self.db.update_status(
                    doc_id, STATUS_FAILED, error_message="分块失败：未生成任何文本块"
                )
                return

            # Stage 3: EMBEDDING — generate vectors and store
            self.db.update_status(doc_id, STATUS_EMBEDDING)

            chunk_count = await asyncio.to_thread(
                _ingest_chunks, chunks, collection, filepath
            )

            # Stage 4: READY
            self.db.update_status(
                doc_id, STATUS_READY, chunk_count=chunk_count
            )
            logger.info(
                f"Document {doc_id} ({filename}) processed: "
                f"{chunk_count} chunks"
            )

        except Exception as e:
            logger.error(f"Document {doc_id} ({filename}) processing failed: {e}")
            self.db.update_status(
                doc_id, STATUS_FAILED, error_message=str(e)
            )


# ── Pipeline stage functions (run in thread pool) ────────────

def _read_file_content(filepath: str) -> tuple[str | None, str | None]:
    """Read and convert file content to text.

    Returns (content, error) tuple.
    """
    from core.service import read_file_content
    return read_file_content(filepath)


def _chunk_text(content: str, filepath: str, collection: str) -> list:
    """Split text content into chunks using the collection's configured strategy.

    Returns list of Chunk objects.
    """
    from core.service import _resolve_config, get_store
    from core.chunker import chunk_text, semantic_chunk_text, structural_chunk_text

    config = _resolve_config(collection)
    chunk_size = config["chunk_size"]
    chunk_mode = config["chunk_mode"]
    chunk_overlap = config.get("chunk_overlap", 50)

    # Delete existing chunks for idempotent re-import
    store = get_store()
    store.delete_document(filepath, collection=collection)

    if chunk_mode == "semantic":
        return semantic_chunk_text(content, filepath=filepath, chunk_size=chunk_size)
    elif chunk_mode == "structural":
        return structural_chunk_text(content, filepath=filepath, chunk_size=chunk_size)
    else:
        return chunk_text(content, filepath=filepath, chunk_size=chunk_size,
                         chunk_overlap=chunk_overlap)


def _ingest_chunks(chunks: list, collection: str, filepath: str) -> int:
    """Ingest chunks into the vector store and register the document.

    Returns the number of chunks ingested.
    """
    from core.service import get_store, _compute_file_hash

    store = get_store()
    count = store.ingest_chunks(chunks, collection=collection)

    # Register in _registry.json (backward compat)
    try:
        file_hash = _compute_file_hash(filepath)
    except Exception:
        file_hash = None
    store.register_document(
        filepath, chunk_count=count, collection=collection,
        file_hash=file_hash,
    )

    return count
