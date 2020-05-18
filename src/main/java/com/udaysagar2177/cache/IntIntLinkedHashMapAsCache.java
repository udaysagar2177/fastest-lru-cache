package com.udaysagar2177.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache implementation on top of {@link LinkedHashMap}.
 *
 * @author uday.
 */
public class IntIntLinkedHashMapAsCache {

    private static final int NULL = -1;

    private final LinkedHashMap<Integer, Integer> mapAsCache;

    @SuppressWarnings("serial")
    public IntIntLinkedHashMapAsCache(int cacheSize, float loadFactor) {
        this.mapAsCache = new LinkedHashMap<Integer, Integer>(cacheSize, loadFactor, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > cacheSize;
            }
        };
    }

    /**
     * Clears the cache for re-use.
     */
    public void clear() {
        mapAsCache.clear();
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise {@link this#NULL} is returned.
     */
    public int put(int key, int value) {
        Integer previousValue = mapAsCache.put(key, value);
        return previousValue == null ? NULL : previousValue;
    }

    /**
     * Returns the value associated with the given key, otherwise {@link this#NULL} is returned.
     */
    public int get(int key) {
        return mapAsCache.getOrDefault(key, NULL);
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise {@link this#NULL} is returned.
     */
    public int remove(int key) {
        Integer removedInteger = mapAsCache.remove(key);
        return removedInteger == null ? NULL : removedInteger;
    }

    /**
     * Returns the size of the cache.
     */
    public int size() {
        return mapAsCache.size();
    }

    /**
     * Returns the underlying map for test purposes.
     */
    public LinkedHashMap<Integer, Integer> getMap() {
        return mapAsCache;
    }
}
