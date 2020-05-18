package com.udaysagar2177.cache;

import com.koloboke.collect.hash.HashConfig;
import com.koloboke.collect.map.IntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;

/**
 * LRU cache implementation that uses {@link IntObjMap<Entry>} as Map and holds Doubly Linked
 * List using {@link Entry}.
 *
 * {@link IntObjMap<Entry>} stores key to {@link Entry} mapping. {@link Entry} has the final
 * value associated with the given key, along with pointers to left and right {@link Entry}
 * objects to preserve access order for LRU functionality.
 *
 * Storing {@link Entry} inside {@link this#entriesArray} allows us to initialize objects at
 * cache creation and continue re-using them throughout cache resets and lifetime. This avoids
 * unnecessary garbage creation.
 *
 * Note: due to NULL being the empty key and for simplicity, allowed keys are [0, Int.MAX) and
 * values are [Int.MIN, INT.MAX]. If negative integers need to stored as keys, there is a
 * workaround by storing the free key and {@link Entry} associated with it separately. Then,
 * put(key, value), get(key) and remove(key) operations should check against free key appropriately.
 *
 * @author uday
 */
public class IntEntryLRUCache {

    private static final int NULL = -1;

    private final int cacheSize;
    private final IntObjMap<Entry> entryLookupMap;
    private final Entry[] entriesArray;

    private int nextEmptyPosition;
    private Entry head = null;
    private Entry tail = null;

    public IntEntryLRUCache(int cacheSize, float loadFactor) {
        this.cacheSize = cacheSize;
        if (cacheSize <= 1) {
            throw new IllegalStateException("Invalid cache size");
        }
        this.entryLookupMap = HashIntObjMaps.getDefaultFactory()
                .withHashConfig(HashConfig.fromLoads(loadFactor / 2, loadFactor * 0.75, loadFactor))
                .newMutableMap(cacheSize);
        this.entriesArray = new Entry[cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            entriesArray[i] = new Entry();
        }
        clear();
    }

    /**
     * Clears the cache for re-use.
     */
    public IntEntryLRUCache clear() {
        nextEmptyPosition = 0;
        entryLookupMap.clear();
        head = null;
        tail = null;
        return this;
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise {@link this#NULL} is returned.
     */
    public int put(int key, int value) {
        Entry entry = entryLookupMap.getOrDefault(key, null);
        if (entry != null) {
            int previousValue = entry.getValue();
            entry.setKeyValue(key, value);
            if (entry != tail) {
                removeEntry(entry);
                addEntry(entry);
            }
            return previousValue;
        } else {
            if (entryLookupMap.size() >= cacheSize) {
                entry = head;
                entryLookupMap.remove(entry.getKey());
                removeEntry(entry);
            } else {
                entry = entriesArray[nextEmptyPosition];
                nextEmptyPosition++;
            }
            entry.setKeyValue(key, value);
            entry.setLeft(null);
            entry.setRight(null);
            addEntry(entry);
            entryLookupMap.put(key, entry);
            return NULL;
        }
    }

    /**
     * Returns the value associated with the given key, otherwise {@link this#NULL} is returned.
     */
    public int get(int key) {
        Entry entry = entryLookupMap.getOrDefault(key, null);
        if (entry == null) {
            return NULL;
        }
        if (entry != tail) {
            removeEntry(entry);
            addEntry(entry);
        }
        return entry.getValue();
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise {@link this#NULL} is returned.
     */
    public int remove(int key) {
        Entry removedEntry = entryLookupMap.remove(key);
        if (removedEntry == null) {
            return NULL;
        }
        removeEntry(removedEntry);
        return removedEntry.getValue();
    }

    /**
     * Returns the size of the cache.
     */
    public int size() {
        return entryLookupMap.size();
    }

    private void removeEntry(Entry entry) {
        if (entry.getLeft() != null) {
            entry.getLeft().setRight(entry.getRight());
        } else {
            head = entry.getRight();
        }
        if (entry.getRight() != null) {
            entry.getRight().setLeft(entry.getLeft());
        } else {
            tail = entry.getLeft();
        }
    }

    private void addEntry(Entry entry) {
        if (tail != null) {
            tail.setRight(entry);
            entry.setLeft(tail);
            entry.setRight(null);
        }
        tail = entry;
        if (head == null) {
            head = tail;
        }
    }

    private class Entry {

        private int key;
        private int value;
        private Entry right;
        private Entry left;

        int getKey() {
            return key;
        }

        void setKeyValue(int key, int value) {
            this.key = key;
            this.value = value;
        }

        int getValue() {
            return value;
        }

        Entry getLeft() {
            return left;
        }

        void setLeft(Entry left) {
            this.left = left;
        }

        Entry getRight() {
            return right;
        }

        void setRight(Entry right) {
            this.right = right;
        }
    }
}
