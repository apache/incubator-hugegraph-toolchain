package com.baidu.hugegraph.loader.source.jdbc.auth;

import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.loader.source.jdbc.auth.util.LoginUtil;
import com.baidu.hugegraph.util.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;

import java.util.Map;

public class KerberosLogin implements Authentication {

    public static final Logger LOG = Log.logger(KerberosLogin.class);

    private static final String ZOOKEEPER_DEFAULT_LOGIN_CONTEXT_NAME = "Client";
    private static final String ZOOKEEPER_SERVER_PRINCIPAL_KEY =
            "zookeeper.server.principal";

    private  Configuration conf = null;
    private  String krb5File = null;
    private  String userName = null;
    private  String userKeytabFile = null;
    private  String zkQuorum = null;
    private  String auth = null;
    private  String saslQop = null;
    private  String zooKeeperNamespace = null;
    private  String serviceDiscoveryMode = null;
    private  String principal = null;

    @Override
    public String auth(JDBCSource source) throws Exception {
        conf = new Configuration();
        String zookeeperPrincipal = "zookeeper/hadoop";
        Map<String, String> clientInfo = source.getPrincipals();
        zkQuorum = clientInfo.get("zk.quorum");
        auth = clientInfo.get("auth");
        saslQop = clientInfo.get("sasl.qop");
        zooKeeperNamespace = clientInfo.get("zooKeeperNamespace");
        serviceDiscoveryMode = clientInfo.get("serviceDiscoveryMode");
        principal = clientInfo.get("principal");
        userName = clientInfo.get("user.name");
        String sslEnable = clientInfo.get("ssl");
        StringBuilder builder = new StringBuilder();
        if ("KERBEROS".equalsIgnoreCase(auth)) {
            try {
                userKeytabFile = clientInfo.get("user.keytab");
                krb5File = clientInfo.get("krb5.conf");
                System.setProperty("java.security.krb5.conf", krb5File);
                zookeeperPrincipal =  clientInfo.get("zookeeperPrincipal");
                if (StringUtils.isEmpty(zookeeperPrincipal)) {
                    zookeeperPrincipal = userName;
                }
                LoginUtil.setJaasConf(ZOOKEEPER_DEFAULT_LOGIN_CONTEXT_NAME,
                                      userName, userKeytabFile);
                LoginUtil.setZookeeperServerPrincipal(
                         ZOOKEEPER_SERVER_PRINCIPAL_KEY,
                         zookeeperPrincipal);
                Configuration conf = new Configuration();
                conf.set("hadoop.security.authentication", "kerberos");
                UserGroupInformation.setConfiguration(conf);
                UserGroupInformation.loginUserFromKeytab(userName,
                                                         userKeytabFile);
                LoginUtil.login(userName, userKeytabFile, krb5File, this.conf);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                throw e;
            }
            if (!StringUtils.isEmpty(sslEnable)) {
                builder.append(";ssl=")
                       .append(sslEnable);
            }
            builder.append(";serviceDiscoveryMode=")
                   .append(serviceDiscoveryMode)
                   .append(";zooKeeperNamespace=")
                   .append(zooKeeperNamespace);
            if (!StringUtils.isEmpty(saslQop)) {
                builder.append(";sasl.qop=")
                       .append(saslQop);
            }
            builder.append(";auth=")
                   .append(auth)
                   .append(";principal=")
                   .append(principal)
                   .append(";");
            LOG.info("builder:" + builder);
        } else {
            builder.append(";serviceDiscoveryMode=")
                   .append(serviceDiscoveryMode)
                   .append(";zooKeeperNamespace=")
                   .append(zooKeeperNamespace)
                   .append(";auth=none");
        }
        return builder.toString();

    }
}
