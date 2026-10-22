## 真实 PD 认证补充验证

在性能采样结束后，另建完全独立目录和端口的 PD、Store、Server、Hubble 认证夹具，复用本地源码构建的1.8.0 JAR和最终前端构建。数据、账号元数据、运行配置均与原实例分开；4个新Java进程堆上限合计1408 MiB，实测总RSS小于3 GiB。凭据和配置仅留私有运行目录，不进入证据或仓库。端口、版本来源、内存及原图完整性记录见`auth-provenance.json`。此阶段只验证功能，不计入前述性能对照。

使用真实 Chrome 表单登录、实际空间和图选项点击，结果如下：

| 检查 | 管理员 | 普通空间成员 |
| --- | --- | --- |
| 认证上下文 | PD / SUPERADMIN | PD / USER |
| 空间选项 | DEFAULT、auth_hidden、auth_visible | DEFAULT、auth_visible |
| 账号管理入口 | 可见，实际/account页可用 | 隐藏，/account重定向/navigation |
| 已授权图查询 | g.V()成功，画布2点 | g.V()成功，画布2点 |
| 直接访问未授权空间 | 成功 | Hubble404，不泄漏空间存在性；Server REST403 |
| 注销后原浏览器会话 | /auth/status返回401 | /auth/status返回401 |
| 页面异常 | 0 | 0 |

普通用户在`auth_visible`具有SPACE_MEMBER和OBSERVER权限，其他自建空间没有授权。初始OBSERVER-only用户可看到授权空间、REST读取点，但Server图列表返回403；最终交互验证使用普通空间成员，不能据此声称所有只读角色流程已覆盖。空间管理员身份未单独验证。

本版前端没有PD/直连或Server实例的热切换按钮。连接由后端启动配置`pd.enabled`与`server.direct_url`等决定；此处真实验证PD模式配置及现有空间/图切换，不虚构连接模式切换操作。原匿名实例已有独立Chrome证据，此次新增真实认证环境的管理员和普通成员证据。

搭建时修正了独立Server的`gremlinserver.url`，使REST代理指向新Gremlin端口38182，与YAML一致；初期错误绑定的只读请求未写原图。截图及最终JSON均来自修正后。任务结果16 MiB容量未调整，原两张图点/边数量仍各12000，原实例未停机、未改认证或配置。

`browser-auth.json`为最终Chrome结果，3张截图分别展示管理员账号页、普通成员空间选项和普通成员查询。私有运行配置、密码、cookie/token及失败阶段完整日志均未归档。

验证后已停止新增4个Java进程和前端，8个新服务端口均关闭；夹具数据和私有配置保留以便复现。原PD、Store、Server、Hubble探活均200。

复现时保留的私有配置位于`/workspace/hubble-perf/auth-fixture/{pd,store,server,hubble}/conf`；使用`/workspace/hubble-perf/start_auth_component.py`依次启动`pd`、`store`、`server`、`hubble`，每个依赖探活成功后再启动下一个。前端在现有hubble-fe目录加载生产build，设置`HUBBLE_API_TARGET=http://127.0.0.1:28088`后执行`pnpm exec vite preview --host 127.0.0.1 --port 23000 --strictPort`。真实Chrome脚本为私有运行目录内`browser_auth.py`，从本地凭据文件读取临时测试账号，未将口令硬编码进脚本。保留数据无需重跑setup/seed；这些路径依赖本机源码、依赖及运行目录，不构成通用安装脚本。
