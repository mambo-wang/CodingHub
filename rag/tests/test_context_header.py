"""Tests for ContextHeader separation in chunker.py."""

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from core.chunker import (
    Chunk,
    structural_chunk_text,
    chunk_text,
    embedding_content,
)


class TestContextHeaderGeneration:
    """Test that structural mode generates correct context_header breadcrumbs."""

    def test_structural_generates_context_header(self):
        """Structural mode stores heading in context_header, not in text."""
        text = (
            "# Introduction\n\n"
            "This is the intro paragraph.\n\n"
            "# Methods\n\n"
            "We used several methods."
        )
        chunks = structural_chunk_text(text, "test.md", chunk_size=500)
        # At least one chunk should have a non-empty context_header
        headers = [c.context_header for c in chunks if c.context_header]
        assert len(headers) > 0, "No context_header generated"
        # The heading should NOT be prefixed into the text body
        for c in chunks:
            if c.context_header and c.context_header.startswith("#"):
                # text should not start with the heading (it's separated)
                assert not c.text.startswith(c.context_header), (
                    f"Heading '{c.context_header}' was prefixed into text: '{c.text[:50]}'"
                )

    def test_context_header_contains_heading(self):
        """context_header contains the section heading."""
        text = "# Setup Guide\n\nFollow these steps to set up the project."
        chunks = structural_chunk_text(text, "test.md", chunk_size=500)
        # Find chunk with the setup guide content
        setup_chunks = [c for c in chunks if "Follow these steps" in c.text]
        assert len(setup_chunks) > 0
        assert "Setup Guide" in setup_chunks[0].context_header

    def test_recursive_mode_empty_context_header(self):
        """Recursive mode produces empty context_header (no heading tracking)."""
        text = "Just plain text without any markdown headings at all."
        chunks = chunk_text(text, "test.md", chunk_size=500)
        for c in chunks:
            assert c.context_header == ""


class TestEmbeddingContent:
    """Test the embedding_content() helper function."""

    def test_with_header(self):
        """When context_header is non-empty, returns header + \\n\\n + content."""
        chunk = Chunk(
            text="Some content here.",
            source="test.md",
            chunk_index=0,
            doc_id="abc",
            context_header="# Section Title",
        )
        result = embedding_content(chunk)
        assert result == "# Section Title\n\nSome content here."

    def test_without_header(self):
        """When context_header is empty, returns just the content."""
        chunk = Chunk(
            text="Plain content.",
            source="test.md",
            chunk_index=0,
            doc_id="abc",
            context_header="",
        )
        result = embedding_content(chunk)
        assert result == "Plain content."

    def test_none_header_treated_as_empty(self):
        """None context_header is treated as empty string."""
        chunk = Chunk(
            text="Content.",
            source="test.md",
            chunk_index=0,
            doc_id="abc",
            context_header=None,
        )
        result = embedding_content(chunk)
        assert result == "Content."

    def test_default_chunk_has_empty_header(self):
        """Chunk created without context_header defaults to empty string."""
        chunk = Chunk(text="Hello", source="x.md", chunk_index=0, doc_id="y")
        assert chunk.context_header == ""
        assert embedding_content(chunk) == "Hello"
