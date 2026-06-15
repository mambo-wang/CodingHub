# Tool Modify and Delete

## Purpose

TBD - MCP Server 工具修改与文件删除能力规格

## Requirements

### Requirement: MCP 工具修改（h3_coding_hub_tool_modify）

系统 SHALL 通过 MCP 工具 `h3_coding_hub_tool_modify` 提供工具元数据的修改能力，调用方需传入 username/password 进行认证。

#### Scenario: 修改自己创建的工具的描述
- GIVEN: 用户已通过 `h3_coding_hub_tool_create` 创建了一个工具（toolId=42，version="1.0.0"）
- WHEN: 用户调用 `h3_coding_hub_tool_modify`，传入 toolId=42, content="新的描述", username和password正确，不传 version
- THEN: 工具描述更新为"新的描述"，版本号自动递增为 "1.0.1"，返回更新后的工具详情

#### Scenario: 修改工具时指定版本号
- GIVEN: 用户拥有工具 toolId=42（version="1.0.0"）
- WHEN: 用户调用 `h3_coding_hub_tool_modify`，传入 toolId=42, version="2.0.0"
- THEN: 工具版本号更新为 "2.0.0"，不自动递增

#### Scenario: 修改他人创建的工具
- GIVEN: 工具 toolId=99 由用户 B 创建
- WHEN: 用户 A 调用 `h3_coding_hub_tool_modify`，传入 toolId=99，使用自己的账号密码认证
- THEN: 返回错误信息，工具未修改

#### Scenario: 修改不存在的工具
- GIVEN: 数据库中不存在 toolId=9999
- WHEN: 用户调用 `h3_coding_hub_tool_modify`，传入 toolId=9999
- THEN: 返回错误信息"工具不存在"

#### Scenario: 版本号非标准格式（带后缀）
- GIVEN: 工具当前 version="1.0.0-beta"
- WHEN: 用户调用 `h3_coding_hub_tool_modify` 不传 version
- THEN: 递增时仅处理最后一段数字，后缀保留，结果为 "1.0.1-beta"

#### Scenario: 版本号最后一段不是数字
- GIVEN: 工具当前 version="1.0.alpha"
- WHEN: 用户调用 `h3_coding_hub_tool_modify` 不传 version
- THEN: 在末尾追加 ".1"，结果为 "1.0.alpha.1"

#### Scenario: 版本号为空或 null
- GIVEN: 工具当前 version 为空或 null
- WHEN: 用户调用 `h3_coding_hub_tool_modify` 不传 version
- THEN: 默认返回 "1.0.1"

#### Scenario: 修改时不传任何可选字段
- GIVEN: 用户拥有工具 toolId=42
- WHEN: 用户调用 `h3_coding_hub_tool_modify` 仅传 toolId、username、password
- THEN: 仅递增版本号，其他字段保持不变

#### Scenario: 修改时只传部分字段
- GIVEN: 用户拥有工具 toolId=42
- WHEN: 用户调用 `h3_coding_hub_tool_modify` 仅传 name 字段
- THEN: name 字段更新，其他字段保持不变

### Requirement: MCP 工具文件删除（h3_coding_hub_tool_file_delete）

系统 SHALL 通过 MCP 工具 `h3_coding_hub_tool_file_delete` 提供工具附件的删除能力，调用方需传入 username/password 进行认证。

#### Scenario: 删除自己工具下的文件
- GIVEN: 用户拥有工具 toolId=42，该工具下有文件 fileId=100
- WHEN: 用户调用 `h3_coding_hub_tool_file_delete`，传入 toolId=42, fileId=100，使用自己的账号密码认证
- THEN: 文件的物理文件和数据库记录均被删除，返回成功响应

#### Scenario: 删除他人工具下的文件
- GIVEN: 工具 toolId=99 由用户 B 创建，其下有文件 fileId=200
- WHEN: 用户 A 调用 `h3_coding_hub_tool_file_delete`，传入 toolId=99, fileId=200，使用自己的账号密码认证
- THEN: 返回错误信息"无权限删除此文件"，文件未被删除

#### Scenario: 删除不存在的文件
- GIVEN: 用户拥有工具 toolId=42，但该工具下不存在 fileId=9999
- WHEN: 用户调用 `h3_coding_hub_tool_file_delete`，传入 toolId=42, fileId=9999
- THEN: 返回错误信息"文件不存在"

### Requirement: MCP 工具认证

MCP 工具 SHALL 在每次调用时验证调用方传入的 username/password，拒绝未通过认证的请求。

#### Scenario: 传入错误的用户名或密码
- GIVEN: 工具 toolId=42 由某用户创建
- WHEN: 调用 `h3_coding_hub_tool_modify` 或 `h3_coding_hub_tool_file_delete` 时传入错误的 username/password
- THEN: 返回认证失败的错误信息，工具/文件未修改或未删除
