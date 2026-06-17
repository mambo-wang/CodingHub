# User Nickname Feature Specification

> Synced from change: `user-nickname-feature` (2026-06-17)
> Schema: ui-design-to-browser-test

## Overview

为用户系统添加 nickname（昵称）字段，支持注册时设置昵称、登录返回昵称、前端各页面展示昵称。

## Capabilities

### 1. User Model - Nickname Field

#### Scenario 1: 用户注册时设置昵称

- GIVEN: 新用户注册信息，username="wangbao", nickname="王宝", password=***
- WHEN: 系统创建用户记录
- THEN: User 模型包含 nickname="王宝" 字段

#### Scenario 2: 用户未设置昵称时显示账号

- GIVEN: 数据库中存在老用户，只有 username="olduser"，nickname 为 NULL
- WHEN: 前端展示用户信息
- THEN: 显示 "olduser"（降级显示账号）

#### Scenario 3: 昵称唯一性约束

- GIVEN: 数据库中已存在 nickname="王宝" 的用户
- WHEN: 新用户注册时尝试使用 nickname="王宝"
- THEN: 系统返回 400 错误，提示"昵称已被使用"

#### Scenario 4: 昵称长度验证

- GIVEN: 新用户注册信息，username="newuser", nickname="A"（长度1）
- WHEN: 用户提交注册请求
- THEN: 系统返回 400 错误，提示"昵称长度需在2-10字符之间"

#### Scenario 5: 昵称格式验证

- GIVEN: 新用户注册信息，username="newuser", nickname="test<Script>"（包含特殊字符）
- WHEN: 用户提交注册请求
- THEN: 系统对 nickname 进行 XSS 过滤或返回 400 错误

### 2. Auth API - Nickname Registration

#### Scenario 1: 注册成功 - 包含昵称

- GIVEN: 新用户注册信息，username="wangbao", nickname="王宝", password=***
- WHEN: 用户提交 POST /api/auth/register 请求
- THEN: 系统返回 201 状态码，用户信息包含 username 和 nickname，accessToken 和 refreshToken

#### Scenario 2: 注册失败 - 昵称重复

- GIVEN: 数据库中已存在用户，username="user1", nickname="王宝"
- WHEN: 新用户提交注册请求，username="user2", nickname="王宝"
- THEN: 系统返回 400 状态码，错误信息"昵称已被使用"

#### Scenario 3: 注册失败 - 昵称长度不足

- GIVEN: 新用户注册信息，username="newuser", nickname="A", password=***
- WHEN: 用户提交注册请求
- THEN: 系统返回 400 状态码，错误信息"昵称长度需在2-10字符之间"

#### Scenario 4: 注册失败 - 用户名重复

- GIVEN: 数据库中已存在用户 username="existing"
- WHEN: 新用户提交注册请求，username="existing", nickname="新昵称"
- THEN: 系统返回 400 状态码，错误信息"用户名已被注册"

#### Scenario 5: 登录返回用户昵称

- GIVEN: 数据库中用户 username="wangbao", nickname="王宝"
- WHEN: 用户提交登录请求，username="wangbao", password=***
- THEN: 系统返回用户信息包含 nickname="王宝"

#### Scenario 6: 获取当前用户信息包含昵称

- GIVEN: 用户已登录，token 有效
- WHEN: 用户发送 GET /api/users/me 请求
- THEN: 系统返回用户信息包含 nickname 字段

### 3. Frontend - Nickname Display

#### Scenario 1: 注册页面显示昵称输入框

- GIVEN: 用户访问注册页面 /register
- WHEN: 页面加载
- THEN: 表单包含 username（账号）、nickname（昵称）、password（密码）三个输入框

#### Scenario 2: 右上角显示昵称

- GIVEN: 用户已登录，用户信息为 username="wangbao", nickname="王宝"
- WHEN: 页面右上角渲染用户信息
- THEN: 显示"王宝"（昵称），而非"wangbao"（账号）

#### Scenario 3: 右上角未设置昵称时显示账号

- GIVEN: 用户已登录，用户信息为 username="olduser", nickname=null
- WHEN: 页面右上角渲染用户信息
- THEN: 显示"olduser"（账号）

#### Scenario 4: 工具详情页作者信息展示

- GIVEN: 工具详情页，工具作者信息为 username="wangbao", nickname="王宝"
- WHEN: 页面渲染作者信息区域
- THEN: 显示"王宝(wangbao)"格式
- AND: Hover 时显示完整信息 tooltip

#### Scenario 5: 帖子详情页作者信息展示

- GIVEN: 帖子详情页，帖子作者信息为 username="wangbao", nickname="王宝"
- WHEN: 页面渲染作者信息区域
- THEN: 显示"王宝(wangbao)"格式
- AND: Hover 时显示完整信息 tooltip

#### Scenario 6: 帖子列表作者信息展示

- GIVEN: 帖子列表页，帖子作者信息为 username="wangbao", nickname="王宝"
- WHEN: 页面渲染帖子列表
- THEN: 每条帖子的作者显示"王宝(wangbao)"格式

#### Scenario 7: 工具列表作者信息展示

- GIVEN: 工具广场列表页，工具作者信息为 username="wangbao", nickname="王宝"
- WHEN: 页面渲染工具卡片
- THEN: 作者信息显示"王宝(wangbao)"格式
