package com.udaysagar2177.cache;

/**
 * Fastest LRU cache implementation that combines Map and Doubly Linked List functionality
 * together on every operation.
 *
 * Map and Doubly Linked List are not two different data structures anymore. Data is stored in an
 * integer array and both map lookups and doubly linked list movements are addressed with respect
 * to data layout. See the article related to this implementation for more details -
 * https://medium.com/@udaysagar.2177/fastest-lru-cache-in-java-c22262de42ad
 *
 * Linear probing scheme which benefits from CPU cache line reads is used to resolve key collisions.
 * Each entry is written across 4 integers (16 bytes), so a 64B cache line read makes 4
 * consecutive entries available for CPU.
 *
 * Note: due to NULL being the empty key and for simplicity, allowed keys are [0, Int.MAX) and
 * values are [Int.MIN, INT.MAX]. If negative integers need to stored as keys, there is a
 * workaround by storing the free key and position associated with it separately. Then,
 * put(key, value), get(key) and remove(key) operations should check against free key appropriately.
 *
 * While the combination of map and doubly linked list is my original work, the map functionality
 * part is inspired from https://github.com/mikvor/hashmapTest.
 *
 * @author uday
 */
public class IntIntLRUCache {

    private static final int NULL = -1;
    private static final int KEY_OFFSET = 0;
    private static final int VALUE_OFFSET = 1;
    private static final int LEFT_OFFSET = 2;
    private static final int RIGHT_OFFSET = 3;
    private static final int NUM_INTEGERS_TO_HOLD_ENTRY = 4;

    static {
        if ((NUM_INTEGERS_TO_HOLD_ENTRY & NUM_INTEGERS_TO_HOLD_ENTRY - 1) != 0) {
            throw new IllegalStateException("Invalid entry size, should be power of 2!");
        }
    }

    private final int cacheSize;
    private final int[] data;
    private final int modulo;
    private final int positionModulo;

    private int size;
    private int headPosition;
    private int tailPosition;

    public IntIntLRUCache(int expectedSize, float fillFactor) {
        this.cacheSize = expectedSize;

        int capacity = calculateArraySize(expectedSize, fillFactor);
        int maxPosition = capacity * NUM_INTEGERS_TO_HOLD_ENTRY;
        modulo = capacity - 1;
        positionModulo = maxPosition - 1;
        data = new int[maxPosition];
        clear();
    }

    /**
     * Clears the cache for re-use.
     */
    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = NULL;
        }
        headPosition = -1;
        tailPosition = -1;
        size = 0;
    }

    /**
     * Inserts key, value into the cache. Returns any previous value associated with the given key,
     * otherwise {@link this#NULL} is returned.
     */
    public int put(int key, int value) {
        if (size > cacheSize) {
            throw new IllegalStateException("Cache size exceeded expected bounds!");
        }
        int position = hash(key);
        int currentPosition = position;
        do {
            if (key == data[currentPosition + KEY_OFFSET]) {
                int previousValue = data[currentPosition + VALUE_OFFSET];
                data[currentPosition + KEY_OFFSET] = key;
                data[currentPosition + VALUE_OFFSET] = value;
                removeEntry(currentPosition);
                addEntry(currentPosition);
                return previousValue;
            }
            if (data[currentPosition + KEY_OFFSET] == NULL) {
                if (size >= cacheSize) {
                    int currentHeadPosition = headPosition;
                    removeEntry(currentHeadPosition);
                    shiftKeys(currentHeadPosition);
                    --size;
                    break;
                } else {
                    data[currentPosition + KEY_OFFSET] = key;
                    data[currentPosition + VALUE_OFFSET] = value;
                    addEntry(currentPosition);
                    ++size;
                    return NULL;
                }
            }
            currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
        } while (true);

        position = hash(key);
        currentPosition = position;
        do {
            if (data[currentPosition + KEY_OFFSET] == NULL) {
                data[currentPosition + KEY_OFFSET] = key;
                data[currentPosition + VALUE_OFFSET] = value;
                addEntry(currentPosition);
                ++size;
                return NULL;
            }
            currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
        } while (currentPosition != position);
        return NULL;
    }

    /**
     * Returns the value associated with the given key, otherwise {@link this#NULL} is returned.
     */
    public int get(int key) {
        int position = hash(key);
        int currentPosition = position;
        do {
            if (data[currentPosition + KEY_OFFSET] == NULL) {
                return NULL;
            }
            if (key == data[currentPosition + KEY_OFFSET]) {
                removeEntry(currentPosition);
                addEntry(currentPosition);
                return data[currentPosition + VALUE_OFFSET];
            }
            currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
        } while (currentPosition != position);
        return NULL;
    }

    /**
     * Removes the given key from the cache. Returns the value associated with key if it is
     * removed, otherwise {@link this#NULL} is returned.
     */
    public int remove(int key) {
        int position = hash(key);
        int currentPosition = position;
        do {
            if (data[currentPosition + KEY_OFFSET] == NULL) {
                return NULL;
            }
            if (key == data[currentPosition + KEY_OFFSET]) {
                int removedValue = data[currentPosition + VALUE_OFFSET];
                removeEntry(currentPosition);
                shiftKeys(currentPosition);
                --size;
                return removedValue;
            }
            currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
        } while (currentPosition != position);
        return NULL;
    }

    /**
     * Returns the size of the cache.
     */
    public int size() {
        return size;
    }

    private void shiftKeys(int currentPosition) {
        int freeSlot;
        int currentKeySlot;
        do {
            freeSlot = currentPosition;
            currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
            while (true) {
                if (data[currentPosition + KEY_OFFSET] == NULL) {
                    data[freeSlot + KEY_OFFSET] = NULL;
                    return;
                }
                currentKeySlot = hash(data[currentPosition + KEY_OFFSET]);
                if (freeSlot <= currentPosition) {
                    if (freeSlot >= currentKeySlot || currentKeySlot > currentPosition) {
                        break;
                    }
                } else {
                    if (currentPosition < currentKeySlot && currentKeySlot <= freeSlot) {
                        break;
                    }
                }
                currentPosition = (currentPosition + NUM_INTEGERS_TO_HOLD_ENTRY) & positionModulo;
            }
            data[freeSlot + KEY_OFFSET] = data[currentPosition + KEY_OFFSET];
            data[freeSlot + VALUE_OFFSET] = data[currentPosition + VALUE_OFFSET];
            data[freeSlot + LEFT_OFFSET] = data[currentPosition + LEFT_OFFSET];
            data[freeSlot + RIGHT_OFFSET] = data[currentPosition + RIGHT_OFFSET];
            if (data[currentPosition + LEFT_OFFSET] >= 0) {
                data[data[currentPosition + LEFT_OFFSET] + RIGHT_OFFSET] = freeSlot;
                if (currentPosition == tailPosition) {
                    tailPosition = freeSlot;
                }
            }
            if (data[currentPosition + RIGHT_OFFSET] >= 0) {
                data[data[currentPosition + RIGHT_OFFSET] + LEFT_OFFSET] = freeSlot;
                if (currentPosition == headPosition) {
                    headPosition = freeSlot;
                }
            }
        } while (true);
    }

    private void removeEntry(int position) {
        if (data[position + LEFT_OFFSET] >= 0) {
            data[data[position + LEFT_OFFSET] + RIGHT_OFFSET] = data[position + RIGHT_OFFSET];
        } else {
            headPosition = data[position + RIGHT_OFFSET];
        }
        if (data[position + RIGHT_OFFSET] >= 0) {
            data[data[position + RIGHT_OFFSET] + LEFT_OFFSET] = data[position + LEFT_OFFSET];
        } else {
            tailPosition = data[position + LEFT_OFFSET];
        }
    }

    private void addEntry(int position) {
        if (tailPosition >= 0) {
            data[tailPosition + RIGHT_OFFSET] = position;
        }
        data[position + LEFT_OFFSET] = tailPosition;
        data[position + RIGHT_OFFSET] = NULL;
        tailPosition = position;
        if (headPosition < 0) {
            headPosition = tailPosition;
        }
    }

    private int hash(int key) {
        int h = key * 0x9E3779B9; // phiMix(x) taken from FastUtil
        return ((h ^ (h >> 16)) & modulo) * NUM_INTEGERS_TO_HOLD_ENTRY;
    }

    /**
     * Returns the least power of two larger than or equal to <code>Math.ceil( expected / f
     * )</code>.
     *
     * @param expectedSize
     *         the expected number of elements in a hash table.
     * @param f
     *         the load factor.
     * @return the minimum possible size for a backing array.
     * @throws IllegalArgumentException
     *         if the necessary size is larger than 2<sup>30</sup>.
     */
    private static int calculateArraySize(int expectedSize, float f) {
        long desiredCapacity = (long) Math.ceil(expectedSize / f);
        if (desiredCapacity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String
                    .format("Storage gets too large with expected size %s, load factor %s",
                            expectedSize, f));
        }
        // find next closest power of 2.
        if (desiredCapacity <= 2) {
            return 2;
        }
        desiredCapacity--;
        desiredCapacity |= desiredCapacity >> 1;
        desiredCapacity |= desiredCapacity >> 2;
        desiredCapacity |= desiredCapacity >> 4;
        desiredCapacity |= desiredCapacity >> 8;
        desiredCapacity |= desiredCapacity >> 16;
        return (int) ((desiredCapacity | desiredCapacity >> 32) + 1);
    }
}