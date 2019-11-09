package com.baidu.hugegraph.loader.test.functional;

import com.baidu.hugegraph.loader.HugeGraphLoader;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.baidu.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class HDFSLoadTest extends LoadTest {

    private static IOUtil ioUtil;

    static {
        String path = "/profile.properties";
        // Read properties defined in maven profile
        try (InputStream is = FileLoadTest.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(is);
            String storePath = properties.getProperty("store_path");
            ioUtil = new HDFSUtil(storePath);
        } catch (IOException e) {
            throw new RuntimeException(
                      "Failed to read properties defined in maven profile", e);
        }
    }

    @BeforeClass
    public static void setUp() {
        clearFileData();
        clearServerData();
    }

    @AfterClass
    public static void tearDown() {
        ioUtil.close();
    }

    @Before
    public void init() {
    }

    @After
    public void clear() {
        clearFileData();
        clearServerData();
    }

    private static void clearFileData() {
        ioUtil.delete();
    }


    @Test
    public void testHDFSWithDefaultFs() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_default_fs/struct.json"),
                "-s", configPath("hdfs_with_default_fs/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }

    @Test
    public void testHDFSWithDefaultFsEmpty() {
        ioUtil.write("vertex_person.csv",
                "name,age,city",
                "marko,29,Beijing",
                "vadas,27,Hongkong",
                "josh,32,Beijing",
                "peter,35,Shanghai",
                "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_default_fs_empty/struct.json"),
                "-s", configPath("hdfs_with_default_fs_empty/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testHDFSWithDefaultCoreSite() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_default_core_site/struct.json"),
                "-s", configPath("hdfs_with_default_core_site/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }

    @Test
    public void testHDFSWithCoreSitePath() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_core_site_path/struct.json"),
                "-s", configPath("hdfs_with_core_site_path/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);
        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }

    @Test
    public void testHDFSWithCoreSitePathEmpty() {
        ioUtil.write("vertex_person.csv",
                "name,age,city",
                "marko,29,Beijing",
                "vadas,27,Hongkong",
                "josh,32,Beijing",
                "peter,35,Shanghai",
                "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_core_site_path_empty/struct.json"),
                "-s", configPath("hdfs_with_core_site_path_empty/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testHDFSWithInvalidCoreSitePath() {
        ioUtil.write("vertex_person.csv",
                "name,age,city",
                "marko,29,Beijing",
                "vadas,27,Hongkong",
                "josh,32,Beijing",
                "peter,35,Shanghai",
                "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_invalid_core_site_path/struct.json"),
                "-s", configPath("hdfs_with_invalid_core_site_path/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        Assert.assertThrows(LoadException.class, () -> {
            HugeGraphLoader.main(args);
        });
    }

    @Test
    public void testHDFSWithDefaultFsAndCoreSitePath() {
        ioUtil.write("vertex_person.csv",
                     "name,age,city",
                     "marko,29,Beijing",
                     "vadas,27,Hongkong",
                     "josh,32,Beijing",
                     "peter,35,Shanghai",
                     "\"li,nary\",26,\"Wu,han\"");

        String[] args = new String[]{
                "-f", configPath("hdfs_with_defaultfs_core_site_path/struct.json" ),
                "-s", configPath("hdfs_with_defaultfs_core_site_path/schema.groovy" ),
                "-g", GRAPH,
                "-h", SERVER,
                "--num-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(5, vertices.size());
    }
}
