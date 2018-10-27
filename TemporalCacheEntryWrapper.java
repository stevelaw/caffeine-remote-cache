package cache;

import java.io.Serializable;
import java.util.Optional;

import lombok.ToString;

@ToString
public class TemporalCacheEntryWrapper<V> implements Serializable {
    private static final long serialVersionUID = 1L;

    final V value;
    final long expiration;
    final long nextRefreshEpoch;

    public TemporalCacheEntryWrapper(V value, long expiration, long nextRefreshEpoch) {
		this.value = value;
		this.expiration = expiration;
		this.nextRefreshEpoch = nextRefreshEpoch;
	}
    
    public Optional<V> getValue() {
		return Optional.ofNullable(value);
	}

	public long getExpiration() {
		return expiration;
	}
    
	/**
	 * Checks if the cache entry is available for refresh, based on the last stored
	 * refresh epoch time compared with wall clock epoch.
	 * 
	 * @return True if the entry if available for refresh, false otherwise.
	 */
    public boolean isAvailableForRefresh() {
        final long epoch = System.currentTimeMillis();
        
        return this.nextRefreshEpoch < epoch;
    }
    
	/**
	 * Checks if the cache entry is expired, based on the last stored expiration
	 * epoch time compared with wall clock epoch.
	 * 
	 * @return True if the entry is expired, false otherwise.
	 */
    public boolean isExpired() {
    	final long epoch = System.currentTimeMillis();
    	
    	return this.expiration < epoch;
    }
}
