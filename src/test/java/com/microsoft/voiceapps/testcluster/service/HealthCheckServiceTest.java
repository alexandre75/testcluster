package com.microsoft.voiceapps.testcluster.service;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class HealthCheckServiceTest {

	private HealthCheckService subject = new HealthCheckService(Duration.ofSeconds(1), 2);
	
	@Test
	void testOK() throws HealthCheckException {
		URI uri = URI.create("https://envoy.nam-a.ic3-sbvmessaging-vms.eastus-prod.cosmic.office.net/api/voicemail/probe");
		
		subject.testHealth(uri);
	}
	
	@Test
	void testTimeout() {
		URI uri = URI.create("https://envoy.nam-a.ic3-sbvmessaging-vms.eastus-prod.cosmic.office.net:3432/api/voicemail/probe");
		
		assertThrows(HealthCheckException.class, () -> subject.testHealth(uri));
	}

}
