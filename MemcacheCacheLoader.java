package cache;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.CacheLoader;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClientIF;

/**
 * Uses Memcache as the far cache, and Caffeine as the near cache.
 *  
 * @param <T> The source type of the cache entry
 */
@Slf4j
public abstract class MemcacheCacheLoader<K, V> implements CacheLoader<K, TemporalCacheEntryWrapper<V>> {
	private final MemcachedClientIF memcachedClient;
	
	private final ExecutorService executorService;
	
    public MemcacheCacheLoader(final MemcachedClientIF memcachedClient, final String name) {
		this.memcachedClient = memcachedClient;
		
		this.executorService = Executors.newFixedThreadPool(20, new DaemonThreadFactory(name));
	}

	@Override
    public TemporalCacheEntryWrapper<V> load(final K key) throws Exception {
    	final String memcacheKey = getCacheKey(key);
    	
        final Optional<TemporalCacheEntryWrapper<V>> cachedEntryOpt = getCacheEntryFromMemcache(memcacheKey);
        
        try {
        	final boolean isAvailableForRefresh = this.isAvailableForRefresh(cachedEntryOpt);
        	final boolean isExpired = this.isExpired(cachedEntryOpt);
        	
            // If we don't have a cached copy, or the cached copy is ready to 
        	// be asynchronously refreshed, then fetch from source, and update 
        	// cache.
        	if (cachedEntryOpt.isPresent() && !isExpired) {
        		// Kick off in another thread
        		if (isAvailableForRefresh) {
        			this.executorService.execute(new Runnable() {
						@Override
						public void run() {
							fetchNewCacheEntryAndSave(key);
						}
					});
	        		
        		}
        		
        		return cachedEntryOpt.get();
        	} else {
        		return this.fetchNewCacheEntryAndSave(key);
        	}
        } catch(final Exception ex) {
            log.error("Error loading cache entry for key " + key, ex);
        }
        
        return null;
    }
    
	/**
	 * Fetches from the source, creates new <code>TemporalCacheEntryWrapper</code>
	 * with updated refresh and expiration times, and saves to memecahce.
	 * 
	 * @param key
	 *            Cache key string
	 * 
	 * @return The newly updated/create cache entry.
	 */
	private TemporalCacheEntryWrapper<V> fetchNewCacheEntryAndSave(final K key) {
		final Optional<V> sourceOpt = this.fetchFromSource(key);
		final String memcacheKey = getCacheKey(key);
        
        if (sourceOpt.isPresent()) {
            final TemporalCacheEntryWrapper<V> cacheEntry = this.wrapSourceObjectInCacheEntryWrapper(sourceOpt);
            
            this.saveCacheEntryToMemcache(memcacheKey, cacheEntry);
            
            return cacheEntry;
        }
        
        return null;
	}
	
	/**
	 * Creates new cache entry with updated refresh and expiration times.
	 * 
	 * @param sourceOpt
	 *            Source data
	 * 
	 * @return The newly updated/create cache entry.
	 */
	private TemporalCacheEntryWrapper<V> wrapSourceObjectInCacheEntryWrapper(final Optional<V> sourceOpt) {
		final V source = sourceOpt.get();
		
		final int cacheExpirationTimeInSeconds = this.getCacheExpirationInSeconds(sourceOpt);
        final int cacheRefreshTimeInSeconds = this.getCacheRefreshEligibleInSeconds(sourceOpt);
        
        // Create new expiration milliseconds into the future from epoch 
        final long expirationTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cacheExpirationTimeInSeconds);
        final long refreshTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cacheRefreshTimeInSeconds);
        
        final TemporalCacheEntryWrapper<V> cacheEntry =
        		new TemporalCacheEntryWrapper<>(source, expirationTime, refreshTime);
        
        return cacheEntry;
	}
	
    private void saveCacheEntryToMemcache(final String key, final TemporalCacheEntryWrapper<V> cacheEntry) {
        try {
            // Store in memcache, setting the entry to never expire since we 
        	// store and check the expiration time, and rely on memcache to 
        	// eventually purge.
            this.memcachedClient.set(key, 0, cacheEntry);
        } catch(final Exception ex) {
            log.warn("Error attempting to save to memcache", ex);
        }
    }
    
    private Optional<TemporalCacheEntryWrapper<V>> getCacheEntryFromMemcache(final String key) {
        try {
            final Future<Object> getFuture = this.memcachedClient.asyncGet(key);
            
            // Timeout after 50 milliseconds
            final TemporalCacheEntryWrapper<V> entry = (TemporalCacheEntryWrapper<V>) getFuture.get(50, TimeUnit.MILLISECONDS);
            
            System.out.println("!!!" + entry.getValue().get());
            
            return Optional.ofNullable(entry);
        } catch(final Exception ex) {
            log.warn("Error attempting to fetch from memcache", ex);
        }
        
        return Optional.empty();
    }
    
    private boolean isAvailableForRefresh(final Optional<TemporalCacheEntryWrapper<V>> cachedEntryOpt) {
        return cachedEntryOpt.isPresent() && 
                cachedEntryOpt.get().isAvailableForRefresh();
    }
    
    private boolean isExpired(final Optional<TemporalCacheEntryWrapper<V>> cachedEntryOpt) {
        return cachedEntryOpt.isPresent() &&  
                cachedEntryOpt.get().isExpired();
    }

    public abstract Optional<V> fetchFromSource(K key);
    
    public abstract int getCacheExpirationInSeconds(Optional<V> source);
    
    public abstract int getCacheRefreshEligibleInSeconds(Optional<V> source);
    
    public String getCacheKey(K key) {
    	return key.toString();
    }
}
