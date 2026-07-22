"""Chunk quality validator with 5-rule verification and tier degradation."""

import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class ValidationResult:
    """Result of chunk validation."""
    ok: bool
    reason: str = ""


def validate_chunks(
    chunks: list,
    total_chars: int,
    chunk_size: int,
) -> ValidationResult:
    """Validate chunking output against 5 quality rules.

    Rules (from WeKnora validator):
    1. Non-empty: chunks count > 0
    2. Large doc not single chunk: if totalChars > 2*chunkSize, chunks > 1
    3. Fragmentation rate: excluding last chunk, tiny chunks (<50 chars) <= 25% AND count <= 2
    4. Oversized chunk: no chunk > 2*chunkSize
    5. Not all fragments: if totalChars > chunkSize, max chunk >= chunkSize/4

    Args:
        chunks: List of chunk objects (must have .text attribute) or strings.
        total_chars: Total character count of the original document.
        chunk_size: The target chunk size used for splitting.

    Returns:
        ValidationResult with ok=True if all rules pass, else ok=False with reason.
    """
    # Extract text lengths
    lengths = []
    for c in chunks:
        if hasattr(c, "text"):
            lengths.append(len(c.text))
        elif isinstance(c, str):
            lengths.append(len(c))
        else:
            lengths.append(0)

    num_chunks = len(lengths)

    # Rule 1: Non-empty
    if num_chunks == 0:
        return ValidationResult(ok=False, reason="empty output: no chunks produced")

    # Rule 2: Large document must not be a single chunk
    if total_chars > 2 * chunk_size and num_chunks == 1:
        return ValidationResult(
            ok=False,
            reason=f"large doc single chunk: {total_chars} chars in 1 chunk (threshold: {2 * chunk_size})",
        )

    # Rule 3: Fragmentation rate (exclude last chunk)
    if num_chunks > 1:
        check_lengths = lengths[:-1]  # exclude last chunk
        tiny_count = sum(1 for l in check_lengths if l < 50)
        frag_rate = tiny_count / len(check_lengths) if check_lengths else 0
        if frag_rate > 0.25 and tiny_count > 2:
            return ValidationResult(
                ok=False,
                reason=f"too many tiny chunks: {tiny_count}/{len(check_lengths)} ({frag_rate:.0%}) < 50 chars",
            )

    # Rule 4: Oversized chunk
    max_len = max(lengths)
    if max_len > 2 * chunk_size:
        return ValidationResult(
            ok=False,
            reason=f"oversized chunk: {max_len} chars > 2x chunk_size ({2 * chunk_size})",
        )

    # Rule 5: Not all fragments
    if total_chars > chunk_size and max_len < chunk_size / 4:
        return ValidationResult(
            ok=False,
            reason=f"all chunks too small: max {max_len} < chunk_size/4 ({chunk_size // 4})",
        )

    return ValidationResult(ok=True)
