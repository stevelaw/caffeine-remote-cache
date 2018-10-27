```
Caffeine
       .newBuilder()
       .maximumSize(500)
       .refreshAfterWrite(CACHE_REFRESH_TIME_IN_SECONDS, TimeUnit.SECONDS)
       .recordStats(() -> new CaffeineStatsCounter(counter, gauge, "header"))
       .expireAfter(new ExpirableCacheEntryExpiry<ExperimentSetCacheKey, ExperimentSetResponse>())
       .build(new MemcacheCacheLoader<ExperimentSetCacheKey, ExperimentSetResponse>(memcachedClient, "search") {
        			@Override
        			public Optional<ExperimentSetResponse> fetchFromSource(ExperimentSetCacheKey key) {
        				return getExperimentSetByContentIdFromSource(
        						key.getId(), 
        						key.getToken(), 
        						key.getNumberOfItems());
        			}

        			@Override
        			public String getCacheKey(final ExperimentSetCacheKey key) {
        				return String.format("%s-key", key.getSwid());
        			}

        			@Override
        			public int getCacheExpirationInSeconds(Optional<ExperimentSetResponse> source) {
        				return CACHE_EXPIRATION_TIME_IN_SECONDS;
        			}

        			@Override
        			public int getCacheRefreshEligibleInSeconds(Optional<ExperimentSetResponse> source) {
        				return CACHE_REFRESH_TIME_IN_SECONDS;
        			}
        		});```
