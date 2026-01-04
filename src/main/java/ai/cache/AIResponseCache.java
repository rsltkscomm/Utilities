package ai.cache;

import reporting.TestLogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * AI Response Cache
 * 
 * Caches AI responses to reduce API calls and improve performance
 * Supports TTL (Time To Live) and cache invalidation
 */
public class AIResponseCache {
    
    private static volatile AIResponseCache instance;
    private final Map<String, CacheEntry> cache;
    private final ReentrantReadWriteLock lock;
    private final long defaultTTL; // milliseconds
    private final int maxCacheSize;
    
    private AIResponseCache() {
        this.cache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.defaultTTL = 3600000; // 1 hour default
        this.maxCacheSize = 10000; // Max 10k entries
    }
    
    public static AIResponseCache getInstance() {
        if (instance == null) {
            synchronized (AIResponseCache.class) {
                if (instance == null) {
                    instance = new AIResponseCache();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get cached response
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            
            // Check if expired
            if (entry.isExpired()) {
                cache.remove(key);
                return Optional.empty();
            }
            
            // Update access time
            entry.updateAccessTime();
            
            // Return cached value
            Object value = entry.getValue();
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            
            return Optional.empty();
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Put value in cache
     */
    public <T> void put(String key, T value) {
        put(key, value, defaultTTL);
    }
    
    /**
     * Put value in cache with custom TTL
     */
    public <T> void put(String key, T value, long ttlMillis) {
        lock.writeLock().lock();
        try {
            // Check cache size limit
            if (cache.size() >= maxCacheSize && !cache.containsKey(key)) {
                evictOldest();
            }
            
            CacheEntry entry = new CacheEntry(value, ttlMillis);
            cache.put(key, entry);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Check if key exists and is valid
     */
    public boolean containsKey(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry == null) return false;
            if (entry.isExpired()) {
                cache.remove(key);
                return false;
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Remove from cache
     */
    public void remove(String key) {
        lock.writeLock().lock();
        try {
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            TestLogManager.info("AI Response Cache cleared");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clear expired entries
     */
    public void clearExpired() {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        lock.readLock().lock();
        try {
            int total = cache.size();
            int expired = 0;
            long totalSize = 0;
            
            for (CacheEntry entry : cache.values()) {
                if (entry.isExpired()) expired++;
                totalSize += estimateSize(entry.getValue());
            }
            
            return new CacheStatistics(total, expired, total - expired, totalSize);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Generate cache key from inputs
     */
    public static String generateKey(String operation, Map<String, Object> inputs) {
        StringBuilder keyBuilder = new StringBuilder(operation);
        inputs.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                keyBuilder.append("|").append(entry.getKey()).append("=").append(entry.getValue());
            });
        return keyBuilder.toString();
    }
    
    /**
     * Evict oldest entry
     */
    private void evictOldest() {
        Optional<Map.Entry<String, CacheEntry>> oldest = cache.entrySet().stream()
            .min(Comparator.comparing(entry -> entry.getValue().getLastAccessTime()));
        
        oldest.ifPresent(entry -> {
            cache.remove(entry.getKey());
            TestLogManager.info("Evicted cache entry: " + entry.getKey());
        });
    }
    
    /**
     * Estimate size of cached value
     */
    private long estimateSize(Object value) {
        if (value == null) return 0;
        if (value instanceof String) return ((String) value).length() * 2; // 2 bytes per char
        if (value instanceof Collection) return ((Collection<?>) value).size() * 100; // Rough estimate
        return 100; // Default estimate
    }
    
    /**
     * Cache entry
     */
    private static class CacheEntry {
        private final Object value;
        private final long createdAt;
        private final long ttl;
        private long lastAccessTime;
        
        public CacheEntry(Object value, long ttl) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.ttl = ttl;
            this.lastAccessTime = this.createdAt;
        }
        
        public Object getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttl;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        private final int totalEntries;
        private final int expiredEntries;
        private final int validEntries;
        private final long estimatedSize;
        
        public CacheStatistics(int totalEntries, int expiredEntries, int validEntries, long estimatedSize) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
            this.validEntries = validEntries;
            this.estimatedSize = estimatedSize;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getExpiredEntries() { return expiredEntries; }
        public int getValidEntries() { return validEntries; }
        public long getEstimatedSize() { return estimatedSize; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, valid=%d, expired=%d, size=%d bytes}",
                totalEntries, validEntries, expiredEntries, estimatedSize);
        }
    }
}

