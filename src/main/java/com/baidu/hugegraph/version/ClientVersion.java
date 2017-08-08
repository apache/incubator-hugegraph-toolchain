package com.baidu.hugegraph.version;

import com.baidu.hugegraph.util.VersionUtil;
import com.baidu.hugegraph.util.VersionUtil.Version;

public class ClientVersion {

    static {
        // Check versions of the dependency packages
        ClientVersion.check();
    }

    public static final String NAME = "hugegraph-client";

    public static final Version VERSION = Version.of(ClientVersion.class);

    public static final void check() {
        // Check version of hugegraph-common
        VersionUtil.check(CommonVersion.VERSION, "1.2", "1.3",
                          CommonVersion.NAME);
    }
}
