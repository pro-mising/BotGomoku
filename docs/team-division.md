# BotGomoku 六人分工文档

## 1. 项目简介

BotGomoku 是一个五子棋在线对战与 Bot 测试平台。项目从原有 Bot 对战项目改造而来，保留了匹配、Bot 代码编译运行、微服务调用等特点，并扩展了五子棋对战、Bot 测试、对局列表、社区、排行榜和个人中心等功能。

本项目按照实际功能模块进行六人分工，其中对战模块工作量最大，因此拆分给两名成员共同完成；Bot 测试模块由本人负责；对局列表、社区、排行榜和个人中心分别由其他成员负责。

## 2. 分工总览

| 成员 | 负责模块 | 主要职责 |
| --- | --- | --- |
| 成员一 | 对战模块：前端棋盘与交互 | 负责五子棋棋盘 UI、落子交互、回合提示、胜负弹窗 |
| 成员二 | 对战模块：后端规则与匹配通信 | 负责五子棋规则、胜负判定、WebSocket 同步、匹配成功进入对局 |
| 成员三 | Bot 测试模块 | 负责 Bot 测评、系统内置 Bot、评分指标、赛后分析 |
| 成员四 | 对局列表模块 | 负责对局列表、对局搜索、收藏、复盘和对局分析展示 |
| 成员五 | 社区模块 | 负责帖子、评论、点赞、社区搜索和在线状态展示 |
| 成员六 | 排行榜与个人中心模块 | 负责多维排行榜、活跃榜、个人中心和用户数据统计 |

## 3. 成员一：对战模块 - 前端棋盘与交互

### 负责范围

成员一主要负责玩家在对战页面中直接看到和操作的内容，包括棋盘、棋子、用户卡片和对战结果展示。

### 完成内容

- 将原项目页面改造为五子棋对战页面。
- 设计并实现 15 × 15 五子棋棋盘。
- 棋盘初始状态为空，不再显示原项目的障碍物。
- 支持鼠标点击棋盘进行落子。
- 区分黑子和白子显示效果。
- 显示最后一步落子位置。
- 显示当前回合提示。
- 优化棋盘坐标和边界显示。
- 优化左右两侧玩家信息卡片。
- 优化胜负结果弹窗。
- 适配不同屏幕下的对战页面布局。

### 主要代码位置

- `web/src/components/PlayGround.vue`
- `web/src/components/GameMap.vue`
- `web/src/assets/scripts/GameMap.js`
- `web/src/components/PlayerCard.vue`
- `web/src/components/ResultBoard.vue`
- `web/src/views/pk/PkIndexView.vue`

### 使用技术栈

- Vue3：实现对战页面、棋盘组件、玩家卡片和结果弹窗。
- Vuex：维护当前对局状态、当前回合、棋盘数据和胜负结果。
- JavaScript Canvas：绘制五子棋棋盘、棋子和最后一步提示。
- Bootstrap：用于部分按钮、布局和基础样式。
- WebSocket：前端接收后端推送的开始对局、落子和对局结果事件。

## 4. 成员二：对战模块 - 后端规则与匹配通信

### 负责范围

成员二主要负责对战模块的后端逻辑，包括玩家匹配、WebSocket 通信、五子棋规则判断和对局保存。

### 完成内容

- 维护玩家匹配池。
- 支持开始匹配和取消匹配。
- 匹配成功后通知双方同时进入对战。
- 修复匹配成功后只有一方进入对战的问题。
- WebSocket 断开时自动移除匹配池中的用户。
- 后端创建对局前检查双方是否在线。
- 实现黑子先手、双方轮流落子。
- 实现横向、纵向、斜向五子连线胜利判断。
- 判断非法落子和超时情况。
- 对局结束后保存对局记录。
- 将落子和结果通过 WebSocket 同步给双方。

### 主要代码位置

- `backendcloud/matchingsystem/src/main/java/com/kob/matchingsystem/service/impl/utils/MatchingPool.java`
- `backendcloud/backend/src/main/java/com/kob/backend/consumer/WebSocketServer.java`
- `backendcloud/backend/src/main/java/com/kob/backend/consumer/utils/Game.java`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/pk`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/pk`

### 使用技术栈

- Spring Boot：实现后端对战服务和匹配服务。
- Spring MVC：提供开始对局、接收 Bot 落子等接口。
- WebSocket：实现前后端实时通信，向双方同步对局状态。
- Spring Cloud OpenFeign：匹配服务调用后端服务创建对局。
- Nacos：用于服务注册与发现。
- ReentrantLock：保证匹配池和对局落子状态的线程安全。
- MyBatis Plus：保存对局记录和读取用户信息。
- MySQL：存储用户、对局记录等数据。

## 5. 成员三：Bot 测试模块

### 负责范围

成员三负责 Bot 测试模块，也就是用户编写 Bot 后，平台如何帮助用户测试 Bot 的强度，并给出分析结果。

### 完成内容

- 新增“Bot测试”页面。
- 支持用户选择自己编写的 Bot 进行测评。
- 支持快速测评和标准测评。
- 设计多个系统内置 Bot 作为测试对手。
- 引入 Alpha-Beta 等更强的内置 Bot 策略。
- 让用户 Bot 与不同难度的系统 Bot 对战。
- 记录每一局测试过程，支持查看回放。
- 设计综合评分公式：
  - 胜率 40%
  - 防守能力 20%
  - 进攻能力 20%
  - 稳定性 10%
  - 效率 10%
- 保存 Bot 测评报告。
- 接入 DeepSeek 赛后分析。
- 赛后分析展示为结构化模块：
  - 测评发现
  - 主要弱点
  - 改进建议
  - 优化后的 Bot 代码
- 优化 DeepSeek 返回代码展示效果。
- 使用异步方式处理赛后分析，避免页面长时间等待。

### 主要代码位置

- `web/src/views/bot/BotEvaluationView.vue`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/bot/BotEvaluationController.java`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/bot/evaluation`
- `backendcloud/backend/src/main/java/com/kob/backend/bot/builtin`
- `backendcloud/backend/src/main/java/com/kob/backend/pojo/BotEvaluationReport.java`
- `backendcloud/bot_evaluation_report.sql`

### 使用技术栈

- Vue3：实现 Bot 测试页面、测评报告展示、回放入口和分析结果展示。
- Spring Boot：实现 Bot 测评业务逻辑。
- Spring MVC：提供 Bot 测试、获取报告、DeepSeek 分析等接口。
- MyBatis Plus：保存和查询 Bot 测评报告。
- MySQL：存储 Bot 测评报告和赛后分析结果。
- RabbitMQ：异步处理 DeepSeek 赛后分析任务。
- Redis：缓存用户最近一次 Bot 测评报告 ID。
- DeepSeek API：生成赛后分析和优化后的 Bot 代码。
- Java 动态编译：编译用户提交的 Bot 代码。
- Alpha-Beta 算法：用于较高强度的系统内置 Bot。

## 6. 成员四：对局列表模块

### 负责范围

成员四负责对局列表模块，将原本简单的对局列表升级为对局中心，方便用户查看、搜索和复盘历史对局。

### 完成内容

- 优化对局列表页面 UI。
- 展示对局双方、胜负结果、对局时间。
- 支持进入对局复盘页面。
- 支持对局搜索。
- 支持按对手、结果、热度等条件筛选。
- 支持对局收藏和取消收藏。
- 展示对局收藏数量。
- 对局结束后生成对局分析。
- 展示对局关键时刻、胜利方向、总步数和精彩度。
- 优化对局分析展示文案，避免信息杂乱。
- 支持热门对局展示。

### 主要代码位置

- `web/src/views/record/RecordIndexView.vue`
- `web/src/views/record/RecordContentView.vue`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/record`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/record`
- `backendcloud/backend/src/main/java/com/kob/backend/pojo/RecordAnalysis.java`
- `backendcloud/backend/src/main/java/com/kob/backend/pojo/RecordFavorite.java`
- `backendcloud/record_center.sql`

### 使用技术栈

- Vue3：实现对局中心、筛选条件、收藏按钮和复盘入口。
- Spring Boot：实现对局列表和对局分析业务。
- Spring MVC：提供对局列表、搜索、收藏等接口。
- MyBatis Plus：查询对局记录、收藏记录和分析结果。
- MySQL：保存原始对局、收藏记录和对局分析结果。
- RabbitMQ：异步生成对局分析，避免列表页面阻塞。
- Redis：缓存热门对局。
- Elasticsearch：支持对局复盘搜索。

## 7. 成员五：社区模块

### 负责范围

成员五负责社区交流模块，让用户可以围绕 Bot、五子棋策略和对局复盘进行交流。

### 完成内容

- 新增“社区”页面。
- 展示帖子列表。
- 支持发表帖子。
- 支持删除自己发布的帖子。
- 支持帖子点赞和取消点赞。
- 支持查看帖子详情。
- 支持发表评论。
- 支持删除自己的评论。
- 展示帖子作者和评论作者的在线状态。
- 支持按标题、正文、作者搜索帖子。
- 支持关键词高亮。
- 支持按最新发布排序。
- 支持按点赞数排序。
- 准备社区演示数据。
- 优化社区页面 UI，使帖子浏览和评论交互更加清晰。

### 主要代码位置

- `web/src/views/community/CommunityIndexView.vue`
- `web/src/views/community/CommunityPostView.vue`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/community/CommunityController.java`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/community`
- `backendcloud/backend/src/main/java/com/kob/backend/pojo/CommunityPost.java`
- `backendcloud/backend/src/main/java/com/kob/backend/pojo/CommunityComment.java`
- `backendcloud/community.sql`
- `backendcloud/community_demo_seed.sql`

### 使用技术栈

- Vue3：实现社区列表、帖子详情、发布弹窗、评论区和点赞交互。
- Spring Boot：实现社区业务逻辑。
- Spring MVC：提供帖子、评论、点赞、搜索等接口。
- MyBatis Plus：操作帖子、评论和点赞数据。
- MySQL：存储社区帖子、评论和点赞记录。
- Elasticsearch：支持按标题、正文、作者搜索帖子。
- Redis：展示作者和评论用户的在线状态。
- HTML 高亮渲染：展示搜索关键词高亮结果。

## 8. 成员六：排行榜与个人中心模块

### 负责范围

成员六负责排行榜和个人中心模块，主要用于展示用户、Bot、社区和活跃度相关的数据统计。

### 完成内容

#### 排行榜

- 优化排行榜页面 UI。
- 新增多维榜单：
  - 天梯榜
  - Bot 强度榜
  - 社区贡献榜
  - 活跃榜
- 天梯榜展示玩家分数、对局数和胜率。
- Bot 强度榜展示 Bot 测评综合评分。
- 社区贡献榜展示发帖、获赞和评论贡献。
- 活跃榜展示近 7 天对局、发帖、评论和点赞活跃度。
- 支持榜单分页。
- 展示榜单更新时间。
- 支持榜单异步刷新和定时刷新。

#### 个人中心

- 新增个人中心入口，放在右上角用户名下拉菜单。
- 展示头像、用户名、用户 ID、天梯分和在线状态。
- 展示对战数据：
  - 总对局
  - 胜场
  - 败场
  - 平局
  - 胜率
- 展示最近对局入口。
- 展示 Bot 数据：
  - Bot 数量
  - 最近测评 Bot
  - 最高综合评分
- 展示社区数据：
  - 发帖数
  - 获赞数
  - 评论数
  - 社区贡献分
- 优化个人中心布局，避免页面出现空白区域。

### 主要代码位置

- `web/src/views/ranklist/RanklistIndexView.vue`
- `web/src/views/user/profile/UserProfileView.vue`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/ranklist`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/ranklist`
- `backendcloud/backend/src/main/java/com/kob/backend/controller/user/account`
- `backendcloud/backend/src/main/java/com/kob/backend/service/impl/user/account`

### 使用技术栈

- Vue3：实现排行榜页面和个人中心页面。
- Vue Router：提供排行榜和个人中心路由。
- Vuex：读取当前登录用户信息和 token。
- Spring Boot：实现排行榜和个人中心聚合接口。
- Spring MVC：提供榜单查询和个人中心概览接口。
- MyBatis Plus：统计用户、对局、Bot 测评和社区数据。
- MySQL：作为排行榜和个人中心统计的数据来源。
- Redis：缓存榜单数据、保存在线状态。
- RabbitMQ：异步刷新排行榜。
- Spring Scheduling：定时刷新排行榜，作为兜底机制。

## 9. 协作关系

| 模块 | 依赖或对接模块 | 协作说明 |
| --- | --- | --- |
| 对战前端 | 对战后端 | 前端发送落子，后端判断是否合法并同步结果 |
| 对战后端 | 对局列表 | 对局结束后保存记录，供对局列表展示 |
| Bot 测试 | Bot 运行逻辑 | Bot 测试需要执行用户 Bot 和系统 Bot |
| Bot 测试 | 排行榜 | Bot 测评报告用于 Bot 强度榜 |
| 社区 | 排行榜 | 社区数据用于社区贡献榜和活跃榜 |
| 对局列表 | 个人中心 | 最近对局和对战统计展示到个人中心 |
| 社区 | 个人中心 | 发帖、获赞、评论数据展示到个人中心 |

## 10. 答辩分工说明

答辩时可以这样介绍：

> 我们按照功能模块进行了分工。由于对战模块是项目最核心、工作量最大的部分，所以拆成两个人分别负责前端棋盘交互和后端规则通信。我负责 Bot 测试模块，主要完成系统内置 Bot、测评指标和赛后分析。其他成员分别负责对局列表、社区、排行榜和个人中心。这样每个人都有独立模块，同时模块之间又能通过对局数据、Bot 测评数据和社区数据形成联动。

这种分工的优点：

- 对战模块拆成前后端两部分，职责更清晰。
- Bot 测试模块独立，方便重点展示项目亮点。
- 对局列表、社区、排行榜、个人中心各自独立，避免成员之间功能重复。
- 每个模块都有明确页面和后端接口，便于答辩时说明个人贡献。
