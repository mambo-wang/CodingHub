package com.iaihub.toolbox.model.forum;

/**
 * 帖子正文的存储格式。
 *
 * <p>这是「存储格式」而非「来源格式」：HTML 帖子的原文就是 HTML，可无损再编辑。
 *
 * @see <a href="../../../../../../../../CONTEXT.md">CONTEXT.md - 正文格式</a>
 */
public enum ContentFormat {
    MARKDOWN,
    HTML
}
