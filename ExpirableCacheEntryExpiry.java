package cache;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Expiry;

/**
 * Updates the cache expiration time to the remaining time available on the 
 * ExpirableCacheEntry when an entry is created or updated. 
 */
public class ExpirableCacheEntryExpiry<K,T> implements Expiry<K, TemporalCacheEntryWrapper<T>> {
    
    @Override
    public long expireAfterCreate(
            final K key, 
            final TemporalCacheEntryWrapper<T> value, 
            final long currentTime) {
        final long epochExpiration = value.getExpiration();
        final long epoch = System.currentTimeMillis();
        
        return Math.max(TimeUnit.MILLISECONDS.toNanos(epochExpiration - epoch), 0); 
    }

    @Override
    public long expireAfterUpdate(
            final K key, 
            final TemporalCacheEntryWrapper<T> value, 
            final long currentTime,
            final long currentDuration) {
        final long epochExpiration = value.getExpiration();
        final long epoch = System.currentTimeMillis();
        
        return Math.max(TimeUnit.MILLISECONDS.toNanos(epochExpiration - epoch), 0);
    }

    @Override
    public long expireAfterRead(
            final K key, 
            final TemporalCacheEntryWrapper<T> value, 
            final long currentTime,
            final long currentDuration) {
        return currentDuration;
    }

}
