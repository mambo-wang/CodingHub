"""Document profiler for automatic chunking strategy selection.

Single-pass scan of document features to determine the optimal
chunking strategy without requiring LLM or external services.
"""

import re
from dataclasses import dataclass


@dataclass
class DocumentProfile:
    """Features extracted from a single pass over the document."""
    heading_count: int = 0       # number of Markdown headings (# to ######)
    heading_density: float = 0.0  # headings / total_lines
    code_ratio: float = 0.0      # fraction of chars inside fenced code blocks
    has_tables: bool = False      # whether document contains | tables
    total_chars: int = 0         # total character count
    total_lines: int = 0         # total line count


def profile_document(text: str) -> DocumentProfile:
    """Analyze document features in a single pass.

    Extracts heading count, code ratio, table presence, and basic
    statistics used by select_strategy() to choose the optimal
    chunking mode.

    Args:
        text: The full document text.

    Returns:
        DocumentProfile with extracted features.
    """
    if not text:
        return DocumentProfile()

    lines = text.split("\n")
    total_lines = len(lines)
    total_chars = len(text)

    heading_count = 0
    has_tables = False
    in_code_fence = False
    code_chars = 0

    for line in lines:
        stripped = line.strip()

        # Track fenced code blocks
        if re.match(r'^(`{3,}|~{3,})', stripped):
            in_code_fence = not in_code_fence
            code_chars += len(line) + 1
            continue

        if in_code_fence:
            code_chars += len(line) + 1
            continue

        # Count Markdown headings
        if re.match(r'^#{1,6}\s+', stripped):
            heading_count += 1

        # Detect table rows
        if stripped.startswith("|") and stripped.endswith("|"):
            has_tables = True

    heading_density = heading_count / max(total_lines, 1)
    code_ratio = code_chars / max(total_chars, 1)

    return DocumentProfile(
        heading_count=heading_count,
        heading_density=heading_density,
        code_ratio=code_ratio,
        has_tables=has_tables,
        total_chars=total_chars,
        total_lines=total_lines,
    )


def select_strategy(profile: DocumentProfile) -> str:
    """Select the optimal chunking strategy based on document profile.

    Decision logic (simplified from WeKnora's 15+ indicators):
    - heading_count >= 3 and density > 0.005 → structural
    - code_ratio > 0.5 → structural (protect code blocks)
    - total_chars < 200 → recursive (too short for structure)
    - default → structural (best for MD-heavy knowledge bases)

    Args:
        profile: DocumentProfile from profile_document().

    Returns:
        Strategy name: "structural" or "recursive".
    """
    if profile.total_chars < 200:
        return "recursive"

    if profile.heading_count >= 3 and profile.heading_density > 0.005:
        return "structural"

    if profile.code_ratio > 0.5:
        return "structural"

    # Default: structural is best for most MD/code documents
    return "structural"
