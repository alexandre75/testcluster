package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.Duration;
import java.util.BitSet;

import com.microsoft.voiceapps.testcluster.service.HealthCheckException;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class HealthCheck {
	private static final int HISTORY_SIZE = 1000;
	private URI clusterHealthCheck;
	private HealthCheckService healthCheckService;
	private volatile boolean started = false;
	private volatile int size = 0;
	
	private BitSet history = new BitSet();
	private int current;
	
	private long lap;
	
	public HealthCheck(URI clusterHealthCheck, HealthCheckService healthCheckService) {
		super();
		this.clusterHealthCheck = requireNonNull(clusterHealthCheck);
		this.healthCheckService = requireNonNull(healthCheckService);
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
		started = true;
		
		Thread thread = new Thread(this::runChecks);
		thread.start();
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
	}

	public void stop() {
		started = false;
	}
	
	private void runChecks() {
		try {
			while (started) {
				long start = System.nanoTime();
				try {
					healthCheckService.testHealth(clusterHealthCheck);
					setHealth(true);
				} catch (HealthCheckException e) {
					setHealth(false);
				}
				Thread.sleep(100);
				
				long duration = System.nanoTime() - start;
				if (lap == 0) {
					lap = duration;
				} else {
					lap = 2 * duration / size + (lap * (size - 1))/ size;
				}
			}
		} catch(InterruptedException e) {
			// ignore
		}
	}
}
