"""Embedding model wrapper using sentence-transformers with lazy loading."""

import logging
import os
import threading
from typing import Optional

logger = logging.getLogger(__name__)

# Default model — Qwen/Qwen3-Embedding-0.6B is a Chinese+English bilingual
# embedding model (1024-dim, 32K context) and is what the whole project targets
# (deploy scripts download it, encode_query uses the "query: " instruction prefix).
# all-MiniLM-L6-v2 was English-only and gave poor Chinese recall, so it is no
# longer the default. For a lighter CPU-only model set RAG_EMBEDDING_MODEL, e.g.
# "BAAI/bge-small-zh-v1.5". The embedding dimension is detected at load time.
DEFAULT_MODEL = os.getenv("RAG_EMBEDDING_MODEL", "Qwen/Qwen3-Embedding-0.6B")


class EmbeddingService:
    """Singleton embedding service with lazy model loading.

    The model is downloaded from HuggingFace on first use and cached
    locally at ~/.cache/huggingface/. Subsequent calls use the cache
    and work fully offline.
    """

    _instance: Optional["EmbeddingService"] = None
    _model = None
    _model_name: str = ""
    _dimension: int = 0
    _load_lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def _ensure_loaded(self):
        """Load the model on first call (lazy initialization). Thread-safe."""
        if self._model is not None:
            return

        with self._load_lock:
            # Double-check after acquiring lock
            if self._model is not None:
                return

            model_name = DEFAULT_MODEL
            logger.info(f"Loading embedding model: {model_name}")

            # Configure the HuggingFace endpoint BEFORE importing sentence-transformers.
            # huggingface_hub caches HF_ENDPOINT as a module-level constant at import
            # time, so the env var must be set first; otherwise requests hit
            # huggingface.co directly and time out on restricted networks. We also
            # override an explicitly-set official endpoint, since it is unreachable
            # from this network.
            current_ep = os.environ.get("HF_ENDPOINT", "")
            if not current_ep or "huggingface.co" in current_ep:
                os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
            # Allow online model download — override any inherited offline flag so
            # the model can actually be fetched on first run.
            os.environ["TRANSFORMERS_OFFLINE"] = "0"
            os.environ["HF_HUB_OFFLINE"] = "0"

            try:
                from sentence_transformers import SentenceTransformer
            except ImportError:
                raise RuntimeError(
                    "sentence-transformers is not installed. "
                    "Run: pip install sentence-transformers"
                )

            # Refresh huggingface_hub's cached endpoint constant. It was read at
            # import time (above) and won't pick up the env change otherwise.
            try:
                import huggingface_hub.constants as _hf_constants
                _hf_constants.HF_ENDPOINT = os.environ.get(
                    "HF_ENDPOINT", "https://huggingface.co"
                )
            except Exception:
                pass

            logger.info(f"HuggingFace endpoint: {os.environ.get('HF_ENDPOINT')}")

            self._model = SentenceTransformer(model_name)

            # Detect actual dimension by encoding a test sentence
            test_emb = self._model.encode(["test"], normalize_embeddings=True)
            self._dimension = test_emb.shape[1]
            self._model_name = model_name

            logger.info(
                f"Model loaded: {self._model_name}, dimension: {self._dimension}"
            )

    @property
    def dimension(self) -> int:
        self._ensure_loaded()
        return self._dimension

    @property
    def model_name(self) -> str:
        self._ensure_loaded()
        return self._model_name

    def encode(self, texts: list[str], batch_size: int | None = None) -> list[list[float]]:
        """Encode a list of texts into normalized embedding vectors.

        Args:
            texts: List of text strings to encode.
            batch_size: Batch size for encoding. None = use RAG_EMBEDDING_BATCH_SIZE
                env var (default 32). Larger values improve GPU utilization
                but increase memory usage.

        Returns:
            List of embedding vectors, each as a list of floats.
        """
        self._ensure_loaded()
        if batch_size is None:
            batch_size = int(os.getenv("RAG_EMBEDDING_BATCH_SIZE", "32"))
        embeddings = self._model.encode(
            texts,
            normalize_embeddings=True,
            show_progress_bar=False,
            batch_size=batch_size,
        )
        # Convert numpy arrays to plain Python lists
        return embeddings.tolist()

    def encode_query(self, query: str) -> list[float]:
        """Encode a single query string into a normalized embedding vector.

        Prepends "query: " prefix as recommended by Qwen3-Embedding for
        asymmetric retrieval (short query vs. long document). The model's
        training uses task-specific instructions to improve relevance.
        """
        self._ensure_loaded()
        embedding = self._model.encode(
            [f"query: {query}"],
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        return embedding[0].tolist()
