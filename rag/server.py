"""RAG Knowledge Base REST API Server.

A local RAG (Retrieval-Augmented Generation) knowledge base service
that uses zvec for vector storage and Qwen3-Embedding for text embedding.

Exposes a REST API for web frontend and Java backend integration.
MCP tools are served by the Java backend (h3_coding_hub_kb_* tools),
which proxies to this service via REST HTTP.

REST API:
  - GET  /api/health
  - GET  /api/collections
  - GET  /api/collections/{name}/documents
  - POST /api/collections/{name}/documents         (single file upload, sync)
  - POST /api/collections/{name}/documents/batch   (batch upload, async)
  - GET  /api/collections/{name}/documents/status  (document status list)
  - GET  /api/collections/{name}/documents/{id}/status (single doc status)
  - DELETE /api/collections/{name}/documents
  - DELETE /api/collections/{name}
  - POST /api/collections/{name}/search
  - GET  /api/collections/{name}/config
  - PUT  /api/collections/{name}/config
  - POST /api/collections/{name}/chunking/preview
"""

import argparse
import logging
import os
import sys

# Route HuggingFace downloads through a mirror. huggingface_hub caches
# HF_ENDPOINT as a constant at import time, so it must be set before any
# huggingface-related import happens.
_current_ep = os.environ.get("HF_ENDPOINT", "")
if not _current_ep or "huggingface.co" in _current_ep:
    os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
os.environ["TRANSFORMERS_OFFLINE"] = "0"
os.environ["HF_HUB_OFFLINE"] = "0"

# Ensure the project root is in the Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Configure logging to stderr
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    stream=sys.stderr,
)
logger = logging.getLogger("wandering-rag")

# Suppress verbose HTTP request logging from libraries
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("huggingface_hub").setLevel(logging.WARNING)
logging.getLogger("sentence_transformers").setLevel(logging.WARNING)

# Parse CLI arguments
_parser = argparse.ArgumentParser(
    description="RAG Knowledge Base REST API Server",
    formatter_class=argparse.RawDescriptionHelpFormatter,
    epilog="""
Examples:
  python server.py                              # default: 127.0.0.1:8000
  python server.py --host 0.0.0.0 --port 8000  # listen on all interfaces
""",
)
_parser.add_argument(
    "--host",
    default=os.getenv("RAG_HOST", "127.0.0.1"),
    help="Host to bind (default: 127.0.0.1, env: RAG_HOST)",
)
_parser.add_argument(
    "--port",
    type=int,
    default=int(os.getenv("RAG_PORT", "8000")),
    help="Port to bind (default: 8000, env: RAG_PORT)",
)
_args = _parser.parse_args()


def create_app():
    """Create the Starlette ASGI app with REST API routes."""
    from starlette.applications import Starlette

    from api.app import create_api_routes, get_cors_middleware, init_db_and_engine

    # Initialize SQLite database and async processing engine
    data_dir = os.getenv("RAG_DATA_DIR", "./data/")
    init_db_and_engine(data_dir)

    routes = list(create_api_routes())
    middleware = [get_cors_middleware()]

    return Starlette(routes=routes, middleware=middleware)


def main():
    """Entry point for the REST API server."""
    import uvicorn

    app = create_app()
    logger.info(f"Starting RAG REST API: http://{_args.host}:{_args.port}/api/")
    # Use the default asyncio event loop (NOT uvloop). Under uvicorn's uvloop,
    # the blocking C extensions executed inside asyncio.to_thread — the zvec
    # vector store (create_and_open) and sentence-transformers model loading —
    # deadlock in the worker thread, hanging document ingestion at CHUNKING.
    # The standard asyncio loop does not exhibit this deadlock.
    uvicorn.run(app, host=_args.host, port=_args.port, loop="asyncio")


if __name__ == "__main__":
    main()
