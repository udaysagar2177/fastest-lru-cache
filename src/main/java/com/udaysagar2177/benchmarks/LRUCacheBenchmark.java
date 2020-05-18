package com.udaysagar2177.benchmarks;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.udaysagar2177.cache.IntEntryLRUCache;
import com.udaysagar2177.cache.IntEntryViewLRUCache;
import com.udaysagar2177.cache.IntIntLRUCache;
import com.udaysagar2177.cache.IntIntLinkedHashMapAsCache;

/**
 * Benchmarks of different cache implementations.
 *
 * The keys are selected at random from the set [0 cacheSize * 2) uniformly for put and get
 * operation benchmarks.
 *
 * @author uday
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(2)
@Warmup(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
public class LRUCacheBenchmark {
    
    private static final Random RANDOM = new Random();
    
    @Param({"10000", "100000", "1000000", "10000000", "25000000"})
    private int cacheSize = 100_000;

    private int populationSize = 0;
    private IntIntLinkedHashMapAsCache intIntLinkedHashMapAsCache;
    private IntEntryLRUCache intEntryLRUCache;
    private IntIntLRUCache intIntLRUCache;
    private IntEntryViewLRUCache intEntryViewLRUCache;

    @Setup
    public void setup() {
        populationSize = cacheSize * 2;
        intIntLinkedHashMapAsCache = new IntIntLinkedHashMapAsCache(cacheSize, 0.66f);
        intEntryLRUCache = new IntEntryLRUCache(cacheSize, 0.66f);
        intIntLRUCache = new IntIntLRUCache(cacheSize, 0.66f);
        intEntryViewLRUCache = new IntEntryViewLRUCache(cacheSize, 0.66f);
        for (int i = 0; i < cacheSize; i++) {
            int key = randInt(populationSize);
            int value = randInt(populationSize);
            intIntLinkedHashMapAsCache.put(key, value);
            intEntryLRUCache.put(key, value);
            intIntLRUCache.put(key, value);
            intEntryViewLRUCache.put(key, value);
        }
    }

    public static void main(String[] args) throws Exception {
        LRUCacheBenchmark lruCacheBenchmark = new LRUCacheBenchmark();
        lruCacheBenchmark.setup();
        lruCacheBenchmark.testPutOnMapIntBoolLRUCache();
    }

    @Benchmark
    public int testPutOnMapIntBoolLRUCache() {
        intIntLinkedHashMapAsCache.put(randInt(populationSize), randInt(populationSize));
        return intIntLinkedHashMapAsCache.size();
    }

    @Benchmark
    public int testGetOnMapIntBoolLRUCache() {
        return intIntLinkedHashMapAsCache.get(randInt(populationSize));
    }

    @Benchmark
    public int testPutOnIntEntryLRUCache() {
        intEntryLRUCache.put(randInt(populationSize), randInt(populationSize));
        return intEntryLRUCache.size();
    }

    @Benchmark
    public int testGetOnIntEntryLRUCache() {
        return intEntryLRUCache.get(randInt(populationSize));
    }

    @Benchmark
    public int testPutOnIntEntryViewLRUCache() {
        intEntryViewLRUCache.put(randInt(populationSize), randInt(populationSize));
        return intEntryViewLRUCache.size();
    }

    @Benchmark
    public int testGetOnIntEntryViewLRUCache() {
        return intEntryViewLRUCache.get(randInt(populationSize));
    }

    @Benchmark
    public int testPutOnIntIntLRUCache() {
        intIntLRUCache.put(randInt(populationSize), randInt(populationSize));
        return intIntLRUCache.size();
    }

    @Benchmark
    public int testGetOnIntIntLRUCache() {
        return intIntLRUCache.get(randInt(populationSize));
    }

    private static int randInt(int max) {
        return Math.abs(RANDOM.nextInt(max));
    }
}
