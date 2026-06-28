"""SQLite database layer for document metadata and status tracking.

Stores document processing state (UPLOADING → CONVERTING → CHUNKING →
EMBEDDING → READY / FAILED) in a local SQLite database with WAL mode
for concurrent read/write support.
"""

import asyncio
import logging
import os
import sqlite3
import threading
from datetime import datetime
from typing import Optional

logger = logging.getLogger(__name__)

# Document status constants
STATUS_UPLOADING = "UPLOADING"
STATUS_CONVERTING = "CONVERTING"
STATUS_CHUNKING = "CHUNKING"
STATUS_EMBEDDING = "EMBEDDING"
STATUS_READY = "READY"
STATUS_FAILED = "FAILED"

INTERMEDIATE_STATUSES = (STATUS_UPLOADING, STATUS_CONVERTING, STATUS_CHUNKING, STATUS_EMBEDDING)
TERMINAL_STATUSES = (STATUS_READY, STATUS_FAILED)

# Max files per batch upload
MAX_BATCH_FILES = 20

# Default data directory
DEFAULT_DATA_DIR = os.getenv("RAG_DATA_DIR", "./data/")


class Database:
    """SQLite database manager for document metadata.

    Uses WAL mode for concurrent read/write support.
    Database file is stored at {data_dir}/documents.db.
    """

    def __init__(self, data_dir: str = DEFAULT_DATA_DIR):
        self.data_dir = data_dir
        self.db_path = os.path.join(data_dir, "documents.db")
        self._local = threading.local()

    def _get_conn(self) -> sqlite3.Connection:
        """Get thread-local database connection with WAL mode."""
        if not hasattr(self._local, 'conn') or self._local.conn is None:
            os.makedirs(self.data_dir, exist_ok=True)
            conn = sqlite3.connect(self.db_path, timeout=30)
            conn.execute("PRAGMA journal_mode=WAL")
            conn.execute("PRAGMA busy_timeout=5000")
            conn.row_factory = sqlite3.Row
            self._local.conn = conn
        return self._local.conn

    def init_db(self):
        """Create tables and indexes if they don't exist."""
        conn = self._get_conn()
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS documents (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                collection TEXT NOT NULL,
                filename TEXT NOT NULL,
                filepath TEXT NOT NULL,
                file_size INTEGER DEFAULT 0,
                uploader TEXT,
                status TEXT NOT NULL DEFAULT 'UPLOADING',
                chunk_count INTEGER DEFAULT 0,
                chunk_mode TEXT,
                error_message TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(collection, filepath)
            );

            CREATE INDEX IF NOT EXISTS idx_doc_collection_status
            ON documents(collection, status);

            CREATE INDEX IF NOT EXISTS idx_doc_created
            ON documents(collection, created_at DESC);
        """)
        conn.commit()
        logger.info(f"Database initialized at {self.db_path}")

    def insert_document(
        self,
        collection: str,
        filename: str,
        filepath: str,
        file_size: int = 0,
        uploader: str | None = None,
        chunk_mode: str | None = None,
    ) -> int:
        """Insert a new document record with UPLOADING status.

        Returns the document ID.
        """
        conn = self._get_conn()
        now = datetime.now().isoformat()
        try:
            cursor = conn.execute(
                """INSERT INTO documents
                   (collection, filename, filepath, file_size, uploader, status, chunk_mode, created_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (collection, filename, filepath, file_size, uploader,
                 STATUS_UPLOADING, chunk_mode, now, now),
            )
            conn.commit()
            return cursor.lastrowid
        except sqlite3.IntegrityError:
            # Duplicate filepath in collection — update existing record
            conn.execute(
                """UPDATE documents SET status=?, file_size=?, uploader=?,
                   chunk_count=0, error_message=NULL, updated_at=?
                   WHERE collection=? AND filepath=?""",
                (STATUS_UPLOADING, file_size, uploader, now, collection, filepath),
            )
            conn.commit()
            row = conn.execute(
                "SELECT id FROM documents WHERE collection=? AND filepath=?",
                (collection, filepath),
            ).fetchone()
            return row["id"]

    def update_status(
        self,
        doc_id: int,
        status: str,
        chunk_count: int | None = None,
        error_message: str | None = None,
    ):
        """Update document status and optional metadata."""
        conn = self._get_conn()
        now = datetime.now().isoformat()
        if chunk_count is not None:
            conn.execute(
                """UPDATE documents SET status=?, chunk_count=?, error_message=?,
                   updated_at=? WHERE id=?""",
                (status, chunk_count, error_message, now, doc_id),
            )
        else:
            conn.execute(
                "UPDATE documents SET status=?, error_message=?, updated_at=? WHERE id=?",
                (status, error_message, now, doc_id),
            )
        conn.commit()

    def get_documents(
        self,
        collection: str,
        status: str | None = None,
    ) -> list[dict]:
        """Get all documents in a collection, optionally filtered by status.

        Returns list sorted by created_at DESC.
        """
        conn = self._get_conn()
        if status:
            rows = conn.execute(
                """SELECT id, collection, filename, filepath, file_size, uploader,
                   status, chunk_count, chunk_mode, error_message, created_at, updated_at
                   FROM documents WHERE collection=? AND status=?
                   ORDER BY created_at DESC""",
                (collection, status),
            ).fetchall()
        else:
            rows = conn.execute(
                """SELECT id, collection, filename, filepath, file_size, uploader,
                   status, chunk_count, chunk_mode, error_message, created_at, updated_at
                   FROM documents WHERE collection=?
                   ORDER BY created_at DESC""",
                (collection,),
            ).fetchall()
        return [dict(row) for row in rows]

    def get_document_by_id(self, doc_id: int) -> dict | None:
        """Get a single document by ID."""
        conn = self._get_conn()
        row = conn.execute(
            """SELECT id, collection, filename, filepath, file_size, uploader,
               status, chunk_count, chunk_mode, error_message, created_at, updated_at
               FROM documents WHERE id=?""",
            (doc_id,),
        ).fetchone()
        return dict(row) if row else None

    def get_document_by_filepath(self, collection: str, filepath: str) -> dict | None:
        """Get a document by collection and filepath."""
        conn = self._get_conn()
        row = conn.execute(
            """SELECT id, collection, filename, filepath, file_size, uploader,
               status, chunk_count, chunk_mode, error_message, created_at, updated_at
               FROM documents WHERE collection=? AND filepath=?""",
            (collection, filepath),
        ).fetchone()
        return dict(row) if row else None

    def delete_document(self, doc_id: int):
        """Delete a document record."""
        conn = self._get_conn()
        conn.execute("DELETE FROM documents WHERE id=?", (doc_id,))
        conn.commit()

    def delete_by_filepath(self, collection: str, filepath: str):
        """Delete a document record by filepath."""
        conn = self._get_conn()
        conn.execute(
            "DELETE FROM documents WHERE collection=? AND filepath=?",
            (collection, filepath),
        )
        conn.commit()

    def mark_stale_as_failed(self):
        """Mark all intermediate-status documents as FAILED on startup.

        Called during service startup to handle documents that were being
        processed when the service was last shut down.
        Documents whose source files still exist are reset to UPLOADING
        for automatic reprocessing; only documents with missing files
        are marked as FAILED.

        Returns:
            List of document entries (dicts) that should be re-submitted
            to the async engine for reprocessing.
        """
        conn = self._get_conn()
        now = datetime.now().isoformat()
        placeholders = ",".join("?" for _ in INTERMEDIATE_STATUSES)

        # Find all stale documents in intermediate states
        rows = conn.execute(
            f"""SELECT id, collection, filename, filepath, file_size
                FROM documents WHERE status IN ({placeholders})""",
            (*INTERMEDIATE_STATUSES,),
        ).fetchall()

        to_retry = []
        to_fail = []

        for row in rows:
            doc = dict(row)
            if doc["filepath"] and os.path.isfile(doc["filepath"]):
                to_retry.append(doc)
            else:
                to_fail.append(doc["id"])

        # Reset retryable docs to UPLOADING
        for doc in to_retry:
            conn.execute(
                "UPDATE documents SET status=?, error_message=NULL, updated_at=? WHERE id=?",
                (STATUS_UPLOADING, now, doc["id"]),
            )

        # Mark docs with missing files as FAILED
        if to_fail:
            fail_placeholders = ",".join("?" for _ in to_fail)
            conn.execute(
                f"""UPDATE documents SET status=?, error_message=?, updated_at=?
                    WHERE id IN ({fail_placeholders})""",
                (STATUS_FAILED, "服务重启，源文件已丢失", now, *to_fail),
            )

        conn.commit()

        if to_retry:
            logger.info(f"Recovered {len(to_retry)} stale documents for reprocessing")
        if to_fail:
            logger.info(f"Marked {len(to_fail)} stale documents as FAILED (source missing)")

        # Build entries compatible with AsyncEngine.submit_tasks
        return [
            {
                "id": d["id"],
                "filepath": d["filepath"],
                "collection": d["collection"],
                "filename": d["filename"],
                "file_size": d.get("file_size", 0),
            }
            for d in to_retry
        ]

    def get_processing_count(self, collection: str) -> int:
        """Count documents in intermediate (processing) states."""
        conn = self._get_conn()
        placeholders = ",".join("?" for _ in INTERMEDIATE_STATUSES)
        row = conn.execute(
            f"""SELECT COUNT(*) as cnt FROM documents
                WHERE collection=? AND status IN ({placeholders})""",
            (collection, *INTERMEDIATE_STATUSES),
        ).fetchone()
        return row["cnt"]

