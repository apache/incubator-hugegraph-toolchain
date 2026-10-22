# 最终验证记录

本地集成源码：`1ea4a9bb7e150b3169a713473b0ccf81eac85bdb`，随后文档提交不改变运行代码。Java17；Node24.22.0 / pnpm12.14.0。Maven使用经过校验的候选SDK和独立缓存，未绕过外部输入门禁。前端生产构建包含i18n、TypeScript及Vite，最终文案修复后再次成功。

## 后端、浏览器及脚本

- 查询/投影：55项专项全部通过、Checkstyle0；最终逐行mapper/局部scope小修单独20项通过（有重叠，不累加）。异常/历史19项全部通过。
- 元数据服务/Controller相关测试通过；TopBar/API/查询上下文29项通过，含迟到Vermeer能力和过期响应。最终集成的GraphAnalysis上下文3项另行通过。
- Server Struct完整136项测试、格式及模块编译通过；压缩输出长度和SHA相同。
- 原匿名Chrome用例及独立PD认证管理员/普通成员Chrome实际登录、查询、空间/账号可见性、越权和注销通过，最终页面异常0。角色边界见[认证说明](comparison/final-followup/auth-verification.md)。
- 巨键SDK错误体HTTP400保持不变，最终消息2048字符、响应2121字节；原始属性SHA保持。局部归约/前序显式limit/全局count最后5项[真实API断言](comparison/final-local-reducer-smoke.json)通过。
- [脚本负向测试](scripts/test_evidence_scripts.py)6项通过（0.041秒）：拒连/短响应真实502、headers后失败不重复写状态、126秒终态轮询、超时非0、边缺失/身份/端点/内容不符必须失败。真实专用1MiB边身份/端点/长度/SHA校验也通过。运行：`python3 -m unittest discover -s docs/performance/2026-10-22-hubble-hstore/scripts -p test_evidence_scripts.py -v`。

## 前端完整扫描及失败项复核

固定Node24.22下执行全量Jest（2workers）完成210组/2064项，耗时416.109秒：207组、2056项通过；3组、8项失败。这不是一次全绿运行。

- 4项文案失败来自本次新增提示中的句号，违反仓库既有简洁文案约束；仅修改中英文提示，不改测试白名单。标点约束及查询相关4组/170项随后全部通过（9.491秒），i18n和生产构建通过。
- 其余4项为未修改Graph编辑页面的5秒/10秒测试超时。固定Node24.22隔离2组结果25/26通过；仍超时的示例导入用例再单独执行以4.391秒通过，保留原5秒门限。所有原失败用例最终都在定向重跑中通过，但不将这些分次结果表述为单次全量全绿。
- 先前与认证夹具启动并行的一次runInBand全量在300秒处中止；inspector实际栈定位到未修改ImageView的jsdom/AntD样式计算，不能仅根据测试排序认定首个测试或查询保护发生死循环。ImageView及TaskEdit/Schema三组隔离48项通过；该观测不属于产品性能采样。

完整日志保留在本机`/workspace/hubble-perf/logs`，包括`optimized-frontend-full-two-workers.log`、`frontend-copy-punctuation-query-tests.log`、`frontend-isolated-graph-pinned-node-tests.log`、`frontend-graph-example-case-isolated.log`。没有为获取通过结果提高测试超时，也没有修改无关页面业务代码。

## GitHub CI

以各PR当前Checks为准，本地结果不代替远端CI。Server本轮Struct、PD、HStore、RocksDB、内存后端、cluster及license检查已通过；Store Arthas交互检查曾因版本输出与prompt同一行而被既有正则误判。该fixture不加载Struct/BytesBuffer，仅失败job重跑一次，其余成功job不重复。Toolchain的最新提交会重新触发CI；未将排队/进行中的检查标记为成功。
