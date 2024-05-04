package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.net.URI;
import java.time.Duration;
import java.util.BitSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.microsoft.voiceapps.testcluster.service.HealthCheckException;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class HealthCheck implements Closeable {
	private static final int CHECK_DELAY = 100;

	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
	
	private static final int HISTORY_SIZE = 1000;
	private URI clusterHealthCheck;
	private HealthCheckService healthCheckService;
	private volatile int size = 0;
	
	private BitSet history = new BitSet();
	private int current;
	
	private long lap;
	
	private ScheduledFuture<?> handle;
	
	private static final Cleaner cleaner = Cleaner.create();

    class State implements Runnable {
         public void run() {
        	 if (handle != null) {
        		 handle.cancel(false);
        	 }
         }
    }
    
    private final State state;
    private final Cleaner.Cleanable cleanable;
	
	public HealthCheck(URI clusterHealthCheck, HealthCheckService healthCheckService) {
		super();
		this.clusterHealthCheck = requireNonNull(clusterHealthCheck);
		this.healthCheckService = requireNonNull(healthCheckService);
		
        this.state = new State();
        this.cleanable = cleaner.register(this, state);
	}
	
	synchronized void setHealth(boolean success) {
		if (current == HISTORY_SIZE) {
			current = 0;
		}
		history.set(current++, !success);
		
		if (size < HISTORY_SIZE) {
			size++;
		}
	}

	public void start() {	
		handle = executor.scheduleWithFixedDelay(this::runCheck, 0, CHECK_DELAY, TimeUnit.MILLISECONDS);
	}
	
	public synchronized Health health() {
		return new Health(clusterHealthCheck.getHost(), size, history.cardinality(), Duration.ofNanos(size * lap));
	}

	@Value
	public static class Health {
		private String cluster;
		private int nbRequests;
		private int nbFailedRequests;
		private Duration window;
		
		public Health(String cluster, int nbRequests, int nbFailedRequests, Duration window) {
			super();
			this.cluster = cluster;
			this.nbRequests = nbRequests;
			this.nbFailedRequests = nbFailedRequests;
			this.window = window;
		}

		public String getCluster() {
			return cluster;
		}

		public int getNbRequests() {
			return nbRequests;
		}

		public int getNbFailedRequests() {
			return nbFailedRequests;
		}

		public Duration getWindow() {
			return window;
		}

		public double getErrorRate() {
			if (getNbRequests() == 0) {
				return 0D;
			}
			
			return getNbFailedRequests() / (double)getNbRequests();
		}
	}

	private void runCheck() {
		long start = System.nanoTime();
		try {
			healthCheckService.testHealth(clusterHealthCheck);
			setHealth(true);
		} catch (HealthCheckException e) {
			setHealth(false);
		}

		long duration = System.nanoTime() - start + CHECK_DELAY * 1_000_000;
		if (lap == 0) {
			lap = duration;
		} else {
			lap = 2 * duration / size + (lap * (size - 1))/ size;
		}
	}

	@Override
	public void close() {
		cleanable.clean();
	}
}
