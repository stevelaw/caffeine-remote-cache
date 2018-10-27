package cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

import javax.annotation.Nonnegative;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bridge between Spring Boot Actuator and Caffeine cache statistics.
 */
public class CaffeineStatsCounter implements StatsCounter {
  private final CounterService counter;
  private final GaugeService gauge;
  private final String prefix;

  private final LongAdder hitCount;
  private final LongAdder missCount;
  private final LongAdder loadSuccessCount;
  private final LongAdder loadFailureCount;
  private final LongAdder totalLoadTime;
  private final LongAdder evictionCount;
  private final LongAdder evictionWeight;

  public CaffeineStatsCounter(final CounterService counter, final GaugeService gauge, final String prefix) {
    this.counter = counter;
    this.gauge = gauge;
    this.prefix = prefix;

    hitCount = new LongAdder();
    missCount = new LongAdder();
    loadSuccessCount = new LongAdder();
    loadFailureCount = new LongAdder();
    totalLoadTime = new LongAdder();
    evictionCount = new LongAdder();
    evictionWeight = new LongAdder();
  }

  @Override
  public void recordHits(@Nonnegative int count) {
    hitCount.add(count);
    this.counter.increment(this.prefix + ".hits");
    recordRatios();
  }

  @Override
  public void recordMisses(@Nonnegative int count) {
    missCount.add(count);
    this.counter.increment(this.prefix + ".misses");
    recordRatios();
  }

  @Override
  public void recordLoadSuccess(@Nonnegative long loadTime) {
    loadSuccessCount.increment();
    totalLoadTime.add(loadTime);

    this.counter.increment(this.prefix + ".loads-success");
    this.gauge.submit(this.prefix + ".loads-success-time-milli", TimeUnit.MILLISECONDS.convert(loadTime, TimeUnit.NANOSECONDS));
  }

  @Override
  public void recordLoadFailure(@Nonnegative long loadTime) {
    loadFailureCount.increment();
    totalLoadTime.add(loadTime);

    this.counter.increment(this.prefix + ".loads-failure");
    this.gauge.submit(this.prefix + ".loads-failure-time-milli", TimeUnit.MILLISECONDS.convert(loadTime, TimeUnit.NANOSECONDS));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void recordEviction() {
    evictionCount.increment();

    this.counter.increment(this.prefix + ".eviction");
  }

  @Override
  public void recordEviction(int weight) {
    evictionCount.increment();
    evictionWeight.add(weight);

    this.counter.increment(this.prefix + ".eviction");
  }

  @Override
  public CacheStats snapshot() {
    return new CacheStats(
        hitCount.sum(),
        missCount.sum(),
        loadSuccessCount.sum(),
        loadFailureCount.sum(),
        totalLoadTime.sum(),
        evictionCount.sum(),
        evictionWeight.sum());
  }

  @Override
  public String toString() {
    return snapshot().toString();
  }

  private void recordRatios() {
    CacheStats cacheStats = snapshot();
    this.gauge.submit(this.prefix + ".hit.ratio", cacheStats.hitRate());
    this.gauge.submit(this.prefix + ".miss.ratio", cacheStats.missRate());
  }
}
