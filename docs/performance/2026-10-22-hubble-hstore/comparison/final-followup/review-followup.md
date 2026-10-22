## Review 后的最终功能补测

本阶段使用最终源码编译的Hubble及已修复压缩扩容的Server，只做功能断言，不覆盖早先性能样本。原始1000空间、两张12000点/12000边图和超长属性夹具继续保留。

- `g.V().fold().unfold().out()`默认返回300行并标明默认限制；显式`limit(12000)`交互预览只返回1000行并标明截断。终结读取不再被中间`fold()`误判为最终聚合。
- `g.V().fold().unfold().out().count()`与`g.V().count()`均仍返回12000，不被默认限制改写。
- 8 KiB属性图使用`g.V().limit(2).groupCount().by('payload')`，两条超长Map键对应entry省略，JSON为`[{}]`且`properties_truncated=true`；不生成可能碰撞的假键。
- 1 MiB字符串作为Map键仍被SDK的JSON字段名长度约束拒绝，HTTP400保持不变；此次未放宽解析限制。错误消息由1151156字符降为2048字符，HTTP响应由1151232字节降为2121字节（2 Ki字符约束适用于消息，不是整个响应体）。证据只保留状态、长度、短前缀及响应SHA256，没有归档完整巨大键。
- 100 KiB / 1 MiB原始点属性SHA256仍一致。完整最终结果见`review-followup-assertions.json`，补丁前摘要见`long-map-key-error-before-summary.json`。

全长Map键未成功查询属于明确能力边界；小属性与大属性性能表不混入这些单次功能断言。授权空间管理员、OBSERVER-only完整UI以及PD/direct运行时热切换未在此新增验证。
