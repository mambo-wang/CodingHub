-- =====================================================
-- Fix: Unescape HTML entities in tool content field
-- XssSanitizer was incorrectly applied to Markdown content,
-- causing double-escaping in code blocks (&lt; instead of <).
-- Frontend markdown-it (html: false) already handles XSS prevention.
-- =====================================================

-- Unescape HTML entities in tool.content
-- Order: &amp; MUST be replaced LAST to avoid double-unescaping
UPDATE tool
SET content = REPLACE(content, '&lt;', '<'),
    content = REPLACE(content, '&gt;', '>'),
    content = REPLACE(content, '&quot;', '"'),
    content = REPLACE(content, '&#39;', ''''),
    content = REPLACE(content, '&amp;', '&')
WHERE content IS NOT NULL
  AND (content LIKE '%&lt;%'
       OR content LIKE '%&gt;%'
       OR content LIKE '%&quot;%'
       OR content LIKE '%&#39;%'
       OR content LIKE '%&amp;%');
