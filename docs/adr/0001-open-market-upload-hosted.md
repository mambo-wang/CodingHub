# 插件市场采用开放市场 + 上传托管形态

用户要求插件市场像现有工具广场一样开放：任何登录用户可自行创建并发布插件。交付形态定为上传托管——用户上传插件源码 zip，CodingHub 校验后托管文件，`marketplace.json` 中插件 `source` 指向 zip 的 HTTP URL。备选的"用户填 GitHub 仓库"被否决：平台退化为薄目录、价值低，且依赖用户拥有 GitHub 仓库。

主要风险：CodeBuddy 对 HTTP source 下载的 zip 是否自动解压**官方文档未说明**，是形态成立与否的关键未知数。因此 spike（实测安装一个 CodingHub 托管的 zip 插件）必须排在开发第一项；验证失败则降级为 GitHub 仓库形态（`source` 用 `owner/repo`）。
