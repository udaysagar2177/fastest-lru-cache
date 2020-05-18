package com.udaysagar2177.cache;

import java.util.Arrays;

import com.koloboke.collect.hash.HashConfig;
import com.koloboke.collect.map.IntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;

/**
 * LRU cache implementation that uses {@link IntIntMap} as Map and holds Doubly Linked
 * List using an integer array.
 *
 * {@link IntIntMap} maps a key to a position in the {@link this#entriesArray}.
 * Given a position, {@link Entry} is a view that allows us to read key, value, left and right
 * values from an integer array. That looks like:
 *
 *    0      1       2       3      4      5       6      7      8      9      10     12
 *  ----------------------------------------------------------------------------------------
 *  | key | value | left | right | key | value | left | right | key | value | left | right |
 *  ----------------------------------------------------------------------------------------
 *
 * Storing the information inside an integer array provides compact data storage. We use
 * {@link this#entryFlyweight} and {@link this#extraFlyweight} to read data from integer array, so
 * no objects are created on the fly and there is no garbage collection concern.
 *
 * Note: due to NULL being the empty key and for simplicity, allowed keys are [0, Int.MAX) and
 * values are [Int.MIN, INT.MAX]. If negative integers need to stored as keys, there is a
 * workaround by storing the free key and {@link Entry} position associated with it separately.
 * Then, put(key, value), get(key) and remove(key) operations should check against free key
 * appropriately.
 *
 * @author uday
 */
public class IntEntryViewLRUCache {

    public static final int NULL = -1;

    private final Entry entryFlyweight = new Entry();
    private final Entry extraFlyweight = new Entry();
    private final Entry head = new Entry();
    private final Entry tail = new Entry();

    private final int cacheSize;
    private final IntIntMap entryPositionLookupMap;
    private final int[] entriesArray;

    private int nextEmptyPosition;

    public IntEntryViewLRUCache(int cacheSize, float loadFactor) {
        this.cacheSize = cacheSize;
        if (cacheSize <= 1) {
            throw new IllegalStateException("Invalid cache size");
        }
        this.entryPositionLookupMap = HashIntIntMaps.getDefaultFactory()
                .withDefaultValue(NULL)
                .withHashConfig(HashConfig.fromLoads(loadFactor / 2, loadFactor * 0.75, loadFactor))
                .newMutableMap(cacheSize);
        this.entriesArray = new int[cacheSize * Entry.NUM_INTEGERS_TO_HOLD_ENTRY];
        clear();
    }

    /**
     * Clears the cache for re-use.
     */
    public IntEntryViewLRUCache clear() {
        nextEmptyPosition = 0;
        Arrays.fill(entriesArray, NULL);
        entryPositionLookupMap.clear();
        entryFlyweight.reset(NULL);
        extraFlyweight.reset(NULL);
        head.reset(NULL);
        tail.reset(NULL);
        return this;
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise {@link this#NULL} is returned.
     */
    public int put(int key, int value) {
        int position = entryPositionLookupMap.getOrDefault(key, NULL);
        if (position != NULL) {
            entryFlyweight.reset(position);
            int previousValue = entryFlyweight.getValue();
            entryFlyweight.setKeyValue(key, value);
            if (position != tail.getPosition()) {
                removeEntry(entryFlyweight);
                addEntry(entryFlyweight);
            }
            return previousValue;
        }
        if (entryPositionLookupMap.size() >= cacheSize) {
            entryFlyweight.reset(head.getPosition());
            entryPositionLookupMap.remove(entryFlyweight.getKey());
            removeEntry(entryFlyweight);
        } else {
            entryFlyweight.reset(nextEmptyPosition);
            nextEmptyPosition += Entry.NUM_INTEGERS_TO_HOLD_ENTRY;
        }
        entryFlyweight.setKeyValue(key, value);
        addEntry(entryFlyweight);
        entryPositionLookupMap.put(key, entryFlyweight.getPosition());
        return NULL;
    }

    /**
     * Returns the value associated with the given key, otherwise {@link this#NULL} is returned.
     */
    public int get(int key) {
        int position = entryPositionLookupMap.getOrDefault(key, NULL);
        if (position == NULL) {
            return NULL;
        }
        entryFlyweight.reset(position);
        if (position != tail.getPosition()) {
            removeEntry(entryFlyweight);
            addEntry(entryFlyweight);
        }
        return entryFlyweight.getValue();
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise {@link this#NULL} is returned.
     */
    public int remove(int key) {
        int position = entryPositionLookupMap.remove(key);
        if (position == NULL) {
            return NULL;
        }
        entryFlyweight.reset(position);
        int removedValue = entryFlyweight.getValue();
        removeEntry(entryFlyweight);
        return removedValue;
    }

    /**
     * Returns the size of the cache.
     */
    public int size() {
        return entryPositionLookupMap.size();
    }

    private void removeEntry(Entry entry) {
        if (entry.getLeft() != NULL) {
            extraFlyweight.reset(entry.getLeft());
            extraFlyweight.setRight(entry.getRight());
        } else {
            head.reset(entry.getRight());
        }

        if (entry.getRight() != NULL) {
            extraFlyweight.reset(entry.getRight());
            extraFlyweight.setLeft(entry.getLeft());
        } else {
            tail.reset(entry.getLeft());
        }
    }

    private void addEntry(Entry entry) {
        if (tail.getPosition() != NULL) {
            tail.setRight(entry.getPosition());
            entry.setLeft(tail.getPosition());
            entry.setRight(NULL);
        }
        tail.reset(entry.getPosition());
        if (head.getPosition() == NULL) {
            head.reset(tail.getPosition());
        }
    }

    private class Entry {

        private static final int KEY_POSITION = 0;
        private static final int VALUE_POSITION = 1;
        private static final int LEFT_INDEX = 2;
        private static final int RIGHT_INDEX = 3;
        private static final int NUM_INTEGERS_TO_HOLD_ENTRY = 4;

        private int position;

        void reset(int position) {
            this.position = position;
        }

        int getPosition() {
            return position;
        }

        void setKeyValue(int key, int value) {
            entriesArray[position + KEY_POSITION] = key;
            entriesArray[position + VALUE_POSITION] = value;
        }

        int getKey() {
            return entriesArray[position + KEY_POSITION];
        }

        int getValue() {
            return entriesArray[position + VALUE_POSITION];
        }

        void setLeft(int left) {
            entriesArray[position + LEFT_INDEX] = left;
        }

        int getLeft() {
            return entriesArray[position + LEFT_INDEX];
        }

        void setRight(int right) {
            entriesArray[position + RIGHT_INDEX] = right;
        }

        int getRight() {
            return entriesArray[position + RIGHT_INDEX];
        }
    }
}