package com.microsoft.voiceapps.testcluster.healthcheck;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

class DirectoryTest {
	
	Directory subject = new Directory();

	@Test
	void test() {
		Partition apacA = new Partition("namespace", "apac-a");
		Partition apacB = new Partition("namespace", "apac-b");
		
		Location japan = new Location(apacA, "japan");
		Location malaysia = new Location(apacA, "malaysia");
		Location malaysiaB = new Location(apacB, "malaysia");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), new HealthCheckService());
		HealthCheck hMalaysia = new HealthCheck(URI.create("http://noam/me"), new HealthCheckService());
		
		subject.add(malaysia, hMalaysia);
		subject.add(japan, hJapan);
		subject.add(malaysiaB, new HealthCheck(URI.create("http://noam/me"), new HealthCheckService()));
		
		Collection<HealthCheck> healthChecks = subject.partition(apacA);
		
		assertEquals(2, healthChecks.size());
	}
	
	@Test
	void shouldDelete() {
		Partition apacA = new Partition("namespace", "apac-a");
		
		Location japan = new Location(apacA, "japan");
		Location malaysia = new Location(apacA, "malaysia");
		HealthCheck hJapan = new HealthCheck(URI.create("http://noam/me"), new HealthCheckService());
		HealthCheck hMalaysia = new HealthCheck(URI.create("http://noam/me"), new HealthCheckService());
		
		subject.add(malaysia, hMalaysia);
		subject.add(japan, hJapan);
		
		Collection<HealthCheck> healthChecks = subject.remove(apacA);
		
		assertEquals(2, healthChecks.size());
		assertEquals(0, subject.partition(apacA).size());
	}
	
	@Test
	void shouldReturnEmptyWhenNoDelete() {
		Partition apacA = new Partition("namespace", "apac-a");		
		
		Collection<HealthCheck> healthChecks = subject.remove(apacA);
		
		assertEquals(0, healthChecks.size());
	}

}
