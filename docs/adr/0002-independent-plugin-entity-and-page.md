# 插件使用独立实体与独立前端页面，不复用 Tool

插件（含 `plugin.json` 强清单 schema + 组件目录结构）与工具广场的 Tool（自由内容 + 附件）是不同领域概念，故建独立 `Plugin` 实体、独立前端 `/plugin-market` 页面，而非给 Tool 加 type 字段混用。复用基础设施：Category 分类树、统一互动、权限、上传组件。

同时清理历史包袱：删除工具广场中"API→插件"的展示替换分类（Category ID=3），其下既有工具迁移到"其他"分类；前端"插件"logo 映射一并移除。

插件的点赞/评论/收藏**复用统一互动模块**（`TargetType` 新增 `PLUGIN` 值 + 三个 Service 补 switch 分支 + Plugin 实体带 likeCount/commentCount/viewCount/score 字段），不新建互动表或接口——遵守"评论收藏点赞必须复用统一实现"的项目纪律。接入即白拿点赞通知（`resolveTargetOwnerId` 打通后插件作者收到被赞通知）。插件接评分联动（对齐工具广场）并支持匿名点赞（IP hash）。
