package cache;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
	private final String name;

	private AtomicInteger count = new AtomicInteger();

	public DaemonThreadFactory(String name) {
		this.name = name;
	}

	@Override
	public Thread newThread(final Runnable runnable) {
		final Thread thread = new Thread(runnable);

		thread.setName(String.format("%s-%s", name, count.incrementAndGet()));
		thread.setDaemon(true);

		return thread;
	}

}
