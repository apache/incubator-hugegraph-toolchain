package com.baidu.hugegraph.loader.test.functional;

import com.baidu.hugegraph.loader.exception.ParseException;
import com.baidu.hugegraph.testutil.Assert;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class AsyncThrowsAssert extends  Assert {

    public static void assertThrows(Class<? extends Throwable> throwable,
                                    Assert.ThrowableRunnable runnable,
                                    Consumer<Throwable> exceptionConsumer) {
        boolean fail = false;
        try {
            runnable.run();
            fail = true;
        } catch (Throwable e) {
            if (CompletionException.class.isInstance(e)) {
                e=e.getCause();
            }
            if (!throwable.isInstance(e)) {
                Assert.fail(String.format(
                        "Bad exception type %s(expected %s)",
                        e.getClass().getName(), throwable.getName()));
            }
            exceptionConsumer.accept(e);
        }
        if (fail) {
            Assert.fail(String.format(
                    "No exception was thrown(expected %s)",
                    throwable.getName()));
        }
    }
    public static void assertThrows(Class<? extends Throwable> throwable,
                                    ThrowableRunnable runnable) {
        assertThrows(throwable, runnable, e -> {
            System.err.println(e);
        });
    }

}
