## 压缩扩容微基准复现

以下命令依赖本机保留的 JDK 17、Server 依赖目录，以及优化前后的 Struct 编译产物。Java 源码来自报告的 `scripts/CompressionGrowthBenchmark.java`。输出写入新的目录，不覆盖报告中的原始记录；本说明没有重新执行基准。

```bash
mkdir -p /workspace/hubble-perf/compression-reproduction/classes

/workspace/.onboarding/tools/jdk17/bin/javac \
  -cp '/workspace/hubble-perf/runtime/server/lib/*' \
  -d /workspace/hubble-perf/compression-reproduction/classes \
  /workspace/hugegraph-toolchain/docs/performance/2026-10-22-hubble-hstore/scripts/CompressionGrowthBenchmark.java

/workspace/.onboarding/tools/jdk17/bin/java \
  -Xms256m -Xmx512m -XX:ActiveProcessorCount=4 \
  -cp '/workspace/.onboarding/sdk-source-verified/hugegraph-struct/target/classes:/workspace/hubble-perf/compression-reproduction/classes:/workspace/hubble-perf/runtime/server/lib/*' \
  CompressionGrowthBenchmark before 8 \
  > /workspace/hubble-perf/compression-reproduction/before.jsonl

/workspace/.onboarding/tools/jdk17/bin/java \
  -Xms256m -Xmx512m -XX:ActiveProcessorCount=4 \
  -cp '/workspace/worktrees/hugegraph-buffer-growth/hugegraph-struct/target/classes:/workspace/hubble-perf/compression-reproduction/classes:/workspace/hubble-perf/runtime/server/lib/*' \
  CompressionGrowthBenchmark after 8 \
  > /workspace/hubble-perf/compression-reproduction/after.jsonl
```

Classpath 的第一个目录分别固定原始 `86c05ef9c5d417bd37eab21d4ecd5af82bab17ef` 与补丁版本的 Struct 类；它们优先于最后的运行时依赖 JAR。因此，即使运行时 Struct JAR 已替换，`before` 仍读取保留的原始编译类。复现前应确认两个目录的版本；仅修改 `before` / `after` 参数标签不会切换实现。

两个独立 JVM 均使用 `Random(7)` 生成相同的 8 MiB 输入，LZ4 块大小为4096字节，初始缓冲区为输入长度除以1.5并取整。每个 JVM 先预热1次（`iteration=-1`），随后采样3次（`iteration=0..2`）；汇总中位数时排除预热。计时覆盖压缩与缓冲区写入，解压回读及 SHA256 核对在计时之外；每轮计时前调用 `System.gc()`。输出还记录扩容次数、累计复制字节和压缩结果摘要。应核对两版输出长度和摘要相同，再比较耗时及复制量。

这些绝对路径是本地实验环境的依赖，迁移环境时需要替换为相同版本的 JDK、源码编译产物及 Server 依赖目录。测试时应暂停其他构建及负载；微基准结果不等同于完整查询或异步任务的吞吐指标。
