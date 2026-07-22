"""Text chunker with recursive character splitting."""

import os
import hashlib
from dataclasses import dataclass


@dataclass
class Chunk:
    """A single text chunk with metadata."""
    text: str
    source: str
    chunk_index: int
    doc_id: str  # sha256(source)[:16]
    context_header: str = ""  # heading breadcrumb (separated from content)


def compute_doc_id(filepath: str) -> str:
    """Compute a stable document ID from file path."""
    normalized = os.path.normpath(os.path.abspath(filepath))
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]


def embedding_content(chunk) -> str:
    """Build the embedding input for a chunk: context_header + content.

    When context_header is non-empty, returns "header\\n\\ncontent".
    Otherwise returns just the content text.
    """
    header = getattr(chunk, "context_header", "") or ""
    if header:
        return f"{header}\n\n{chunk.text}"
    return chunk.text


def chunk_with_validation(
    text: str,
    filepath: str = "",
    mode: str = "structural",
    chunk_size: int = 500,
    chunk_overlap: int = 50,
) -> list:
    """Chunk text with quality validation and automatic tier degradation.

    Executes the specified chunking mode, validates the result against 5
    quality rules, and falls back to recursive mode if validation fails
    (structural only). Semantic mode never degrades (user-explicit choice).

    Args:
        text: The full document text.
        filepath: Source file path.
        mode: Chunking mode — "structural", "recursive", or "semantic".
        chunk_size: Target max characters per chunk.
        chunk_overlap: Overlap between chunks.

    Returns:
        List of Chunk objects (validated or degraded).
    """
    import logging
    from core.validator import validate_chunks

    logger = logging.getLogger(__name__)
    total_chars = len(text)

    # Execute the requested chunking mode
    if mode == "semantic":
        chunks = semantic_chunk_text(text, filepath, chunk_size, chunk_overlap)
    elif mode == "recursive":
        chunks = chunk_text(text, filepath, chunk_size, chunk_overlap)
    else:  # structural (default)
        chunks = structural_chunk_text(text, filepath, chunk_size, chunk_overlap)

    # Validate
    result = validate_chunks(chunks, total_chars, chunk_size)
    if result.ok:
        return chunks

    # Semantic mode: log but do NOT degrade
    if mode == "semantic":
        logger.warning(
            "chunker: semantic validation failed (%s) but not degrading "
            "(user-explicit mode)", result.reason
        )
        return chunks

    # Structural mode: degrade to recursive
    if mode == "structural":
        logger.info(
            "chunker: tier structural rejected: %s; falling back to recursive",
            result.reason,
        )
        fallback_chunks = chunk_text(text, filepath, chunk_size, chunk_overlap)
        return fallback_chunks

    # Recursive mode: final fallback, return as-is
    logger.warning(
        "chunker: recursive validation failed (%s) but this is the final fallback",
        result.reason,
    )
    return chunks


def chunk_text(
    text: str,
    filepath: str = "",
    chunk_size: int = 500,
    chunk_overlap: int = 50,
) -> list[Chunk]:
    """Split text into overlapping chunks using recursive character splitting.

    Splits by paragraphs first, then sentences, then characters,
    trying to keep chunks under chunk_size while preserving semantic boundaries.
    Protected regions (images, links, code blocks, tables, LaTeX) are kept intact.
    """
    if not text.strip():
        return []

    doc_id = compute_doc_id(filepath)

    # Identify protected regions before splitting
    protected = protected_spans(text)

    segments = _recursive_split(text, chunk_size, chunk_overlap, protected)

    chunks = []
    for i, segment in enumerate(segments):
        segment = segment.strip()
        if segment:
            chunks.append(Chunk(
                text=segment,
                source=filepath,
                chunk_index=i,
                doc_id=doc_id,
            ))
    return chunks


def _recursive_split(
    text: str,
    chunk_size: int,
    chunk_overlap: int,
    protected: list[tuple[int, int]] | None = None,
) -> list[str]:
    """Recursively split text by paragraph, sentence, then character boundaries.

    When protected spans are provided, split points are adjusted to avoid
    breaking inside protected regions.
    """
    if len(text) <= chunk_size:
        return [text] if text.strip() else []

    # Try splitting by paragraphs first
    paragraphs = text.split("\n\n")
    if len(paragraphs) > 1:
        return _merge_segments(paragraphs, chunk_size, chunk_overlap, "\n\n", protected, text)

    # Try splitting by single newlines
    lines = text.split("\n")
    if len(lines) > 1:
        return _merge_segments(lines, chunk_size, chunk_overlap, "\n", protected, text)

    # Try splitting by sentences
    sentences = _split_sentences(text)
    if len(sentences) > 1:
        return _merge_segments(sentences, chunk_size, chunk_overlap, " ", protected, text)

    # Last resort: split by characters with overlap
    return _split_by_chars(text, chunk_size, chunk_overlap, protected)


def _merge_segments(
    segments: list[str],
    chunk_size: int,
    chunk_overlap: int,
    separator: str,
    protected: list[tuple[int, int]] | None = None,
    original_text: str = "",
) -> list[str]:
    """Merge small segments into chunks, splitting large ones recursively.

    When protected spans are provided, segments containing protected regions
    are kept whole even if they slightly exceed chunk_size.
    """
    result = []
    current_parts = []
    current_len = 0

    for seg in segments:
        seg = seg.strip()
        if not seg:
            continue

        seg_len = len(seg)

        # If a single segment exceeds chunk_size, recursively split it
        if seg_len > chunk_size:
            if current_parts:
                result.append(separator.join(current_parts))
                current_parts = []
                current_len = 0
            # Check if this segment contains a protected region
            if protected and original_text:
                seg_start = original_text.find(seg)
                if seg_start >= 0:
                    seg_end = seg_start + len(seg)
                    # Check if any protected span is within this segment
                    has_protected = any(
                        s >= seg_start and e <= seg_end
                        for s, e in protected
                    )
                    if has_protected:
                        # Use protection-aware splitting
                        sub_chunks = _split_with_protection(
                            seg, chunk_size, chunk_overlap, protected, seg_start
                        )
                        result.extend(sub_chunks)
                        continue
            sub_chunks = _recursive_split(seg, chunk_size, chunk_overlap)
            result.extend(sub_chunks)
            continue

        # Check if adding this segment would exceed chunk_size
        new_len = current_len + seg_len + (len(separator) if current_parts else 0)
        if new_len > chunk_size and current_parts:
            # Before splitting, check if the boundary would cut a protected region
            if protected and original_text:
                # Find position of current segment in original text
                seg_pos = original_text.find(seg)
                if seg_pos >= 0 and _is_in_protected(seg_pos, protected):
                    # This segment is inside a protected region — keep it in current chunk
                    current_parts.append(seg)
                    current_len = new_len
                    continue

            result.append(separator.join(current_parts))
            # Keep overlap: carry over tail of current chunk
            current_parts, current_len = _compute_overlap(
                current_parts, separator, chunk_overlap
            )
            current_parts.append(seg)
            current_len += seg_len + len(separator)
        else:
            current_parts.append(seg)
            current_len = new_len

    if current_parts:
        result.append(separator.join(current_parts))

    return result


def _compute_overlap(
    parts: list[str],
    separator: str,
    overlap_size: int,
) -> tuple[list[str], int]:
    """Compute overlap parts from the tail of the current chunk."""
    if overlap_size <= 0:
        return [], 0

    overlap_parts = []
    overlap_len = 0
    for part in reversed(parts):
        part_len = len(part) + (len(separator) if overlap_parts else 0)
        if overlap_len + part_len > overlap_size:
            break
        overlap_parts.insert(0, part)
        overlap_len += part_len

    return overlap_parts, overlap_len


def _split_sentences(text: str) -> list[str]:
    """Split text into sentences, handling Chinese and English punctuation."""
    import re
    # Split on sentence-ending punctuation followed by space or end
    sentences = re.split(r'(?<=[。！？.!?])\s*', text)
    return [s for s in sentences if s.strip()]


def _split_by_chars(
    text: str,
    chunk_size: int,
    chunk_overlap: int,
    protected: list[tuple[int, int]] | None = None,
) -> list[str]:
    """Hard split by character count with overlap, respecting protected regions."""
    if not protected:
        chunks = []
        start = 0
        while start < len(text):
            end = start + chunk_size
            chunks.append(text[start:end])
            start = end - chunk_overlap
        return chunks

    # Protection-aware character split
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        if end >= len(text):
            chunks.append(text[start:])
            break

        # Adjust end to avoid splitting inside a protected region
        safe_end = _find_safe_split_point(text, end, protected, search_back=True)
        if safe_end <= start:
            # Can't go back, try going forward
            safe_end = _find_safe_split_point(text, end, protected, search_back=False)
        if safe_end <= start:
            safe_end = end  # Fallback: hard cut

        chunks.append(text[start:safe_end])
        start = max(safe_end - chunk_overlap, start + 1)

    return chunks


def _split_with_protection(
    text: str,
    chunk_size: int,
    chunk_overlap: int,
    protected: list[tuple[int, int]],
    offset: int = 0,
) -> list[str]:
    """Split a large text segment while keeping protected regions intact.

    Protected regions are treated as atomic units. If a protected region
    exceeds _ABSOLUTE_MAX_SIZE, it is force-split at newlines/spaces.

    Args:
        text: The segment text to split.
        chunk_size: Target max chunk size.
        chunk_overlap: Overlap between chunks.
        protected: Protected spans relative to the original document.
        offset: Character offset of this segment within the original document.

    Returns:
        List of text chunks.
    """
    # Convert global protected spans to local (relative to this segment)
    local_spans: list[tuple[int, int]] = []
    seg_end = offset + len(text)
    for s, e in protected:
        if e <= offset or s >= seg_end:
            continue
        local_s = max(s - offset, 0)
        local_e = min(e - offset, len(text))
        if local_s < local_e:
            local_spans.append((local_s, local_e))

    if not local_spans:
        return _recursive_split(text, chunk_size, chunk_overlap)

    # Build units: interleave free text and protected regions
    units: list[tuple[str, bool]] = []  # (text, is_protected)
    pos = 0
    for s, e in local_spans:
        if s > pos:
            units.append((text[pos:s], False))
        protected_text = text[s:e]
        # Force-split oversized protected regions
        if len(protected_text) > _ABSOLUTE_MAX_SIZE:
            for part in _split_protected_region_forced(protected_text):
                units.append((part, True))
        else:
            units.append((protected_text, True))
        pos = e
    if pos < len(text):
        units.append((text[pos:], False))

    # Merge units into chunks respecting chunk_size
    chunks: list[str] = []
    current = ""
    for unit_text, is_prot in units:
        if not unit_text.strip() and not is_prot:
            current += unit_text
            continue

        candidate = current + unit_text
        if len(candidate) <= chunk_size or is_prot:
            # Protected units are always kept whole (allow overshoot)
            current = candidate
        else:
            # Flush current chunk
            if current.strip():
                chunks.append(current.strip())
            current = unit_text

    if current.strip():
        chunks.append(current.strip())

    # If any free-text chunk is still too large, recursively split it
    final: list[str] = []
    for chunk in chunks:
        if len(chunk) > chunk_size * 2:
            # Likely a large free-text region, split recursively
            sub = _recursive_split(chunk, chunk_size, chunk_overlap)
            final.extend(sub)
        else:
            final.append(chunk)

    return final


# ── Semantic Chunking ────────────────────────────────────────


def semantic_chunk_text(
    text: str,
    filepath: str = "",
    chunk_size: int = 500,
    chunk_overlap: int = 50,
) -> list[Chunk]:
    """Split text into chunks based on semantic similarity between sentences.

    Uses the embedding model to encode sentences, then detects topic boundaries
    where adjacent sentence similarity drops below a dynamic threshold
    (mean - 1 standard deviation). Oversized chunks fall back to recursive
    character splitting.

    Args:
        text: The full document text.
        filepath: Source file path (used for doc_id).
        chunk_size: Max characters per chunk (safety limit).
        chunk_overlap: Overlap size (used only for fallback recursive split).

    Returns:
        List of Chunk objects.
    """
    import logging
    logger = logging.getLogger(__name__)

    if not text.strip():
        return []

    doc_id = compute_doc_id(filepath)

    # Step 1: split into sentences (preserving paragraph structure)
    sentences = _extract_sentences(text)
    if not sentences:
        return []

    # Short-circuit: too few sentences or fits in one chunk
    total_len = sum(len(s) for s in sentences)
    if len(sentences) <= 2 or total_len <= chunk_size:
        combined = " ".join(sentences).strip()
        if combined:
            return [Chunk(text=combined, source=filepath,
                          chunk_index=0, doc_id=doc_id)]
        return []

    # Step 2: encode all sentences
    try:
        from core.embeddings import EmbeddingService
        embedder = EmbeddingService()
        embeddings = embedder.encode(sentences)
    except Exception as e:
        logger.warning(f"Semantic chunking failed to encode sentences: {e}. "
                       f"Falling back to recursive split.")
        segments = _recursive_split(text, chunk_size, chunk_overlap)
        return [
            Chunk(text=seg.strip(), source=filepath,
                  chunk_index=i, doc_id=doc_id)
            for i, seg in enumerate(segments) if seg.strip()
        ]

    if len(embeddings) < 2:
        combined = " ".join(sentences).strip()
        return [Chunk(text=combined, source=filepath,
                      chunk_index=0, doc_id=doc_id)] if combined else []

    # Step 3: compute adjacent cosine similarities
    similarities = []
    for i in range(len(embeddings) - 1):
        sim = _dot_product(embeddings[i], embeddings[i + 1])
        similarities.append(sim)

    # Step 4: dynamic threshold = mean - 1*std
    threshold = _dynamic_threshold(similarities)

    # Step 5: find breakpoints and group sentences
    groups = _group_by_breakpoints(sentences, similarities, threshold, chunk_size)

    # Step 6: build chunks, falling back to recursive split for oversized groups
    chunks = []
    chunk_idx = 0
    for group in groups:
        combined = " ".join(group).strip()
        if not combined:
            continue
        if len(combined) <= chunk_size:
            chunks.append(Chunk(text=combined, source=filepath,
                                chunk_index=chunk_idx, doc_id=doc_id))
            chunk_idx += 1
        else:
            # Oversized group: fall back to recursive character split
            sub_segments = _recursive_split(combined, chunk_size, chunk_overlap)
            for seg in sub_segments:
                seg = seg.strip()
                if seg:
                    chunks.append(Chunk(text=seg, source=filepath,
                                        chunk_index=chunk_idx, doc_id=doc_id))
                    chunk_idx += 1

    return chunks


def _extract_sentences(text: str) -> list[str]:
    """Split text into sentences, treating paragraph breaks as sentence boundaries."""
    import re
    # First split by paragraphs
    paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
    sentences = []
    for para in paragraphs:
        # Within each paragraph, split by sentence-ending punctuation
        parts = re.split(r'(?<=[。！？.!?])\s*', para)
        for part in parts:
            part = part.strip()
            if part:
                sentences.append(part)
    return sentences


def _dot_product(a: list[float], b: list[float]) -> float:
    """Compute dot product of two vectors (cosine similarity for normalized vectors)."""
    return sum(x * y for x, y in zip(a, b))


def _dynamic_threshold(similarities: list[float], n_sigma: float = 1.0) -> float:
    """Compute dynamic breakpoint threshold as mean - n_sigma * std."""
    if not similarities:
        return 0.5
    n = len(similarities)
    mean = sum(similarities) / n
    if n < 2:
        return mean
    variance = sum((s - mean) ** 2 for s in similarities) / n
    std = variance ** 0.5
    return mean - n_sigma * std


def _group_by_breakpoints(
    sentences: list[str],
    similarities: list[float],
    threshold: float,
    chunk_size: int,
) -> list[list[str]]:
    """Group sentences into chunks based on similarity breakpoints.

    A new chunk starts wherever adjacent similarity drops below threshold,
    or when the current group would exceed chunk_size.
    """
    groups = []
    current_group = [sentences[0]]
    current_len = len(sentences[0])

    for i in range(len(similarities)):
        next_sentence = sentences[i + 1]
        next_len = len(next_sentence) + 1  # +1 for space join

        is_break = similarities[i] < threshold
        would_exceed = current_len + next_len > chunk_size

        if is_break or (would_exceed and current_group):
            groups.append(current_group)
            current_group = [next_sentence]
            current_len = len(next_sentence)
        else:
            current_group.append(next_sentence)
            current_len += next_len

    if current_group:
        groups.append(current_group)

    return groups


# ── Structural Chunking ──────────────────────────────────────


def structural_chunk_text(
    text: str,
    filepath: str = "",
    chunk_size: int = 500,
    chunk_overlap: int = 50,
) -> list[Chunk]:
    """Split text into chunks that respect document structure.

    Recognises Markdown headings, fenced code blocks, and tables as
    structural boundaries.  Each resulting chunk is prefixed with its
    enclosing heading (if any) so the LLM retains section context.

    Within a single structural section that exceeds *chunk_size*, the
    content is further split by the existing recursive character splitter.
    Protected regions (images, links, code blocks, tables, LaTeX) are kept intact.

    Args:
        text: The full document text.
        filepath: Source file path (used for doc_id).
        chunk_size: Max characters per chunk.
        chunk_overlap: Overlap size (passed to recursive split for
            oversized sections).

    Returns:
        List of Chunk objects.
    """
    if not text.strip():
        return []

    doc_id = compute_doc_id(filepath)

    # Identify protected regions before splitting
    protected = protected_spans(text)

    blocks = _parse_structural_blocks(text)

    if not blocks:
        return []

    # Merge small blocks and split large ones (protection-aware)
    # Returns list of (heading, body) tuples
    raw_chunks = _merge_and_split_blocks(blocks, chunk_size, chunk_overlap, protected, text)

    chunks = []
    for i, (heading, body) in enumerate(raw_chunks):
        body = body.strip()
        if body:
            chunks.append(Chunk(
                text=body,
                source=filepath,
                chunk_index=i,
                doc_id=doc_id,
                context_header=heading,
            ))
    return chunks


def _parse_structural_blocks(text: str) -> list[dict]:
    """Parse text into structural blocks based on document structure.

    Returns a list of dicts with keys:
        heading: str — the most recent Markdown heading (may be empty)
        body: str — the text content of this block
        kind: str — "heading", "code", "table", or "text"
    """
    import re

    lines = text.split("\n")
    blocks: list[dict] = []
    current_heading = ""
    current_lines: list[str] = []
    in_code_fence = False
    in_table = False

    def _flush():
        """Flush accumulated lines as a text block."""
        nonlocal current_lines
        body = "\n".join(current_lines).strip()
        if body:
            blocks.append({"heading": current_heading, "body": body, "kind": "text"})
        current_lines = []

    for line in lines:
        stripped = line.strip()

        # ── Fenced code block (``` or ~~~) ──
        if re.match(r'^(`{3,}|~{3,})', stripped):
            if in_code_fence:
                # Closing fence — flush the code block
                current_lines.append(line)
                _flush()
                in_code_fence = False
                continue
            else:
                # Opening fence — flush any preceding text first
                _flush()
                in_code_fence = True
                current_lines = [line]
                continue

        if in_code_fence:
            current_lines.append(line)
            continue

        # ── Markdown heading ──
        heading_match = re.match(r'^(#{1,6})\s+(.+)', stripped)
        if heading_match:
            _flush()
            current_heading = stripped
            # Store the heading line itself as a tiny block so it's not lost
            blocks.append({"heading": current_heading, "body": current_heading, "kind": "heading"})
            continue

        # ── Table row (lines starting with |) ──
        is_table_row = stripped.startswith("|") and stripped.endswith("|")
        if is_table_row:
            if not in_table:
                # Entering a table — flush preceding text
                _flush()
                in_table = True
            current_lines.append(line)
            continue
        elif in_table:
            # Leaving the table
            _flush()
            in_table = False

        # ── Plain text ──
        current_lines.append(line)

    # Flush remaining content
    if in_code_fence or in_table:
        _flush()
    else:
        _flush()

    return blocks


def _merge_and_split_blocks(
    blocks: list[dict],
    chunk_size: int,
    chunk_overlap: int,
    protected: list[tuple[int, int]] | None = None,
    original_text: str = "",
) -> list[tuple[str, str]]:
    """Merge small structural blocks and split oversized ones.

    Returns list of (heading, body) tuples where heading is the context
    breadcrumb (kept separate from body for context_header storage).
    When protected spans are provided, oversized blocks are split
    with protection awareness.
    """
    result: list[tuple[str, str]] = []
    buf_parts: list[str] = []
    buf_heading = ""
    buf_len = 0

    for block in blocks:
        heading = block["heading"]
        body = block["body"]
        kind = block.get("kind", "text")

        # Heading blocks only update context, don't contribute body text
        if kind == "heading":
            # Flush current buffer if heading changes
            if buf_parts and heading != buf_heading:
                result.append((buf_heading, "\n\n".join(buf_parts)))
                buf_parts = []
                buf_len = 0
            buf_heading = heading
            continue

        # For length calculation, include heading prefix if it differs from body
        if heading and not body.startswith(heading):
            block_len = len(heading) + 2 + len(body)
        else:
            block_len = len(body)

        # Oversized block: flush buffer first, then split
        if block_len > chunk_size:
            if buf_parts:
                result.append((buf_heading, "\n\n".join(buf_parts)))
                buf_parts = []
                buf_len = 0

            # Split the body (without prefix) then pair with heading
            if protected and original_text:
                body_start = original_text.find(body)
                if body_start >= 0:
                    sub_segments = _split_with_protection(
                        body, chunk_size, chunk_overlap, protected, body_start
                    )
                else:
                    sub_segments = _recursive_split(body, chunk_size, chunk_overlap)
            else:
                sub_segments = _recursive_split(body, chunk_size, chunk_overlap)
            for seg in sub_segments:
                seg = seg.strip()
                if seg:
                    result.append((heading, seg))
            continue

        # Check if merging would exceed chunk_size
        new_len = buf_len + block_len + (2 if buf_parts else 0)  # \n\n join
        heading_changed = buf_parts and heading != buf_heading

        if heading_changed or (new_len > chunk_size and buf_parts):
            result.append((buf_heading, "\n\n".join(buf_parts)))
            buf_parts = []
            buf_len = 0

        # Add body (without heading prefix — heading stored separately)
        buf_parts.append(body)
        buf_len += block_len + (2 if buf_len > 0 else 0)
        buf_heading = heading

    if buf_parts:
        result.append((buf_heading, "\n\n".join(buf_parts)))

    return result


# ── Protected Patterns ───────────────────────────────────────

import re as _re

# 6 protected region patterns (order matters for priority in overlap resolution)
_PROTECTED_PATTERNS: list[tuple[str, _re.Pattern]] = [
    # 1. LaTeX block formula: $$...$$  (multiline)
    ("latex_block", _re.compile(r"\$\$[\s\S]+?\$\$")),
    # 2. Markdown image: ![alt](url)
    ("md_image", _re.compile(r"!\[[^\]]*\]\([^)]+\)")),
    # 3. Markdown link: [text](url)
    ("md_link", _re.compile(r"\[[^\]]*\]\([^)]+\)")),
    # 4. Table header + separator row: | col | col |\n| --- | --- |
    ("table_header_sep", _re.compile(
        r"^\|.+\|[ \t]*\n\|[\s:]*-+[\s:|-]*\|[ \t]*$", _re.MULTILINE
    )),
    # 5. Table data row: | ... |
    ("table_row", _re.compile(r"^\|.+\|[ \t]*$", _re.MULTILINE)),
    # 6. Fenced code block: ```lang ... ``` or ~~~lang ... ~~~
    ("fenced_code", _re.compile(r"^(?:`{3,}|~{3,})[^\n]*\n[\s\S]*?^(?:`{3,}|~{3,})[ \t]*$", _re.MULTILINE)),
]

# Absolute maximum size for a single protected region before forced split
_ABSOLUTE_MAX_SIZE = 7500


def protected_spans(text: str) -> list[tuple[int, int]]:
    """Identify protected regions in text that must not be split.

    Scans for 6 types of atomic content (LaTeX blocks, MD images, MD links,
    table header+separator, table data rows, fenced code blocks) and returns
    a sorted list of non-overlapping (start, end) character offset spans.
    Overlapping regions are resolved by keeping the longest match.

    Args:
        text: The full document text.

    Returns:
        Sorted list of (start, end) tuples representing protected byte ranges.
    """
    if not text:
        return []

    # Collect all matches from all patterns
    raw_spans: list[tuple[int, int]] = []
    for _name, pattern in _PROTECTED_PATTERNS:
        for m in pattern.finditer(text):
            raw_spans.append((m.start(), m.end()))

    if not raw_spans:
        return []

    # Sort by start position, then by length descending (longer first)
    raw_spans.sort(key=lambda s: (s[0], -(s[1] - s[0])))

    # Merge overlapping spans: keep the longest when overlapping
    merged: list[tuple[int, int]] = []
    for start, end in raw_spans:
        if not merged:
            merged.append((start, end))
            continue
        prev_start, prev_end = merged[-1]
        if start <= prev_end:
            # Overlapping or adjacent — extend to the longer end
            merged[-1] = (prev_start, max(prev_end, end))
        else:
            merged.append((start, end))

    return merged


def _is_in_protected(pos: int, spans: list[tuple[int, int]]) -> bool:
    """Check if a character position falls inside any protected span."""
    for start, end in spans:
        if start <= pos < end:
            return True
        if start > pos:
            break
    return False


def _find_safe_split_point(
    text: str,
    target: int,
    spans: list[tuple[int, int]],
    search_back: bool = True,
) -> int:
    """Find the nearest split point at or before target that is outside protected spans.

    Args:
        text: The full text being split.
        target: The desired split position.
        spans: Protected spans (sorted).
        search_back: If True, search backwards from target; else forwards.

    Returns:
        A safe split position (character offset).
    """
    if not spans:
        return target

    if not _is_in_protected(target, spans):
        return target

    if search_back:
        # Search backwards for the start of the protected region
        for start, end in spans:
            if start <= target < end:
                return start
        return target
    else:
        # Search forwards for the end of the protected region
        for start, end in spans:
            if start <= target < end:
                return end
        return target


def _split_protected_region_forced(
    text: str,
    max_size: int = _ABSOLUTE_MAX_SIZE,
) -> list[str]:
    """Force-split an oversized protected region at newlines or spaces.

    Used when a single protected region exceeds _ABSOLUTE_MAX_SIZE.
    """
    if len(text) <= max_size:
        return [text]

    parts: list[str] = []
    start = 0
    while start < len(text):
        end = start + max_size
        if end >= len(text):
            parts.append(text[start:])
            break

        # Try to find a newline to split on (search backwards from end)
        split_at = text.rfind("\n", start, end)
        if split_at <= start:
            # No newline found, try space
            split_at = text.rfind(" ", start, end)
        if split_at <= start:
            # No good split point, hard cut
            split_at = end

        parts.append(text[start:split_at])
        start = split_at
        # Skip the delimiter character
        if start < len(text) and text[start] in ("\n", " "):
            start += 1

    return parts
