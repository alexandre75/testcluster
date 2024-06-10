package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microsoft.voiceapps.testcluster.service.TcpConnectService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class HealthCheck implements Closeable {
	private static final int CHECK_DELAY = 100;
	private static final Logger logger = LoggerFactory.getLogger(HealthCheck.class);

	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(25);
	private final int historySize;
	private static final int HISTORY_TCP_SIZE = 1000;
	private URI clusterHealthCheck;
	private final InetSocketAddress inetAddr;
	
	private final Timer tcpTimerSuccess;
	private final Timer tcpTimerFail;
	private final Counter errors;
	private final Counter success;
	
	private HttpHealth healthCheckService;
	
	private volatile int size = 0;
	private BitSet history = new BitSet();
	private int current;
	
	private volatile int sizeTcp = 0;
	private BitSet historyTcp = new BitSet();
	private int currentTcp;
	
	private long lap;
	
	private ScheduledFuture<?> handle;
	private ScheduledFuture<?> handleTcp;
	
	private static final Cleaner cleaner = Cleaner.create();

    class State implements Runnable {
         public void run() {
        	 if (handle != null) {
        		 handle.cancel(false);
        		 handleTcp.cancel(false);
        	 }
         }
    }
    
    private final State state;
    private final Cleaner.Cleanable cleanable;
	
    public HealthCheck(URI clusterHealthCheck, HttpHealth healthCheckService, int historySize) {
    	this(clusterHealthCheck, healthCheckService, historySize, Metrics.globalRegistry);
    }
    
	public HealthCheck(URI clusterHealthCheck, HttpHealth healthCheckService, int historySize, MeterRegistry meterRegistry) {
		super();
		this.clusterHealthCheck = requireNonNull(clusterHealthCheck);
		this.healthCheckService = requireNonNull(healthCheckService);
		this.inetAddr = new InetSocketAddress(clusterHealthCheck.getHost(), 443); 
	    this.state = new State();
	    this.cleanable = cleaner.register(this, state);
	    this.historySize = historySize;

	    
	    Location location;
	    try {
	    	location = Location.from(clusterHealthCheck);
	    } catch(IllegalArgumentException ignored) {
	    	location = new Location(new Partition("", ""), "", "");
	    }
	    
	    tcpTimerSuccess = Timer.builder("healths")
	    		.tags("datacenter", location.getDatacenter(), "namespace", location.getPartition().getNamespace(), "partition", location.getPartition().getPartition(), "outcome", "success", "service", location.getService())
	    		.register(meterRegistry);
	    tcpTimerFail = Timer.builder("healths")
	    		.tags("datacenter", location.getDatacenter(), "namespace", location.getPartition().getNamespace(), "partition", location.getPartition().getPartition(), "outcome", "fail", "service", location.getService())
	    		.register(meterRegistry);
	    
	    List<Tag> tags = List.of(
	    		Tag.of("datacenter", location.getDatacenter()),
	    		Tag.of("namespace", location.getPartition().getNamespace()),
	    		Tag.of("partition", location.getPartition().getPartition()),
	    		Tag.of("service", location.getService()));
	    errors = Counter.builder("health.error")
	    		.tags(tags)
	    		.register(meterRegistry);
	    
	    success = Counter.builder("health.success")
	    		.tags(tags)
	    		.register(meterRegistry);
	}
	
	synchronized void setHealth(boolean success) {
		if (current == historySize) {
			current = 0;
		}
		history.set(current++, !success);
		
		if (size < historySize) {
			size++;
		}
	}
	
	private synchronized void setTcpHealth(boolean success) {
		if (currentTcp == HISTORY_TCP_SIZE) {
			currentTcp = 0;
		}
		historyTcp.set(currentTcp++, !success);
		
		if (sizeTcp < HISTORY_TCP_SIZE) {
			sizeTcp++;
		}
	}

	public void start() {	
		handle = executor.scheduleWithFixedDelay(this::runCheck, 50, CHECK_DELAY, TimeUnit.MILLISECONDS);
		handleTcp = executor.scheduleWithFixedDelay(this::runTcpCheck, 0, CHECK_DELAY, TimeUnit.MILLISECONDS);
	}
	
	public synchronized Health health() {
		return new Health(clusterHealthCheck.toString(), size, history.cardinality(), Duration.ofNanos(sizeTcp * lap), sizeTcp, historyTcp.cardinality());
	}


	@Value
	public static class Health extends RepresentationModel<Health>{
		private String cluster;
		private int nbRequests;
		private int nbFailedRequests;
		private Duration window;
		
		private int nbTcpConnect;
		private int nbTcpConnectFailed;
		
		public Health(String cluster, int nbRequests, int nbFailedRequests, Duration window, int nbTcpConnect, int nbTcpConnectFailed) {
			super();
			this.cluster = cluster;
			this.nbRequests = nbRequests;
			this.nbFailedRequests = nbFailedRequests;
			this.window = window;
			this.nbTcpConnect = nbTcpConnect;
			this.nbTcpConnectFailed =nbTcpConnectFailed;
		}

		public String getCluster() {
			return cluster;
		}

		@JsonIgnore
		public int getNbRequests() {
			return nbRequests;
		}

		@JsonIgnore
		public int getNbFailedRequests() {
			return nbFailedRequests;
		}

		public Duration getWindow() {
			return window;
		}

		public double getErrorRate() {
			if (getNbTcpConnect() == 0) {
				return 0D;
			}
			
			return getNbTcpConnectFailed() / (double)getNbTcpConnect();
		}
		
		@JsonIgnore
		public Location location() {
			return Location.fromHost(cluster);
		}

		public int getNbTcpConnectFailed() {
			return nbTcpConnectFailed;
		}

		public int getNbTcpConnect() {
			return nbTcpConnect;
		}
	}

	private void runCheck() {
		try {
			healthCheckService.testHealth(clusterHealthCheck);
			setHealth(true);
		} catch (HealthCheckException e) {
			//logger.info("Health Check failed " + e.getRemoteMessage(), e);
			setHealth(false);
		}
	}
	
	private void runTcpCheck() {
		long start = System.nanoTime();
		long duration;
		try {
			TcpConnectService.testConnect(inetAddr);
		    duration = System.nanoTime() - start + CHECK_DELAY * 1_000_000;
			setTcpHealth(true);
			
			tcpTimerSuccess.record(duration, TimeUnit.NANOSECONDS);
			success.increment();
		} catch (HealthCheckException e) {
			duration = System.nanoTime() - start + CHECK_DELAY * 1_000_000;
			setTcpHealth(false);
			
			tcpTimerFail.record(duration, TimeUnit.NANOSECONDS);
			errors.increment();
		} catch (Exception e) {
			duration = System.nanoTime() - start + CHECK_DELAY * 1_000_000;
			logger.warn("TCP check failed ", e);
		} catch (Throwable e) {
			duration = System.nanoTime() - start + CHECK_DELAY * 1_000_000;
			logger.error("TCP check failed ", e);
		}

		
		if (lap == 0) {
			lap = duration;
		} else {
			lap = 2 * duration / sizeTcp + (lap * (sizeTcp - 1))/ sizeTcp;
		}
	}

	@Override
	public void close() {
		cleanable.clean();
	}

	public boolean isActive() {
		return handleTcp != null && !handleTcp.isCancelled();
	}
}
