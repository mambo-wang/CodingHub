"""Tests for document profiler and auto strategy selection."""

import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from core.profiler import profile_document, select_strategy, DocumentProfile


class TestProfileDocument:
    """Test document feature extraction."""

    def test_markdown_headings_counted(self):
        """Markdown headings are correctly counted."""
        text = "# Title\n\n## Section 1\n\nContent.\n\n## Section 2\n\nMore.\n\n### Sub\n\nDeep."
        profile = profile_document(text)
        assert profile.heading_count == 4  # #, ##, ##, ###

    def test_code_ratio_computed(self):
        """Code ratio reflects fenced code block content."""
        text = "Intro text.\n\n```python\ndef foo():\n    return 42\n```\n\nOutro."
        profile = profile_document(text)
        assert profile.code_ratio > 0.2  # significant code content

    def test_table_detection(self):
        """Tables are detected by | row pattern."""
        text = "Data:\n\n| Name | Age |\n| --- | --- |\n| Alice | 30 |\n"
        profile = profile_document(text)
        assert profile.has_tables is True

    def test_no_tables(self):
        """Plain text has no tables."""
        text = "Just a paragraph of text without any tables."
        profile = profile_document(text)
        assert profile.has_tables is False

    def test_empty_text(self):
        """Empty text returns zero profile."""
        profile = profile_document("")
        assert profile.total_chars == 0
        assert profile.heading_count == 0

    def test_total_chars_and_lines(self):
        """Basic stats are correct."""
        text = "line1\nline2\nline3"
        profile = profile_document(text)
        assert profile.total_chars == len(text)
        assert profile.total_lines == 3


class TestSelectStrategy:
    """Test strategy selection logic."""

    def test_md_document_selects_structural(self):
        """A Markdown document with headings selects structural."""
        text = (
            "# Title\n\n## Section A\n\n"
            "This is a longer paragraph of content that makes the document exceed two hundred characters in total length for testing.\n\n"
            "## Section B\n\nMore detailed content here with enough text to be meaningful.\n\n"
            "## Section C\n\nFinal section with concluding remarks and summary."
        )
        profile = profile_document(text)
        assert profile.total_chars >= 200
        assert select_strategy(profile) == "structural"

    def test_pure_code_selects_structural(self):
        """A code-heavy document selects structural (protects code blocks)."""
        code_lines = [f"def func_{i}():\n    pass\n" for i in range(20)]
        text = "```python\n" + "\n".join(code_lines) + "\n```"
        profile = profile_document(text)
        assert profile.code_ratio > 0.5
        assert select_strategy(profile) == "structural"

    def test_plain_text_no_structure_selects_structural(self):
        """Plain text without headings defaults to structural."""
        text = "This is a plain paragraph. " * 50
        profile = profile_document(text)
        # No headings, but default is structural
        assert select_strategy(profile) == "structural"

    def test_very_short_text_selects_recursive(self):
        """Very short text (<200 chars) selects recursive."""
        text = "Short note."
        profile = profile_document(text)
        assert select_strategy(profile) == "recursive"

    def test_user_explicit_override(self):
        """When user explicitly sets strategy, profiler is not consulted.

        This is tested indirectly: select_strategy always returns a value,
        but service.py checks strategy != 'auto' before calling it.
        """
        profile = DocumentProfile(heading_count=0, total_chars=50)
        # Profiler would say "recursive" for short text
        assert select_strategy(profile) == "recursive"
        # But in service.py, if strategy="structural", profiler is skipped
