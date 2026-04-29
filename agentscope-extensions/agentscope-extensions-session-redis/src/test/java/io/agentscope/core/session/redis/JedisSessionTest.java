/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.core.session.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.shutdown.GracefulShutdownManager;
import io.agentscope.core.shutdown.ShutdownInterruptedState;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Unit tests for {@link RedisSession} with Jedis client.
 */
@DisplayName("RedisSession with Jedis Tests")
class JedisSessionTest {

    private UnifiedJedis unifiedJedis;

    @BeforeEach
    void setUp() {
        unifiedJedis = mock(UnifiedJedis.class);
    }

    @Test
    @DisplayName("Should build session with UnifiedJedis")
    void testBuilderWithUnifiedJedis() {
        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();
        assertNotNull(session);
    }

    @Test
    @DisplayName("Should throw exception when building with empty prefix")
    void testBuilderWithEmptyPrefix() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RedisSession.builder().jedisClient(unifiedJedis).keyPrefix("").build());
    }

    @Test
    @DisplayName("Should save and get single state correctly")
    void testSaveAndGetSingleState() {
        String stateJson = "{\"value\":\"test_value\",\"count\":42}";
        when(unifiedJedis.get("agentscope:session:session1:testModule")).thenReturn(stateJson);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        TestState state = new TestState("test_value", 42);

        // Save state
        session.save(sessionKey, "testModule", state);

        // Verify save operations
        verify(unifiedJedis).set(anyString(), anyString());
        verify(unifiedJedis).sadd("agentscope:session:session1:_keys", "testModule");

        // Get state
        Optional<TestState> loaded = session.get(sessionKey, "testModule", TestState.class);
        assertTrue(loaded.isPresent());
        assertEquals("test_value", loaded.get().value());
        assertEquals(42, loaded.get().count());
    }

    @Test
    @DisplayName("Should save and get list state correctly")
    void testSaveAndGetListState() {
        when(unifiedJedis.llen("agentscope:session:session1:testList:list")).thenReturn(0L);
        when(unifiedJedis.lrange("agentscope:session:session1:testList:list", 0, -1))
                .thenReturn(
                        List.of(
                                "{\"value\":\"value1\",\"count\":1}",
                                "{\"value\":\"value2\",\"count\":2}"));

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        List<TestState> states = List.of(new TestState("value1", 1), new TestState("value2", 2));

        // Save list state
        session.save(sessionKey, "testList", states);

        // Verify rpush was called for each item
        verify(unifiedJedis, atLeast(1)).rpush(anyString(), anyString());

        // Get list state
        List<TestState> loaded = session.getList(sessionKey, "testList", TestState.class);
        assertEquals(2, loaded.size());
        assertEquals("value1", loaded.get(0).value());
        assertEquals("value2", loaded.get(1).value());
    }

    @Test
    @DisplayName("Should return empty for non-existent state")
    void testGetNonExistentState() {
        when(unifiedJedis.get("agentscope:session:non_existent:testModule")).thenReturn(null);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("non_existent");
        Optional<TestState> state = session.get(sessionKey, "testModule", TestState.class);
        assertFalse(state.isPresent());
    }

    @Test
    @DisplayName("Should return empty list for non-existent list state")
    void testGetNonExistentListState() {
        when(unifiedJedis.lrange("agentscope:session:non_existent:testList:list", 0, -1))
                .thenReturn(List.of());

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("non_existent");
        List<TestState> states = session.getList(sessionKey, "testList", TestState.class);
        assertTrue(states.isEmpty());
    }

    @Test
    @DisplayName("Should return true when session exists")
    void testSessionExists() {
        when(unifiedJedis.exists("agentscope:session:session1:_keys")).thenReturn(true);
        when(unifiedJedis.scard("agentscope:session:session1:_keys")).thenReturn(2L);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        assertTrue(session.exists(sessionKey));
    }

    @Test
    @DisplayName("Should return false when session does not exist")
    void testSessionDoesNotExist() {
        when(unifiedJedis.exists("agentscope:session:session1:_keys")).thenReturn(false);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        assertFalse(session.exists(sessionKey));
    }

    @Test
    @DisplayName("Should delete session correctly")
    void testDeleteSession() {
        Set<String> trackedKeys = new HashSet<>();
        trackedKeys.add("module1");
        trackedKeys.add("module2:list");
        when(unifiedJedis.smembers("agentscope:session:session1:_keys")).thenReturn(trackedKeys);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        session.delete(sessionKey);

        // Verify del was called with the keys
        verify(unifiedJedis).smembers("agentscope:session:session1:_keys");
    }

    @Test
    @DisplayName("Should delete single state key and remove tracking metadata")
    void testDeleteSingleStateKey() {
        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        session.delete(sessionKey, "shutdown_interrupted");

        verify(unifiedJedis).del(new String[] {"agentscope:session:session1:shutdown_interrupted"});
        verify(unifiedJedis).srem("agentscope:session:session1:_keys", "shutdown_interrupted");
    }

    @Test
    @DisplayName("Should not return single state after deleting it")
    void testDeleteSingleStateKeyRemovesPersistedValue() {
        RedisSession session =
                RedisSession.builder()
                        .clientAdapter(new StatefulRedisClientAdapter())
                        .keyPrefix("agentscope:session:")
                        .build();

        SessionKey sessionKey = SimpleSessionKey.of("session1");
        session.save(sessionKey, "shutdown_interrupted", new TestState("interrupted", 1));

        assertTrue(session.get(sessionKey, "shutdown_interrupted", TestState.class).isPresent());

        session.delete(sessionKey, "shutdown_interrupted");

        assertFalse(session.get(sessionKey, "shutdown_interrupted", TestState.class).isPresent());
    }

    @Test
    @DisplayName("Should clear shutdown interrupted flag after first manager check")
    void testManagerCheckAndClearShutdownInterruptedWithRedisSession() {
        GracefulShutdownManager manager = GracefulShutdownManager.getInstance();
        manager.resetForTesting();
        try {
            RedisSession session =
                    RedisSession.builder()
                            .clientAdapter(new StatefulRedisClientAdapter())
                            .keyPrefix("agentscope:session:")
                            .build();
            SessionKey sessionKey = SimpleSessionKey.of("session1");
            Agent agent = mock(Agent.class);
            when(agent.getAgentId()).thenReturn("agent-1");

            session.save(sessionKey, "shutdown_interrupted", new ShutdownInterruptedState(true));
            manager.bindSession(agent, session, sessionKey);

            assertTrue(manager.checkAndClearShutdownInterrupted(agent));
            assertFalse(manager.checkAndClearShutdownInterrupted(agent));
        } finally {
            manager.resetForTesting();
        }
    }

    @Test
    @DisplayName("Should list all session keys")
    void testListSessionKeys() {
        Set<String> keysKeys = new HashSet<>();
        keysKeys.add("agentscope:session:session1:_keys");
        keysKeys.add("agentscope:session:session2:_keys");
        ScanResult<String> scanResult = mock(ScanResult.class);
        when(scanResult.getResult()).thenReturn(new ArrayList<>(keysKeys));
        when(scanResult.getCursor()).thenReturn(ScanParams.SCAN_POINTER_START);
        when(unifiedJedis.scan(anyString(), any(ScanParams.class))).thenReturn(scanResult);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        Set<SessionKey> sessionKeys = session.listSessionKeys();
        assertEquals(2, sessionKeys.size());
        assertTrue(sessionKeys.contains(SimpleSessionKey.of("session1")));
        assertTrue(sessionKeys.contains(SimpleSessionKey.of("session2")));
    }

    @Test
    @DisplayName("Should clear all sessions")
    void testClearAllSessions() {
        Set<String> allKeys = new HashSet<>();
        allKeys.add("agentscope:session:s1:module1");
        allKeys.add("agentscope:session:s1:_keys");
        allKeys.add("agentscope:session:s2:module1");
        allKeys.add("agentscope:session:s2:_keys");
        ScanResult<String> scanResult = mock(ScanResult.class);
        when(scanResult.getResult()).thenReturn(new ArrayList<>(allKeys));
        when(scanResult.getCursor()).thenReturn(ScanParams.SCAN_POINTER_START);
        when(unifiedJedis.scan(anyString(), any(ScanParams.class))).thenReturn(scanResult);

        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();

        StepVerifier.create(session.clearAllSessions()).expectNext(4).verifyComplete();
    }

    @Test
    @DisplayName("Should close jedis client when closing session")
    void testCloseShutsDownClient() {
        RedisSession session =
                RedisSession.builder()
                        .jedisClient(unifiedJedis)
                        .keyPrefix("agentscope:session:")
                        .build();
        session.close();
        verify(unifiedJedis).close();
    }

    /** Simple test state record for testing. */
    public record TestState(String value, int count) implements State {}

    private static class StatefulRedisClientAdapter implements RedisClientAdapter {

        private final Map<String, String> values = new HashMap<>();
        private final Map<String, Set<String>> sets = new HashMap<>();

        @Override
        public void set(String key, String value) {
            values.put(key, value);
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void rightPushList(String key, String value) {}

        @Override
        public List<String> rangeList(String key, long start, long end) {
            return List.of();
        }

        @Override
        public long getListLength(String key) {
            return 0;
        }

        @Override
        public void deleteKeys(String... keys) {
            for (String key : keys) {
                values.remove(key);
            }
        }

        @Override
        public void addToSet(String key, String member) {
            sets.computeIfAbsent(key, unused -> new HashSet<>()).add(member);
        }

        @Override
        public void removeFromSet(String key, String member) {
            Set<String> members = sets.get(key);
            if (members != null) {
                members.remove(member);
            }
        }

        @Override
        public Set<String> getSetMembers(String key) {
            return sets.getOrDefault(key, Set.of());
        }

        @Override
        public long getSetSize(String key) {
            return getSetMembers(key).size();
        }

        @Override
        public boolean keyExists(String key) {
            return values.containsKey(key) || sets.containsKey(key);
        }

        @Override
        public Set<String> findKeysByPattern(String pattern) {
            return Set.of();
        }

        @Override
        public void close() {}
    }
}
