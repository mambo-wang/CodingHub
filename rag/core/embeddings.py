"""Embedding model wrapper using sentence-transformers with lazy loading."""

import logging
import os
import threading
from typing import Optional

logger = logging.getLogger(__name__)

# Default model — all-MiniLM-L6-v2 is fast on CPU (22M params vs Qwen3's 600M).
# Set RAG_EMBEDDING_MODEL for higher-quality models when GPU is available.
DEFAULT_MODEL = os.getenv("RAG_EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
# Expected embedding dimension for the default model
DEFAULT_DIMENSION = 384


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

            try:
                from sentence_transformers import SentenceTransformer
            except ImportError:
                raise RuntimeError(
                    "sentence-transformers is not installed. "
                    "Run: pip install sentence-transformers"
                )

            # Set HuggingFace mirror for China users (if not already configured)
            os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
            os.environ.setdefault("TRANSFORMERS_OFFLINE", "0")
            os.environ.setdefault("HF_HUB_OFFLINE", "0")

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
