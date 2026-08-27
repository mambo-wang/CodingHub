### 2026-08-26 21:53

诊断并修复「工具市场更新时间显示为当前时间」bug：根因是 Tool 实体的 @PreUpdate 在任意 save() 时刷新 updatedAt，配合读改写式计数更新（getToolById 每次浏览即 incrementViewCount+save）导致更新时间被刷成浏览时刻。

### 2026-08-26 21:53

已将 Tool 的计数更新改为 repository 层原子 SQL（@Modifying，8 个方法，score 权重 view1/download2/like3/favorite4/comment5），并扩展到 ForumPost、Video、Plugin 三实体（score 权重 view1/like3/comment5，无 favoriteCount），同步修改 ToolService/ForumPostService/VideoService/PluginService 与 UnifiedLikeService/UnifiedCommentService 调用点。

### 2026-08-26 21:53

顺带修正 GitHttpConfig 预先存在的编译错误（FileResolver 泛型误用 org.eclipse.jgit.transport.HttpServletRequest，改为 jakarta.servlet.http.HttpServletRequest），compileJava BUILD SUCCESSFUL。

### 2026-08-26 21:53

遗留：需重启后端生效；修复前四类实体的历史 updatedAt 已被浏览/点赞/评论污染，用户已表示可生成回填 SQL（重置为 createdAt 或最后编辑时间），尚未执行，待确认是否需要回填。
