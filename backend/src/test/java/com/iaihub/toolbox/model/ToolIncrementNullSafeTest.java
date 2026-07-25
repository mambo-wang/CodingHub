package com.iaihub.toolbox.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolIncrementNullSafeTest {

    /** 模拟从数据库加载、计数字段为 NULL 的实体（Hibernate 直接赋值绕过 @Builder.Default 的 0 默认值） */
    private Tool nullCountTool() {
        Tool tool = new Tool();
        // 不设置任何 count 字段，保持 null，复现下载时 NPE 的真实场景
        return tool;
    }

    @Test
    void incrementDownloadCount_shouldNotThrowOnNull() {
        Tool tool = nullCountTool();
        tool.incrementDownloadCount();
        assertEquals(1, tool.getDownloadCount());
        assertNotNull(tool.getScore());
    }

    @Test
    void incrementViewCount_shouldNotThrowOnNull() {
        Tool tool = nullCountTool();
        tool.incrementViewCount();
        assertEquals(1, tool.getViewCount());
    }

    @Test
    void incrementLikeCount_shouldNotThrowOnNull() {
        Tool tool = nullCountTool();
        tool.incrementLikeCount();
        assertEquals(1, tool.getLikeCount());
    }

    @Test
    void incrementCommentCount_shouldNotThrowOnNull() {
        Tool tool = nullCountTool();
        tool.incrementCommentCount();
        assertEquals(1, tool.getCommentCount());
    }

    @Test
    void incrementFavoriteCount_shouldNotThrowOnNull() {
        Tool tool = nullCountTool();
        tool.incrementFavoriteCount();
        assertEquals(1, tool.getFavoriteCount());
    }

    @Test
    void decrementShouldStayAtZeroWhenNull() {
        Tool tool = nullCountTool();
        tool.decrementLikeCount();
        tool.decrementFavoriteCount();
        assertEquals(0, tool.getLikeCount());
        assertEquals(0, tool.getFavoriteCount());
    }

    @Test
    void scoreIsComputedFromZeroWhenCountsNull() {
        Tool tool = nullCountTool();
        tool.incrementViewCount();
        assertEquals(BigDecimal.valueOf(1), tool.getScore());
    }
}
