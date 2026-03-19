package org.apache.hugegraph.unit;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.driver.HugeClientBuilder;
import org.apache.hugegraph.rest.ClientException;
import org.junit.Assert;
import org.junit.Test;

public class HugeClientBuilderTest {

    @Test
    public void testBuilderWithSkipRequiredChecks() {
        // Should not throw IllegalArgumentException when skipRequiredChecks is true and graph is null
        HugeClientBuilder builder = new HugeClientBuilder("http://127.0.0.1:8080", "DEFAULT", null, true);
        try {
            builder.build();
        } catch (IllegalArgumentException e) {
            Assert.fail("Should not throw IllegalArgumentException when skipRequiredChecks is true, but got: " + e.getMessage());
        } catch (Exception e) {
            // Expected since there is probably no server running at localhost:8080
            // The fact we reach here means the bypass of graph/url check was successful.
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithoutSkipRequiredChecks() {
        // Should throw exception when skipRequiredChecks is false and graph is null
        HugeClientBuilder builder = new HugeClientBuilder("http://127.0.0.1:8080", "DEFAULT", null, false);
        builder.build();
    }

    @Test
    public void testHugeClientBuilderMethod() {
        // Should not throw IllegalArgumentException
        HugeClientBuilder builder = HugeClient.builder("http://127.0.0.1:8080", "DEFAULT", null, true);
        try {
            builder.build();
        } catch (IllegalArgumentException e) {
            Assert.fail("Should not throw IllegalArgumentException when skipRequiredChecks is true, but got: " + e.getMessage());
        } catch (Exception e) {
            // Expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHugeClientBuilderMethodWithoutSkip() {
        // Should throw exception
        HugeClientBuilder builder = HugeClient.builder("http://127.0.0.1:8080", "DEFAULT", null);
        builder.build();
    }
}
