package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;

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
	private int size = 0;
	
	private BitSet history = new BitSet();
	private int current;
	
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
		return new Health(clusterHealthCheck.getHost(), size, history.cardinality());
	}

	@Value
	public static class Health {
		private String cluster;
		private int nbRequests;
		private int nbFailedRequests;
		
		public Health(String cluster, int nbRequests, int nbFailedRequests) {
			super();
			this.cluster = cluster;
			this.nbRequests = nbRequests;
			this.nbFailedRequests = nbFailedRequests;
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
		
		
	}

	public void stop() {
		started = false;
	}
	
	private void runChecks() {
		try {
			while (started) {
				try {
					healthCheckService.testHealth(clusterHealthCheck);
					setHealth(true);
				} catch (HealthCheckException e) {
					setHealth(false);
				}
				Thread.sleep(100);
			}
		} catch(InterruptedException e) {
			// ignore
		}
	}
}
