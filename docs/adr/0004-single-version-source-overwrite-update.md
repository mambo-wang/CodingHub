# 插件版本采用单一版本源（plugin.json）与覆盖式更新

CodeBuddy 缓存键按 `plugin.json` 的 version > 市场条目 version > Git SHA 取优先级，`/plugin update` 依赖版本变化触发更新。若 CodingHub 与 plugin.json 各存一份 version 必然漂移，导致缓存键错乱。决定：单一版本源 = zip 内 `plugin.json.version`，上传时解析入库、marketplace.json 生成时引用；version 与库中当前版本相同则拒绝覆盖上传。不做多版本管理/回滚（后置）。
