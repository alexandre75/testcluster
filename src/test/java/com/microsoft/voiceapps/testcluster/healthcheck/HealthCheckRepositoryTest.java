package com.microsoft.voiceapps.testcluster.healthcheck;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

class HealthCheckRepositoryTest {
	
	HealthCheckRepository subject = new HealthCheckRepository();
	HealthCheckService healthCheckService = new HealthCheckService(Duration.ofSeconds(1));

	@Test
	void test() {
		Partition apacA = new Partition("namespace", "apac-a");
		Partition apacB = new Partition("namespace", "apac-b");
		
		Location japan = new Location(apacA, "japan");
		Location malaysia = new Location(apacA, "malaysia");
		Location malaysiaB = new Location(apacB, "malaysia");
		HealthCheck hJapan = createHealthCheck();
		HealthCheck hMalaysia = createHealthCheck();
		
		subject.add(malaysia, hMalaysia);
		subject.add(japan, hJapan);
		subject.add(malaysiaB, createHealthCheck());
		
		Collection<HealthCheck> healthChecks = subject.partition(apacA);
		
		assertEquals(2, healthChecks.size());
	}

	private HealthCheck createHealthCheck() {
		return new HealthCheck(URI.create("http://noam/me"), healthCheckService, 1000);
	}
	
	@Test
	void shouldReturnEmpty() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		
		Optional<HealthCheck> result = subject.findOne(japan);
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldReturnEmptyWhenPartitionExist() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = createHealthCheck();
		subject.add(japan, hJapan);
		
		Optional<HealthCheck> result = subject.findOne(new Location(apacA, "malaysia"));
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldReturnHealth() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = createHealthCheck();
		subject.add(japan, hJapan);
		
		Optional<HealthCheck> result = subject.findOne(japan);
		
		assertEquals(hJapan, result.get());
	}
	
	@Test
	void shouldReturnEmptyFind() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = createHealthCheck();
		subject.add(japan, hJapan);
		
		List<HealthCheck> result = subject.find("namespace", "apac-b");
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldMatchPartition() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = createHealthCheck();
		subject.add(japan, hJapan);
		
		List<HealthCheck> result = subject.find("namespace", "apac");
		
		assertFalse(result.isEmpty());
	}
	
	@Test
	void shouldReturnEmptyWhenNoNamspace() {	
		List<HealthCheck> result = subject.find("namespace", "apac-b");
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldMatchPartitionWhenNullPattern() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = createHealthCheck();
		subject.add(japan, hJapan);
		
		List<HealthCheck> result = subject.find("namespace", null);
		
		assertFalse(result.isEmpty());
	}
}
