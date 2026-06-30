"""REST API for wandering-rag-mcp knowledge base management.

Provides HTTP endpoints for document CRUD and semantic search,
designed to be called from web frontends (e.g. CodingHub).
Uses starlette directly — no FastAPI dependency needed.
"""

import asyncio
import logging
import os

from starlette.middleware import Middleware
from starlette.middleware.cors import CORSMiddleware
from starlette.requests import Request
from starlette.responses import FileResponse, JSONResponse
from starlette.routing import Route

from core import service
from core.database import Database, MAX_BATCH_FILES, TERMINAL_STATUSES

logger = logging.getLogger(__name__)


# ── Module-level state (initialized on app startup) ─────────
_db: Database | None = None
_async_engine = None
_pending_recovery: list[dict] = []


def init_db_and_engine(data_dir: str):
    """Initialize the Database and AsyncEngine. Called at app startup."""
    global _db, _async_engine, _pending_recovery
    _db = Database(data_dir)
    _db.init_db()
    recovered = _db.mark_stale_as_failed()
    from core.async_engine import AsyncEngine
    _async_engine = AsyncEngine(_db)
    # Defer re-submission until the event loop is running
    if recovered:
        _pending_recovery = recovered
    return _db, _async_engine


def _flush_recovery():
    """Submit recovered stale documents to the async engine.

    Called lazily on the first API request when the event loop is active.
    """
    global _pending_recovery
    if _pending_recovery and _async_engine:
        _async_engine.submit_tasks(_pending_recovery)
        _pending_recovery = []


def get_db() -> Database:
    if _db is None:
        raise RuntimeError("Database not initialized. Call init_db_and_engine() first.")
    return _db


def get_async_engine():
    if _async_engine is None:
        raise RuntimeError("AsyncEngine not initialized. Call init_db_and_engine() first.")
    return _async_engine


# ── Helpers ──────────────────────────────────────────────────

def _fix_filename_encoding(filename: str) -> str:
    """Fix mojibake filenames caused by encoding mismatches.

    Common issue: curl on Chinese Windows sends filenames in GBK,
    but the HTTP multipart parser interprets raw bytes as Latin-1,
    resulting in garbled names like 'ÊÛÇ°¼¼ÊõÖ¸ÄÏ'.
    This function tries to detect and fix such cases.
    """
    # Quick check: if all chars are ASCII or common CJK, likely fine
    try:
        if all(ord(c) < 128 or '\u4e00' <= c <= '\u9fff' or c in '（）()' for c in filename):
            return filename
    except Exception:
        pass

    # Try Latin-1 → GBK (Chinese Windows curl)
    try:
        raw_bytes = filename.encode("latin-1")
        fixed = raw_bytes.decode("gbk")
        if fixed != filename:
            logger.info(f"Fixed filename encoding: '{filename}' → '{fixed}'")
            return fixed
    except (UnicodeDecodeError, UnicodeEncodeError):
        pass

    # Try Latin-1 → UTF-8 (some clients)
    try:
        raw_bytes = filename.encode("latin-1")
        fixed = raw_bytes.decode("utf-8")
        if fixed != filename:
            logger.info(f"Fixed filename encoding (UTF-8): '{filename}' → '{fixed}'")
            return fixed
    except (UnicodeDecodeError, UnicodeEncodeError):
        pass

    return filename


def _json(data, status_code: int = 200):
    return JSONResponse(content=data, status_code=status_code)


def _error(message: str, status_code: int = 400):
    return JSONResponse(content={"error": message}, status_code=status_code)


def _get_collection(request: Request) -> str:
    return request.path_params.get("name", "default")


# ── Route Handlers ───────────────────────────────────────────

async def health(request: Request):
    """GET /api/health — health check."""
    _flush_recovery()
    return _json({"status": "ok", "service": "wandering-rag-mcp"})


async def list_collections(request: Request):
    """GET /api/collections — list all collections."""
    try:
        result = service.list_collections()
        return _json(result)
    except Exception as e:
        logger.error(f"list_collections failed: {e}")
        return _error(str(e), 500)


async def list_documents(request: Request):
    """GET /api/collections/{name}/documents — list documents."""
    collection = _get_collection(request)
    try:
        result = service.list_documents(collection=collection)
        return _json(result)
    except Exception as e:
        logger.error(f"list_documents failed: {e}")
        return _error(str(e), 500)


async def upload_document(request: Request):
    """POST /api/collections/{name}/documents — upload a file.

    Accepts multipart/form-data with a 'file' field.
    Optional query params:
      chunk_size (int, default 500) - max characters per chunk.
      chunk_mode (str, default "recursive") - "recursive" or "semantic".
    """
    collection = _get_collection(request)
    chunk_size_raw = request.query_params.get("chunk_size")
    chunk_mode_raw = request.query_params.get("chunk_mode")
    chunk_size = int(chunk_size_raw) if chunk_size_raw else None
    chunk_mode = chunk_mode_raw if chunk_mode_raw else None

    try:
        form = await request.form()
    except Exception:
        return _error("Invalid multipart form data. Did you forget python-multipart?", 400)

    upload = form.get("file")
    if upload is None:
        return _error("Missing 'file' field in multipart form", 400)

    filename = _fix_filename_encoding(upload.filename or "unnamed")

    # Read file content
    try:
        raw = await upload.read()
    except Exception as e:
        return _error(f"Failed to read uploaded file: {e}", 500)

    # Determine content type and process
    ext = os.path.splitext(filename)[1].lower()

    if ext in service.BINARY_EXTENSIONS:
        # Binary file: save temporarily, convert with markitdown
        store = service.get_store()
        upload_dir = os.path.join(store.data_dir, "_uploads", collection)
        os.makedirs(upload_dir, exist_ok=True)
        tmp_path = os.path.join(upload_dir, filename)

        with open(tmp_path, "wb") as f:
            f.write(raw)

        result = await asyncio.to_thread(
            service.ingest_file, tmp_path,
            collection=collection, chunk_size=chunk_size, chunk_mode=chunk_mode,
        )
    else:
        # Text file: decode and ingest directly
        try:
            content = raw.decode("utf-8", errors="replace")
        except Exception as e:
            return _error(f"Failed to decode file as UTF-8: {e}", 400)

        result = await asyncio.to_thread(
            service.ingest_content,
            content, filename,
            collection=collection, chunk_size=chunk_size, chunk_mode=chunk_mode,
        )

    if result.get("status") == "error":
        return _error(result["error"], 422)

    return _json(result, 201)


async def delete_document(request: Request):
    """DELETE /api/collections/{name}/documents — delete a document.

    Expects JSON body: {"filepath": "..."}
    """
    collection = _get_collection(request)

    try:
        body = await request.json()
    except Exception:
        return _error("Invalid JSON body", 400)

    filepath = body.get("filepath")
    if not filepath:
        return _error("Missing 'filepath' in request body", 400)

    try:
        result = service.delete_document(filepath, collection=collection)
        return _json(result)
    except Exception as e:
        logger.error(f"delete_document failed: {e}")
        return _error(str(e), 500)


async def download_document(request: Request):
    """GET /api/collections/{name}/documents/download — download source file.

    Query params:
      filepath (required): Path to the file in _uploads/{collection}/.
    Returns the file as an attachment (Content-Disposition: attachment).
    """
    collection = _get_collection(request)
    filepath = request.query_params.get("filepath")

    if not filepath:
        return _error("Missing 'filepath' query parameter", 400)

    try:
        real_path = service.download_document(filepath, collection=collection)
    except ValueError as e:
        logger.warning(f"download_document rejected: {e}")
        return _error(str(e), 403)
    except FileNotFoundError as e:
        return _error(str(e), 404)
    except Exception as e:
        logger.error(f"download_document failed: {e}")
        return _error(str(e), 500)

    filename = os.path.basename(real_path)
    # RFC 5987: use filename*=UTF-8''<url-encoded> for non-ASCII filenames
    from urllib.parse import quote
    encoded_filename = quote(filename, safe='')
    return FileResponse(
        real_path,
        media_type="application/octet-stream",
        headers={
            "Content-Disposition": f"attachment; filename*=UTF-8''{encoded_filename}"
        },
    )


async def delete_collection(request: Request):
    """DELETE /api/collections/{name} — delete an entire collection.

    Permanently removes the collection including all documents,
    vectors, and configuration.
    """
    collection = _get_collection(request)

    try:
        result = service.delete_collection(collection)
        if result.get("status") == "error":
            return _error(result["error"], 404)
        return _json(result)
    except Exception as e:
        logger.error(f"delete_collection failed: {e}")
        return _error(str(e), 500)


async def search_documents(request: Request):
    """POST /api/collections/{name}/search — semantic search.

    Expects JSON body: {"query": "...", "top_k": 5, "rerank": false,
                        "filter": "*.md", "expand_context": 0}
    Fields not included in the body use the collection config default.
    """
    collection = _get_collection(request)

    try:
        body = await request.json()
    except Exception:
        return _error("Invalid JSON body", 400)

    query = body.get("query")
    if not query:
        return _error("Missing 'query' in request body", 400)

    top_k = int(body.get("top_k", 5))
    rerank_val = body.get("rerank")
    rerank = bool(rerank_val) if rerank_val is not None else None
    filter_pattern = body.get("filter", "")
    expand_context = int(body.get("expand_context", 0))

    try:
        results = service.search(
            query=query, top_k=top_k,
            collection=collection, rerank=rerank,
            filter=filter_pattern,
            expand_context=expand_context,
        )
        return _json(results)
    except Exception as e:
        logger.error(f"search failed: {e}")
        return _error(str(e), 500)


# ── Collection Config Endpoints ──────────────────────────────

async def get_config(request: Request):
    """GET /api/collections/{name}/config — get collection configuration."""
    collection = _get_collection(request)
    try:
        config = service.get_collection_config(collection)
        return _json(config)
    except Exception as e:
        logger.error(f"get_config failed: {e}")
        return _error(str(e), 500)


async def update_config(request: Request):
    """PUT /api/collections/{name}/config — update collection configuration.

    Expects JSON body with optional fields:
    {"chunk_mode": "semantic", "chunk_size": 500, "chunk_overlap": 50,
     "rerank": true, "description": "My knowledge base"}
    Only included fields are updated; omitted fields keep their current value.
    """
    collection = _get_collection(request)

    try:
        body = await request.json()
    except Exception:
        return _error("Invalid JSON body", 400)

    try:
        config = service.set_collection_config(
            collection=collection,
            chunk_mode=body.get("chunk_mode"),
            chunk_size=body.get("chunk_size"),
            chunk_overlap=body.get("chunk_overlap"),
            rerank=body.get("rerank"),
            description=body.get("description"),
        )
        return _json(config)
    except Exception as e:
        logger.error(f"update_config failed: {e}")
        return _error(str(e), 500)


# ── Batch Upload & Status Endpoints ──────────────────────────

async def batch_upload_documents(request: Request):
    """POST /api/collections/{name}/documents/batch — batch upload files.

    Accepts multipart/form-data with multiple 'files' fields.
    Returns 202 Accepted with document IDs and initial UPLOADING status.
    Files are processed asynchronously in the background.
    """
    collection = _get_collection(request)

    try:
        form = await request.form()
    except Exception:
        return _error("Invalid multipart form data. Did you forget python-multipart?", 400)

    # Collect all uploaded files
    uploads = form.getlist("files")
    if not uploads:
        # Also try "file" for backward compat with single-file clients
        single = form.get("file")
        if single:
            uploads = [single]

    if not uploads:
        return _error("至少需要上传 1 个文件", 400)

    if len(uploads) > MAX_BATCH_FILES:
        return _error(f"单次最多上传 {MAX_BATCH_FILES} 个文件，当前 {len(uploads)} 个", 400)

    db = get_db()
    engine = get_async_engine()
    store = service.get_store()
    upload_dir = os.path.join(store.data_dir, "_uploads", collection)
    os.makedirs(upload_dir, exist_ok=True)

    doc_entries = []

    for upload in uploads:
        filename = _fix_filename_encoding(upload.filename or "unnamed")
        filepath = os.path.join(upload_dir, filename)

        try:
            raw = await upload.read()
        except Exception as e:
            # Skip files that fail to read, record error
            doc_entries.append({
                "filename": filename,
                "status": "error",
                "error": f"Failed to read uploaded file: {e}",
            })
            continue

        file_size = len(raw)
        with open(filepath, "wb") as f:
            f.write(raw)

        # Resolve chunk_mode from collection config
        config = service._resolve_config(collection)
        chunk_mode = config.get("chunk_mode")

        # Insert into SQLite with UPLOADING status
        doc_id = db.insert_document(
            collection=collection,
            filename=filename,
            filepath=filepath,
            file_size=file_size,
            chunk_mode=chunk_mode,
        )

        doc_entries.append({
            "id": doc_id,
            "filename": filename,
            "filepath": filepath,
            "collection": collection,
            "status": "UPLOADING",
            "file_size": file_size,
        })

    # Submit all valid entries for async processing
    valid_entries = [e for e in doc_entries if "id" in e]
    if valid_entries:
        engine.submit_tasks(valid_entries)

    return _json(doc_entries, 202)


async def get_documents_status(request: Request):
    """GET /api/collections/{name}/documents/status — query all document statuses.

    Returns list of documents with their processing status, sorted by created_at DESC.
    """
    collection = _get_collection(request)
    try:
        db = get_db()
        documents = db.get_documents(collection)
        return _json(documents)
    except Exception as e:
        logger.error(f"get_documents_status failed: {e}")
        return _error(str(e), 500)


async def get_single_document_status(request: Request):
    """GET /api/collections/{name}/documents/{doc_id}/status — query single document.

    Returns detailed document status including error_message if FAILED.
    """
    collection = _get_collection(request)
    doc_id_str = request.path_params.get("doc_id")

    try:
        doc_id = int(doc_id_str)
    except (ValueError, TypeError):
        return _error(f"Invalid document ID: {doc_id_str}", 400)

    try:
        db = get_db()
        doc = db.get_document_by_id(doc_id)
        if doc is None:
            return _error(f"Document not found: {doc_id}", 404)
        if doc["collection"] != collection:
            return _error(f"Document {doc_id} not in collection {collection}", 404)
        return _json(doc)
    except Exception as e:
        logger.error(f"get_single_document_status failed: {e}")
        return _error(str(e), 500)


# ── Router ───────────────────────────────────────────────────

def create_api_routes() -> list[Route]:
    """Create and return the list of API routes."""
    return [
        Route("/api/health", health, methods=["GET"]),
        Route("/api/collections", list_collections, methods=["GET"]),
        # Batch upload (must come before generic documents route)
        Route("/api/collections/{name}/documents/batch", batch_upload_documents, methods=["POST"]),
        # Status endpoints (must come before generic documents route)
        Route("/api/collections/{name}/documents/status", get_documents_status, methods=["GET"]),
        Route("/api/collections/{name}/documents/{doc_id}/status", get_single_document_status, methods=["GET"]),
        # Original document endpoints
        Route("/api/collections/{name}/documents", list_documents, methods=["GET"]),
        Route("/api/collections/{name}/documents", upload_document, methods=["POST"]),
        Route("/api/collections/{name}/documents", delete_document, methods=["DELETE"]),
        Route("/api/collections/{name}/documents/download", download_document, methods=["GET"]),
        Route("/api/collections/{name}", delete_collection, methods=["DELETE"]),
        Route("/api/collections/{name}/search", search_documents, methods=["POST"]),
        Route("/api/collections/{name}/config", get_config, methods=["GET"]),
        Route("/api/collections/{name}/config", update_config, methods=["PUT"]),
    ]


def get_cors_middleware() -> Middleware:
    """Return CORS middleware configured for web frontend access."""
    allowed_origins = os.getenv("RAG_CORS_ORIGINS", "*")
    origins = [o.strip() for o in allowed_origins.split(",")]

    return Middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        allow_headers=["Content-Type", "Authorization"],
        allow_credentials=True,
    )
