# Loader CI Troubleshooting Guide

## Quick Reference

### Issue: Workflow fails with "Could not pull mysql:5.7"

**Cause**: Docker image pull failed or Docker not available

**Solution**: The improved script now:
1. Checks for Docker availability
2. Falls back to native MySQL if Docker fails
3. Retries connection until MySQL is ready

**What to check**:
- Is Docker running? `docker ps`
- Is MySQL native package installed? `mysql --version`
- Check logs for "Waiting for MySQL connection..." messages

---

### Issue: Hadoop namenode won't start

**Cause**: Directory permissions or format failure

**Solution**: The improved script now:
1. Validates directory creation
2. Sets proper permissions automatically
3. Provides detailed error messages

**What to check**:
- Check `/opt/hdfs/name` and `/opt/hdfs/data` exist
- Verify Hadoop environment variables are set: `echo $HADOOP_HOME`
- Run `jps` to see running Java processes

---

### Issue: "Failed to clone HugeGraph repository"

**Cause**: Network timeout or invalid commit ID

**Solution**: The improved script now:
1. Applies 300-second timeout to prevent hanging
2. Validates checkout success
3. Returns to working directory on error

**What to check**:
- Is the commit ID valid? `git rev-parse <commit_id>`
- Is network connectivity good? `ping github.com`
- Check git clone logs for authentication issues

---

### Issue: Coverage upload fails

**Cause**: Wrong file path or missing token

**Solution**: The improved workflow now:
1. Uses correct path: `hugegraph-loader/target/jacoco.xml`
2. Only uploads on Java 11 (prevents duplicates)
3. Gracefully handles missing Codecov token

**What to check**:
- Is `hugegraph-loader/target/jacoco.xml` present after tests?
- Is CODECOV_TOKEN secret configured?
- Check if `fail_ci_if_error: false` is set (graceful failure)

---

## Common Errors and Solutions

### Error: "java.lang.UnsupportedClassVersionError"

**Symptom**: Java compilation fails with version error

**Cause**: Java version mismatch between compilation and runtime

**Solution**: 
- Ensure Java version matches pom.xml requirements
- Check `maven.compiler.source` and `target` properties

```bash
# Check current Java version
java -version

# Check compiled class version
javap -v path/to/CompiledClass.class | grep "major version"
```

---

### Error: "Cannot create temporary directory"

**Symptom**: Hadoop or test setup fails with directory errors

**Cause**: Insufficient permissions or disk space

**Solution**:
- Check disk space: `df -h`
- Check permissions: `ls -la /opt/`
- Ensure `/tmp` is writable: `touch /tmp/test && rm /tmp/test`

---

### Error: "Connection refused" on port 3306

**Symptom**: MySQL tests fail immediately

**Cause**: MySQL not running or not accepting connections

**Solution**:
- Check if MySQL is running: `ps aux | grep mysql`
- Check port binding: `netstat -tuln | grep 3306`
- Run the install script manually to see detailed errors

```bash
hugegraph-loader/assembly/travis/install-mysql.sh load_test root
```

---

### Error: "timeout waiting for HugeGraph"

**Symptom**: Tests start before HugeGraph is ready

**Cause**: Services starting but not fully initialized

**Solution**:
- Increase wait time in script
- Check HugeGraph logs in `apache-hugegraph-*/logs/`
- Verify ports are bound: `netstat -tuln | grep 8080`

---

## Performance Optimization

### To speed up CI runs:

1. **Cache Maven artifacts** (already configured):
   ```yaml
   - name: Cache Maven packages
     uses: actions/cache@v4
     with:
       path: ~/.m2
       key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
   ```

2. **Skip unnecessary tests**:
   - Use test profiles for focused testing
   - Consider running full test suite only on master

3. **Parallel execution**:
   - Java 11 and 17 tests already run in parallel
   - Consider adding Java 21 for even better coverage

---

## Debugging Tips

### Enable verbose logging

```bash
# For Maven
mvn test -X -P unit

# For Hadoop
export HADOOP_DATANODE_HEAPSIZE=1024
sbin/hadoop-daemon.sh start datanode

# For MySQL
docker logs load_test

# For HugeGraph
tail -f apache-hugegraph-*/logs/hugegraph-server.log
```

### Check service status

```bash
# Hadoop
jps

# MySQL
mysql -u root -proot -e "SELECT VERSION();"

# HugeGraph
curl -s http://localhost:8080 | head

# Ports
netstat -tuln | grep LISTEN
```

---

## Log Analysis

### Where to find logs

- **GitHub Actions**: https://github.com/apache/incubator-hugegraph-toolchain/actions
- **HugeGraph**: `apache-hugegraph-*/logs/hugegraph-server.log`
- **Test output**: `hugegraph-loader/target/surefire-reports/`
- **Coverage**: `hugegraph-loader/target/site/jacoco/`

### What to look for in logs

1. **Initialization messages**: Should see "Starting..." for each service
2. **Error lines**: Search for `ERROR` and `FATAL`
3. **Warnings**: Search for `WARNING` to find potential issues
4. **Connection logs**: Look for "connected to", "listening on", etc.

---

## Environment Variables

Key environment variables used in the workflow:

| Variable | Value | Purpose |
|----------|-------|---------|
| `USE_STAGE` | `true` | Use Apache stage Maven repository |
| `TRAVIS_DIR` | `hugegraph-loader/assembly/travis` | Path to setup scripts |
| `COMMIT_ID` | `5b3d295` | HugeGraph server commit to test against |
| `DB_USER` | `root` | MySQL username |
| `DB_PASS` | `root` | MySQL password |
| `DB_DATABASE` | `load_test` | Test database name |
| `JAVA_VERSION` | `11` or `17` | JDK version to test |

---

## Update Schedule

The workflow should be updated when:

1. **New Java LTS versions released**: Add to matrix
2. **HugeGraph major version released**: Update COMMIT_ID
3. **Action versions updated**: Check for deprecations
4. **Test infrastructure changes**: Update setup scripts

Current schedule:
- HugeGraph commit ID: Update every 3-6 months
- Java versions: Update when new LTS released
- Actions: Check GitHub notifications quarterly

---

## Rollback Instructions

If issues occur after deployment:

1. **Revert workflow file**:
   ```bash
   git revert <commit-hash>
   git push
   ```

2. **Run workflow with old script**:
   ```bash
   git checkout main -- .github/workflows/loader-ci.yml
   ```

3. **Check git log for changes**:
   ```bash
   git log --oneline .github/workflows/loader-ci.yml | head
   ```

---

## Contact and Support

For issues or questions:
1. Check this guide first
2. Review GitHub Actions logs
3. Search existing issues
4. Open a new issue with logs and reproduction steps

---

## Version History

| Date | Change | Status |
|------|--------|--------|
| 2025-12-08 | Initial CI fixes | ✅ Complete |
| TBD | Add Java 21 support | ⏳ Pending |
| TBD | Docker Compose setup | 📋 Planned |
| TBD | Performance monitoring | 📋 Planned |

