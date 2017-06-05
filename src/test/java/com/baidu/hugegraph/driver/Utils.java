package com.baidu.hugegraph.driver;

import java.util.function.Consumer;

import org.junit.Assert;

public class Utils {

    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Throwable;
    }

    public static void assertThrows(
            Class<? extends Throwable> throwable,
            ThrowableRunnable runnable) {
        assertThrows(throwable, runnable, t -> {
            t.printStackTrace();
        });
    }

    public static void assertThrows(
            Class<? extends Throwable> throwable,
            ThrowableRunnable runnable,
            Consumer<Throwable> exceptionConsumer) {
        boolean fail = false;
        try {
            runnable.run();
            fail = true;
        } catch (Throwable t) {
            exceptionConsumer.accept(t);
            Assert.assertTrue(String.format(
                    "Bad exception type %s(expect %s)",
                    t.getClass(), throwable),
                    throwable.isInstance(t));
        }
        if (fail) {
            Assert.fail(String.format("No exception was thrown(expect %s)",
                    throwable));
        }
    }
}
