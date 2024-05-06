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
	HealthCheckService healthCheckService = new HealthCheckService(Duration.ofSeconds(1), 1);

	@Test
	void test() {
		Partition apacA = new Partition("namespace", "apac-a");
		Partition apacB = new Partition("namespace", "apac-b");
		
		Location japan = new Location(apacA, "japan");
		Location malaysia = new Location(apacA, "malaysia");
		Location malaysiaB = new Location(apacB, "malaysia");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		HealthCheck hMalaysia = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		
		subject.add(malaysia, hMalaysia);
		subject.add(japan, hJapan);
		subject.add(malaysiaB, new HealthCheck(URI.create("http://noam/me"), healthCheckService));
		
		Collection<HealthCheck> healthChecks = subject.partition(apacA);
		
		assertEquals(2, healthChecks.size());
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
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		subject.add(japan, hJapan);
		
		Optional<HealthCheck> result = subject.findOne(new Location(apacA, "malaysia"));
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldReturnHealth() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		subject.add(japan, hJapan);
		
		Optional<HealthCheck> result = subject.findOne(japan);
		
		assertEquals(hJapan, result.get());
	}
	
	@Test
	void shouldReturnEmptyFind() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		subject.add(japan, hJapan);
		
		List<HealthCheck> result = subject.find("namespace", "apac-b");
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldMatchPartition() {
		Partition apacA = new Partition("namespace", "apac-a");		
		Location japan = new Location(apacA, "japan");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
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
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), healthCheckService);
		subject.add(japan, hJapan);
		
		List<HealthCheck> result = subject.find("namespace", null);
		
		assertFalse(result.isEmpty());
	}
}
