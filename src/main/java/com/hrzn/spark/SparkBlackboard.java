package com.hrzn.spark;

import java.util.HashMap;
import java.util.Map;

public class SparkBlackboard {
    private static final Map<String, Key<?>> KEYS = new HashMap<>();

    public static final Key<BootstrapConfig> CONFIG = key("config");

    @SuppressWarnings("unchecked")
    public static <T> Key<T> key(String key) {
        Key<?> existingKey = KEYS.computeIfAbsent(key, s -> new Key<>());
        return (Key<T>) existingKey;
    }

    public static class Key<T> {
        private T value;

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return this.value;
        }
    }
}
