# 全局 TODO

仅记录各 goal 完成后仍有价值、但不属于已完成 goal 的后续事项。

## Hubble 2.0

- 完成 Hubble UI/UX 重构：基于真实双模式与 Chrome 基线完成三方向选型，交付“理解图、准备数据、查询分析”三个核心旅程，并以能力映射、可访问性、性能和发布包回归收口。
- 现代化 React、Ant Design、X6/Graphin/G6、Dagre 与构建链；先固化核心页面、图交互、性能和浏览器回归基线，再分批升级并清理兼容层。
- HugeGraph Server 提供保留 Schema 的 data-only clear API 后，恢复 Hubble“仅清数据”能力及权限、默认图和失败合同测试。
- 重构 Gremlin Basic 认证，移除 HTTP Session 中的短期明文凭据，并验证 token 生命周期。
- 发布加固：建立逐 JAR license/native allowlist 与 NOTICE 审计，RC 使用正式发布密钥验证 `.sha512`/`.asc`。
