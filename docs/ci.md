# Continuous integration

CI results are advisory and human review remains required. `.asf.yaml` requests no required CI contexts. The entry job keeps the real `check-license-header` name and executes headers and RAT, so this PR satisfies the old live rule before Gitbox applies the new configuration; no synthetic status or extra migration runner is needed.

`Module validation` is the single PR/push entry. Its entry job classifies the cumulative diff, records immutable Server inputs, runs policy/runtime fixtures, and checks license headers and RAT. License or policy-test failure does not implicitly skip already selected suites: callers use the classifier's actual outcome and cancellation state. Failed fixture producers retain their own failed checks; their consumers require the real fixture result.

Pure prose documentation and nested READMEs outside runtime/fixture directories run only this entry. Valid packaged README text no longer selects image builds. Source, type definitions, runtime config, executable scripts and test fixture text remain code inputs. Automatic PR labeling is removed; maintainers apply labels manually.

| Event / input | Validation |
| --- | --- |
| Normal PR or release push | Affected modules and consumers against the locked current Server only |
| Master push with runtime inputs | One mainline smoke runner after the entry; no repeated module/platform matrices |
| Client change | Client, Loader, Tools, Spark Connector and Hubble |
| Loader change | Loader and Hubble; production inputs also select Spark Loader and Flink CDC |
| Legacy/protocol compatibility, shared SDK/bootstrap/build inputs | Both current and official Server 1.7 baselines |
| HDFS inputs, shared file/line reader/source contracts or shared build/bootstrap inputs | HDFS inside the selected Loader job |
| Spark Loader / Flink CDC inputs, Client/Loader production code or shared build/bootstrap inputs | Corresponding specialty |
| Image packaging inputs | One image job; relevant PRs and full runs include data checks |
| Authentication/permission/security, dependency or CodeQL configuration inputs | Configured CodeQL languages; ordinary application edits do not start scans |
| Monday 06:00 Asia/Singapore | Full retained validation through this same entry |
| Run workflow | `mainline` (default) or `full`; branch selection remains native |

Manual and weekly validation are independent of documentation filtering. The default manual mainline uses the current baseline; `full` includes legacy, HDFS, Spark/Flink, image data checks, launcher platforms, scanning and the floating upstream check. The legacy fixture is allocated only when legacy validation is selected. Current runtime fixtures are shared by Hubble and Go; other Java consumers keep their existing job-local same-source SDK preparation.

Loader production inputs also select the Spark Loader and Flink CDC consumers. Shared file/line
reader and source contracts select HDFS because its implementations consume those classes; other
Loader changes do not enable HDFS or the legacy baseline by default. Changes to the entry
`ci.yml` exercise all its callers, including images, scanning and smoke, on PRs. Master
pushes retain the single smoke runner policy.

Module, launcher, Spark Loader and CodeQL matrices run one entry at a time. Including reusable workflows, the maximum possible job concurrency is 16 when entry changes also exercise smoke; this limits simultaneous work, while scoping removes unnecessary allocations. There is no report-only runner or independent CodeQL planner. Native job checks provide actual results; the entry summary and `ci-plan` explain selection. GitHub native reruns remain available, and automatic retry workflows are removed.

The floating upstream compatibility workflow remains manually callable and runs in the weekly full entry. It checks the current Toolchain checkout against one resolved ASF master commit, separately from the locked default baseline. Master pushes with affected runtime inputs call its smoke mode against the locked current baseline. It compiles the current Java consumers, runs Client unit and real schema/graph read/write checks, Loader unit and local-file read/write checks, Hubble backend unit checks, Tools CLI checks, and Go version/schema requests on one runner. Its Server fixtures are stopped afterward. Pure documentation master pushes still run only the entry. PR/full compatibility suites keep their original scope; no prior-run result is reused.

Image selection is independent of the Client → Loader → Hubble module-test dependency closure:

| Image input change | Images built and started |
| --- | --- |
| Loader Dockerfile | Loader |
| Hubble Dockerfile | Hubble |
| Loader POM, assembly descriptor/static files or runtime license inputs | Loader and Hubble |
| Hubble POMs, assembly descriptor/static files, distribution checker, backend startup properties or frontend build configuration | Hubble |
| Root/Client/reactor POMs, `.mvn`, `.dockerignore`, shared release docs or unknown shared inputs | Loader and Hubble |
| Ordinary Java/frontend application source or tests | None; the module behavior tests above still run |

Dockerfile-only changes run the image check without building Server fixtures. Hubble's Dockerfile builds Client and Loader before Hubble, so Loader packaging changes also select Hubble. Plain README text does not select images; the existing nonempty Hubble README content contract still requires fresh validation if the file is missing, empty or has an invalid mode. Every selected module and image runs in the current workflow; successful results from another run are not reused.

Each image job builds the checkout's actual Dockerfile, records its image ID and starts a local run-scoped tag with pulling disabled. The container's image ID must match the build. Loader directly executes `./bin/hugegraph-loader.sh --help`, including its executable permission and shebang; its default idle container alone cannot pass. Hubble must remain running and return a valid `/about` JSON response with the application name and version. Its public root page must contain the React root and serve nonempty local JavaScript bundles and referenced stylesheets with the correct content types, rejecting the HTML fallback for missing assets. The full image job also verifies same-source SDK JAR provenance, real Loader data readback and Hubble H2 persistence after container recreation. It reuses the public launcher, actual image identity and frontend asset checks in that same job, with a single product-image build. Mainline manual and push image checks keep build/start/public-entrypoint smoke; relevant PRs and weekly/full runs include data behavior. They do not publish an image.

Selected Client, Loader, Tools, Spark and Hubble tests compile and run on Java 17, with HDFS enabled in the selected Loader job when affected. Java module tests compile against SDK 1.8.0; Client, Loader, Tools, Hubble and Spark Connector test the selected current/legacy baselines. Go Client runs the same selected Server matrix without installing the Java SDK; these runtime checks supplement affected-module selection without adding unrelated modules. Their results remain advisory.

The historical Server 1.7 fixture uses the official ASF 1.7.0 binary archive with a pinned official SHA-512 and runs on its own Java 11 JVM. Downloads fail without a source-build fallback. The locked current Server uses SDK 1.8.0, Java 17 and TinkerPop 3.8.1. CI locks the complete immutable source commit `68855199031d5801edb4fe41b2bacbacffe8fe68`, so later master updates do not change the build inputs. The SDK action, CI fixtures, image defaults and packaging verifier use the same locked source commit.

Hubble's loader smoke checks the imported labels through Hubble Gremlin queries as well as direct Server REST counts. When the selected Server reports verified Cypher support, it also checks a scalar query, vertex/edge JSON, table and graph conversion, and successful execution history. An explicitly unsupported Server records a Cypher skip; errors from an enabled Cypher route fail the smoke. The fixture retains the selected Server's native channelizer, script imports and serializers while keeping its own addresses and authentication settings.

The official release archive is downloaded once and cached; the current Server fixture shared by Hubble and Go is built once, with independent services per job. Reuse verifies source, commit, JDK, build inputs and archive checksum. Client, Loader, Tools and Spark candidate tests start the archive already produced by their SDK bootstrap. Same-source SDK generation remains job-local and retains complete source-install and artifact-hash evidence; release/staging checks use their own isolated Maven repository without a candidate manifest. The bootstrap flattens CI-friendly POM versions before installation, including the root parent, so dependency and license metadata readers consume concrete coordinates without changing the locked source POMs. The service JVM is scoped to startup; Toolchain compilation and tests remain on Java 17.

Each Java HTTPS service startup generates temporary paired key/trust stores using the service JDK's `keytool`, with a two-day certificate for `localhost` and `127.0.0.1`. Client and Loader tests consume `HUGEGRAPH_TEST_TRUST_STORE_FILE`, exported through `GITHUB_ENV`; they no longer download the expired public test certificate. For local testing, the starter prints the truststore path. Pass it with `-Dhugegraph.test.trust-store-file=/absolute/path/to/hugegraph.truststore` when running the HTTPS tests. The truststore must match the running service; production hostname verification remains enabled.

Callers without `GITHUB_ENV`, including the legacy Loader Travis job, can set `HUGEGRAPH_TEST_TRUST_STORE_PATH_FILE` to an explicit temporary file before invoking the installer. After both services become ready, the starter writes the generated truststore path there. The caller exports that value as `HUGEGRAPH_TEST_TRUST_STORE_FILE` before running Maven, then removes the transfer file. The stores remain in the isolated service directory.

The shared Java initialization action requires Maven 3.9 or newer and supplies explicit settings through `MAVEN_ARGS`, preserving existing arguments and user settings. Dependencies are resolved from Central before ASF Stage, with Stage snapshots disabled; build plugins use the standard public repositories. Maven cache keys include repository settings and SDK preparation inputs. Only third-party dependencies seed the isolated SDK repository; cached HugeGraph artifacts are discarded, and Hubble reuses the shared Maven cache without a second overlapping cache.

Loader validates unit, file, JDBC and Kafka profiles against the selected Server baselines. HDFS setup and its profile execute only when selected. The Ubuntu HDFS checks use the ASF Hadoop 3.3.6 image pinned by digest, with separate NameNode and DataNode containers on the host network. Tests run on Java 17 and use `localhost:8020`. Startup checks live DataNode registration and a real block write/read before testing; containers are removed afterward. Image pulling and readiness are bounded. Cold runners still need to pull the image.

The immutable Server packages and `ci-plan` artifacts are retained for seven days. Partial reruns require the selected original fixture within that window; after expiry, rerun the whole workflow. Source, commit, JDK, build inputs and checksum validation remain enforced, with no cross-run success reuse or new cross-job SDK cache.

Selection uses the cumulative PR diff, so appending documentation to a source PR cannot bypass its tests. Entry planning verifies the event head, checkout merge and live PR identity. A known mismatch fails planning; unverified scope conservatively selects mainline modules and images, without enabling unrelated high-cost compatibility suites. Selection summaries include input paths and optional validation flags. The local diagnostic `gate` command remains available, but CI has no post-test metadata lookup or independent aggregate check.

CodeQL keeps its existing analysis identity, configured languages and upload permissions. Its source/SDK build only runs for Java scans; dependency review belongs to the selected dependency audit. Current and legacy runtime checks compile Toolchain on Java 17 while the official Server 1.7 service uses its own Java 11 JVM.

The dependency inventory audit includes root `.mvn/**`, POMs, dependency manifests (including Go `go.mod` and `go.sum`), assembly/license inputs and staged Maven settings. It runs from the repository root with the same root Maven configuration, separately from the entry license checks. On pull requests, the selected audit also runs dependency review.

For rollout, confirm live branch protection has reconciled to an empty context list; required approving review count and stale-review dismissal remain configured. Until reconciliation, the real entry `check-license-header` still satisfies the previous rule. A new PR head cancels older first attempts; native reruns use separate groups and cannot cancel the current head.

The parent-metadata Maven regression runs once in the entry for selected Java consumers or its own fixture changes, using a temporary Maven repository. Documentation and Go-only PRs skip this extra regression; the manual fixture workflow also runs this regression.

Validate policy and runtime behavior locally:

```bash
python3 -m unittest discover -s .github/scripts -p 'test_*.py'
actionlint
```

Upstream compatibility runs in weekly full validation and on demand against the latest ASF master, resolved once to an immutable commit for each run. It reuses the SDK build, core compilation, Client HTTP tests, Loader and Hubble backend unit tests, and Tools functional tests; Hadoop, Spark/Flink engine matrices and frontend browser tests remain in module CI. Its default full mode does not change the locked Module validation baseline; the native manual workflow also offers the same locked-baseline smoke mode used after merge.

The floating upstream check selects `hugegraph.version` from the verified SDK manifest for every consumer build and test. The same version is passed to SDK packaging verification through `CANDIDATE_SDK_VERSION`; provenance, local-install and artifact-hash checks remain enforced. This lets Server master advance its Maven version without accidentally testing a released SDK. Fixed-baseline jobs retain their default SDK version.
