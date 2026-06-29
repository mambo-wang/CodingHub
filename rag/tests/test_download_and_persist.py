"""Tests for download_document and ingest_content file persistence.

Run: cd rag && python -m pytest tests/test_download_and_persist.py -v
"""

import os
import sys

import pytest

# Add project root to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core import service


@pytest.fixture
def temp_data_dir(tmp_path):
    """Override VectorStore data_dir to a temp directory for testing."""
    store = service.get_store()
    original_data_dir = store.data_dir
    store.data_dir = str(tmp_path)
    yield str(tmp_path)
    store.data_dir = original_data_dir


@pytest.fixture
def clean_collection(temp_data_dir):
    """Ensure a clean test collection."""
    store = service.get_store()
    try:
        store.delete_collection("test-dl")
    except Exception:
        pass
    yield "test-dl"
    try:
        store.delete_collection("test-dl")
    except Exception:
        pass


class TestDownloadDocument:

    def test_download_existing_file(self, clean_collection, temp_data_dir):
        collection = clean_collection
        uploads_dir = os.path.join(temp_data_dir, "_uploads", collection)
        os.makedirs(uploads_dir, exist_ok=True)
        test_file = os.path.join(uploads_dir, "test-doc.md")
        with open(test_file, "w", encoding="utf-8") as f:
            f.write("# Test content")
        result = service.download_document(test_file, collection=collection)
        assert result == os.path.realpath(test_file)

    def test_download_missing_filepath(self, clean_collection):
        with pytest.raises(ValueError, match="Missing filepath"):
            service.download_document("", collection=clean_collection)

    def test_download_path_traversal(self, clean_collection, temp_data_dir):
        collection = clean_collection
        malicious_path = os.path.join(temp_data_dir, "_uploads", collection, "..", "..", "etc", "passwd")
        with pytest.raises(ValueError, match="traversal"):
            service.download_document(malicious_path, collection=collection)

    def test_download_file_not_found(self, clean_collection, temp_data_dir):
        collection = clean_collection
        uploads_dir = os.path.join(temp_data_dir, "_uploads", collection)
        os.makedirs(uploads_dir, exist_ok=True)
        fake_path = os.path.join(uploads_dir, "nonexistent.md")
        with pytest.raises(FileNotFoundError, match="not found"):
            service.download_document(fake_path, collection=collection)


class TestIngestContentPersistence:

    def test_text_file_saved_to_uploads(self, clean_collection, temp_data_dir):
        collection = clean_collection
        content = "# Hello\n\nThis is a test document with enough content to create at least one chunk for testing purposes."
        filename = "persist-test.md"
        result = service.ingest_content(content, filename=filename, collection=collection)
        assert result["status"] == "ok"
        expected_path = os.path.join(temp_data_dir, "_uploads", collection, filename)
        assert os.path.isfile(expected_path)
        with open(expected_path, "r", encoding="utf-8") as f:
            assert f.read() == content

    def test_text_file_overwrite_on_reupload(self, clean_collection, temp_data_dir):
        collection = clean_collection
        service.ingest_content("Version 1", filename="overwrite.md", collection=collection)
        path = os.path.join(temp_data_dir, "_uploads", collection, "overwrite.md")
        with open(path, "r", encoding="utf-8") as f:
            assert f.read() == "Version 1"
        service.ingest_content("Version 2 content", filename="overwrite.md", collection=collection)
        with open(path, "r", encoding="utf-8") as f:
            assert f.read() == "Version 2 content"

    def test_download_after_ingest(self, clean_collection, temp_data_dir):
        collection = clean_collection
        content = "# Downloadable\n\nThis text file should be downloadable after ingest."
        filename = "downloadable.md"
        service.ingest_content(content, filename=filename, collection=collection)
        filepath = os.path.join(temp_data_dir, "_uploads", collection, filename)
        result = service.download_document(filepath, collection=collection)
        assert os.path.isfile(result)
        with open(result, "r", encoding="utf-8") as f:
            assert f.read() == content
