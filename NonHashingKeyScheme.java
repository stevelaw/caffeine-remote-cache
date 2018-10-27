package cache;

import org.apache.http.impl.client.cache.memcached.KeyHashingScheme;

/**
 * Useful for HTTP Memcache caching configurations to more easily view which
 * keys are being cached.
 */
public class NonHashingKeyScheme implements KeyHashingScheme {

	@Override
	public String hash(String storageKey) {
		return storageKey;
	}

}
