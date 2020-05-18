package com.udaysagar2177.cache;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class CacheRandomInputTest {

    private static final Random RANDOM = new Random();

    private static int randInt(int max) {
        return Math.abs(RANDOM.nextInt(max));
    }

    @Test
    public void testCaches() {
        int[] cacheSizes = new int[] { 3, 1000, 10000, 100000};
        for (int i = 0; i < cacheSizes.length; i++) {
            int cacheSize = cacheSizes[i];
            System.out.println("Testing with cache size: " + cacheSize);
            IntIntLinkedHashMapAsCache intIntLinkedHashMapAsCache =
                    new IntIntLinkedHashMapAsCache(cacheSize, 0.66f);
            IntEntryLRUCache intEntryLRUCache = new IntEntryLRUCache(cacheSize, 0.66f);
            IntEntryViewLRUCache intEntryViewLRUCache = new IntEntryViewLRUCache(cacheSize, 0.66f);
            IntIntLRUCache intIntLRUCache = new IntIntLRUCache(cacheSize, 0.66f);

            testCaches(intIntLinkedHashMapAsCache, intEntryLRUCache, intEntryViewLRUCache,
                    intIntLRUCache, cacheSize);

            // clear caches
            intIntLinkedHashMapAsCache.clear();
            intEntryLRUCache.clear();
            intEntryViewLRUCache.clear();
            intIntLRUCache.clear();

            // test again
            testCaches(intIntLinkedHashMapAsCache, intEntryLRUCache, intEntryViewLRUCache,
                    intIntLRUCache, cacheSize);
        }
    }

    private void testCaches(IntIntLinkedHashMapAsCache intIntLinkedHashMapAsCache,
                            IntEntryLRUCache intEntryLRUCache,
                            IntEntryViewLRUCache intEntryViewLRUCache,
                            IntIntLRUCache intIntLRUCache, int cacheSize) {

        int loopIterations = cacheSize * 3;
        int populationSize = cacheSize * 2;
        int[] updateSequence = new int[loopIterations * 2];
        Arrays.fill(updateSequence, -1);

        // test puts
        for (int i = 0, j = 0; i < loopIterations; i++, j += 2) {
            int randomKey = randInt(populationSize);
            int randomValue = randInt(populationSize);
            updateSequence[j] = randomKey;
            updateSequence[j + 1] = randomValue;

            intEntryLRUCache.put(randomKey, randomValue);
            intEntryViewLRUCache.put(randomKey, randomValue);
            intIntLRUCache.put(randomKey, randomValue);
            intIntLinkedHashMapAsCache.put(randomKey, randomValue);
        }
        compareGetsOnCaches(intIntLinkedHashMapAsCache, intEntryLRUCache, intEntryViewLRUCache,
                intIntLRUCache, updateSequence);

        // test removes;
        // caches will have only last few elements from updateSequence
        // but attempt to remove elements over the entire range.
        int[] removedKeySequence = new int[loopIterations];
        Arrays.fill(removedKeySequence, -1);
        for (int i = 0; i < loopIterations; i++) {
            int key = updateSequence[randInt(updateSequence.length)];
            removedKeySequence[i] = key;
            int removedValue = intIntLinkedHashMapAsCache.remove(key);
            try {
                int removedIntEntryCacheValue = intEntryLRUCache.remove(key);
                assertEquals(String.format("Removed value from IntEntryCache doesn't match"
                                + " expected value. Key: %s, Expected: %s, Actual: %s", key,
                        removedValue, removedIntEntryCacheValue),
                        removedValue, removedIntEntryCacheValue);

                int removedIntEntryViewCacheValue = intEntryViewLRUCache.remove(key);
                assertEquals(String.format("Removed value from IntEntryViewCache doesn't match"
                                + " expected value. Key: %s, Expected: %s, Actual: %s", key,
                        removedValue, removedIntEntryViewCacheValue),
                        removedValue, removedIntEntryViewCacheValue);

                int removedIntIntCacheValue = intIntLRUCache.remove(key);
                assertEquals(String.format("Removed value from IntIntCache doesn't match"
                                + " expected value. Key: %s, Expected: %s, Actual: %s", key,
                        removedValue, removedIntIntCacheValue),
                        removedValue, removedIntIntCacheValue);
            } catch (Error | Exception e) {
                System.out.println(String.format("Cache removals sequence: %s",
                        Arrays.toString(removedKeySequence)));
                throw e;
            }
        }

        // test caches once again
        try {
            compareGetsOnCaches(intIntLinkedHashMapAsCache, intEntryLRUCache, intEntryViewLRUCache,
                    intIntLRUCache, updateSequence);
        } catch (Error | Exception e) {
            System.out.println("Exception comparing caches after removals");
            System.out.println(String.format("Cache removals sequence: %s",
                    Arrays.toString(removedKeySequence)));
            throw e;
        }
    }

    private void compareGetsOnCaches(IntIntLinkedHashMapAsCache intIntLinkedHashMapAsCache,
                                     IntEntryLRUCache intEntryLRUCache,
                                     IntEntryViewLRUCache intEntryViewLRUCache,
                                     IntIntLRUCache intIntLRUCache, int[] cacheUpdateSequence) {
        try {
            assertEquals(String.format("Cache sizes don't match. Expected: %s, IntEntryCache: %s",
                    intIntLinkedHashMapAsCache.size(), intEntryLRUCache.size()),
                    intIntLinkedHashMapAsCache.size(), intEntryLRUCache.size());
            assertEquals(String.format("Cache sizes don't match. Expected: %s, IntEntryViewCache: %s",
                    intIntLinkedHashMapAsCache.size(), intEntryViewLRUCache.size()),
                    intIntLinkedHashMapAsCache.size(), intEntryViewLRUCache.size());
            assertEquals(String.format("Cache sizes don't match. Expected: %s, IntIntCache: %s",
                    intIntLinkedHashMapAsCache.size(), intIntLRUCache.size()),
                    intIntLinkedHashMapAsCache.size(), intIntLRUCache.size());

            for (Map.Entry<Integer, Integer> entry : intIntLinkedHashMapAsCache.getMap().entrySet()) {

                int intEntryCacheValue = intEntryLRUCache.get(entry.getKey());
                assertEquals(String.format("IntEntryCache value %s doesn't match expected value %s,"
                                + " key: %s", intEntryCacheValue, entry.getValue(), entry.getKey()),
                        (int) entry.getValue(), intEntryCacheValue);

                int intEntryViewCacheValue = intEntryViewLRUCache.get(entry.getKey());
                assertEquals(
                        String.format("IntEntryViewCache value %s doesn't match expected value %s,"
                                        + " key: %s", intEntryViewCacheValue, entry.getValue(),
                                entry.getKey()),
                        (int) entry.getValue(), intEntryViewCacheValue);

                int intIntCacheValue = intIntLRUCache.get(entry.getKey());
                assertEquals(String.format("IntIntCache value %s doesn't match expected value %s,"
                                + " key: %s", intIntCacheValue, entry.getValue(), entry.getKey()),
                        (int) entry.getValue(), intIntCacheValue);
            }
        } catch (Error | Exception e) {
            System.out.println(String.format("Cache additions sequence: %s",
                    Arrays.toString(cacheUpdateSequence)));
            throw e;
        }

    }

}