/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.service.auth;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.apache.hugegraph.exception.LoginThrottledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class LoginAttemptGuard {

    private final Cache<String, Attempt> attempts;
    private final LongSupplier clock;
    private final int failureThreshold;
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;

    @Autowired
    public LoginAttemptGuard(
            @Value("${auth.login.failure-threshold}") int failureThreshold,
            @Value("${auth.login.initial-backoff-seconds}") long initialSeconds,
            @Value("${auth.login.max-backoff-seconds}") long maxSeconds,
            @Value("${auth.login.max-tracked-principals}") long maximumSize) {
        this(failureThreshold, TimeUnit.SECONDS.toMillis(initialSeconds),
             TimeUnit.SECONDS.toMillis(maxSeconds), maximumSize,
             System::currentTimeMillis);
    }

    public LoginAttemptGuard(int failureThreshold, long initialBackoffMillis,
                             long maxBackoffMillis, long maximumSize,
                             LongSupplier clock) {
        if (failureThreshold < 1 || initialBackoffMillis < 1L ||
            maxBackoffMillis < initialBackoffMillis || maximumSize < 1L) {
            throw new IllegalArgumentException("Invalid login backoff settings");
        }
        this.clock = clock;
        this.failureThreshold = failureThreshold;
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        // FIXME: For multi-instance or hostile high-cardinality traffic, move this
        // state to a shared rate limiter that preserves active bans under key spray.
        this.attempts = Caffeine.newBuilder()
                                .maximumSize(maximumSize)
                                .expireAfterAccess(maxBackoffMillis * 2L,
                                                   TimeUnit.MILLISECONDS)
                                .build();
    }

    public void checkAllowed(String username, String address) {
        String key = key(username, address);
        long now = this.clock.getAsLong();
        long[] retrySeconds = {0L};
        this.attempts.asMap().compute(key, (ignored, current) -> {
            Attempt attempt = current == null ? Attempt.EMPTY : current;
            if (attempt.blockedUntil > now) {
                retrySeconds[0] = retrySeconds(attempt.blockedUntil - now);
                return attempt;
            }
            if (attempt.inFlight) {
                retrySeconds[0] = 1L;
                return attempt;
            }
            return new Attempt(attempt.failures, attempt.blockedUntil, true);
        });
        if (retrySeconds[0] > 0L) {
            throw new LoginThrottledException(retrySeconds[0]);
        }
    }

    public void recordFailure(String username, String address) {
        String key = key(username, address);
        Attempt attempt = this.attempts.asMap().compute(key, (ignored, current) -> {
            int failures = current == null ? 1 : increment(current.failures);
            long blockedUntil = 0L;
            if (failures >= this.failureThreshold) {
                int exponent = failures - this.failureThreshold;
                long delay = backoff(exponent, this.initialBackoffMillis,
                                     this.maxBackoffMillis);
                blockedUntil = this.clock.getAsLong() + delay;
            }
            return new Attempt(failures, blockedUntil, false);
        });
        if (attempt.failures >= this.failureThreshold) {
            log.warn("Login throttled for principal {}, failures={}, retry={}s",
                     Integer.toHexString(key.hashCode()), attempt.failures,
                     Math.max(1L, (attempt.blockedUntil - this.clock.getAsLong() +
                                  999L) / 1000L));
        }
    }

    public void reset(String username, String address) {
        this.attempts.invalidate(key(username, address));
    }

    public void release(String username, String address) {
        this.attempts.asMap().computeIfPresent(key(username, address),
                                              (ignored, current) ->
                                              new Attempt(current.failures,
                                                          current.blockedUntil,
                                                          false));
    }

    private static String key(String username, String address) {
        return String.valueOf(username) + '|' +
               String.valueOf(address);
    }

    private static int increment(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    private static long retrySeconds(long remainingMillis) {
        return Math.max(1L, (remainingMillis + 999L) / 1000L);
    }

    private static long backoff(int exponent, long initial, long maximum) {
        long delay = initial;
        for (int i = 0; i < exponent && delay < maximum; i++) {
            delay = delay > maximum / 2L ? maximum : delay * 2L;
        }
        return Math.min(delay, maximum);
    }

    private static final class Attempt {

        private static final Attempt EMPTY = new Attempt(0, 0L, false);

        private final int failures;
        private final long blockedUntil;
        private final boolean inFlight;

        private Attempt(int failures, long blockedUntil, boolean inFlight) {
            this.failures = failures;
            this.blockedUntil = blockedUntil;
            this.inFlight = inFlight;
        }
    }
}
