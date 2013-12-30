package com.newrelic.plugins.terracotta.utils;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsBufferingWorker {
	private static Logger log = LoggerFactory.getLogger(MetricsBufferingWorker.class);

	private final MetricsBuffer metricsBuffer = new MetricsBuffer();
	private final MetricsFetcher metricsFetcher;

	private final long intervalInMilliSeconds;

	private static final long REFRESHINTERVALDEFAULT = 5000L;
	private static final TimeUnit refreshIntervalUnit = TimeUnit.MILLISECONDS;

	private final ScheduledExecutorService cacheTimerService;
	private ScheduledFuture<?> cacheTimerServiceFuture;

	public MetricsBufferingWorker(MetricsFetcher metrics) {
		this(0L, metrics);
	}

	public MetricsBufferingWorker(long intervalInMilliSeconds, MetricsFetcher metricsFetcher) {
		super();

		if(intervalInMilliSeconds <= 0)
			intervalInMilliSeconds = REFRESHINTERVALDEFAULT;
		this.intervalInMilliSeconds = intervalInMilliSeconds;

		this.metricsFetcher = metricsFetcher;

		//setup the timer thread pool for every 5 seconds
		cacheTimerService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Sync Timer Cache Pool");
			}
		});
	}

	public List<Metric> getMetricsSnapshot() {
		return metricsBuffer.getAllMetricsAndReset();
	}

	public MetricsFetcher getMetricsFetcher() {
		return metricsFetcher;
	}

	public void startAndMoveOn(){
		//schedule the timer pool to execute a cache search every 5 seconds...which in turn will execute the cache sync operations
		cacheTimerServiceFuture = cacheTimerService.scheduleAtFixedRate(new MetricFetcherOp(), 0L, intervalInMilliSeconds, refreshIntervalUnit);
	}

	public void stop(){
		// clean up and exit
		if(null != cacheTimerServiceFuture)
			cacheTimerServiceFuture.cancel(true);

		shutdownNow(cacheTimerService, 5);
	}

	public void waitForever(){
		if(null != cacheTimerServiceFuture)
			throw new IllegalArgumentException("The Scheduler was not started...make sure to start before calling this method");

		try {
			// getting the future's response will block forever unless an exception is thrown
			cacheTimerServiceFuture.get();	
		} catch (InterruptedException e) {
			System.err.println("SEVERE: An error has occurred");
			e.printStackTrace();
		} catch (CancellationException e) {
			System.err.println("SEVERE: An error has occurred");
			e.printStackTrace();
		} catch (ExecutionException e) {
			// ExecutionException will wrap any java.lang.Error from the polling thread that we should not catch there (e.g. OutOfMemoryError)
			System.err.println("SEVERE: An error has occurred");
			e.printStackTrace();
		} finally {
			// clean up and exit
			cacheTimerServiceFuture.cancel(true);
			shutdownNow(cacheTimerService);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		stop();
	}

	/*
	 * thread executor shutdown
	 */
	private void shutdownNow(ExecutorService pool) {
		shutdownNow(pool, Integer.MAX_VALUE);
	}

	private void shutdownNow(ExecutorService pool, int timeoutInSeconds) {
		pool.shutdown();

		try {
			while(!cacheTimerService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS));

			pool.shutdownNow(); // Cancel currently executing tasks

			// Wait a while for tasks to respond to being canceled
			if (!pool.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS))
				log.error("Pool did not terminate");
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();

			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/*
	 * Searches elements in delegated cache, and call refreshOp for every returned results
	 */
	private class MetricFetcherOp implements Runnable {
		public MetricFetcherOp() {
		}

		public void run() {
			try{
				metricsBuffer.bulkAddMetrics(metricsFetcher.getMetricsFromServer());
			} catch (Exception exc){
				log.error("Unexpected error...", exc);
			}
		}
	}
}
