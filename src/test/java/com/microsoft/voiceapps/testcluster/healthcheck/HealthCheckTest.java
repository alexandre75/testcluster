package com.microsoft.voiceapps.testcluster.healthcheck;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck.Health;
import com.microsoft.voiceapps.testcluster.service.HealthCheckException;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

class HealthCheckTest {
	private HealthCheck subject;
	private URI uri = URI.create("https://alex/me");
	
	@Mock
	private HealthCheckService healthCheckService;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		subject = new HealthCheck(uri, healthCheckService);
	}
	
	@AfterEach
	void tearDown() {
		subject.stop();
	}

	@Test
	void shouldCallHealthCheck() throws HealthCheckException, InterruptedException {
		subject.start();
		
		Thread.sleep(100);
		verify(healthCheckService, atLeastOnce()).testHealth(uri);
	}

	@Test
	void shouldUpdateHealthCheck() throws HealthCheckException, InterruptedException {
		subject.start();
		
		Thread.sleep(100);
		
		Assertions.assertTrue(subject.health().getNbRequests() > 0);
		Assertions.assertTrue(subject.health().getNbFailedRequests() == 0);
	}
	

	@Test
	void shouldUpdateHealthCheckWhenFail() throws InterruptedException, HealthCheckException {
		doThrow(HealthCheckException.class).when(healthCheckService).testHealth(uri);
		subject.start();
		
		Thread.sleep(100);
		
		Health result = subject.health();
		Assertions.assertTrue(result.getNbRequests() > 0);
		Assertions.assertTrue(result.getNbFailedRequests() == result.getNbRequests());
	}
	
	@Test
	void shouldLoop() throws HealthCheckException, InterruptedException {

		for (int i = 0 ; i < 750 ; i++) {
			subject.setHealth(false);
			subject.setHealth(true);
		}

		assertEquals(1000, subject.health().getNbRequests());
		assertEquals(500, subject.health().getNbFailedRequests());
	}
	
	@Test
	void shouldRingWorks() throws HealthCheckException, InterruptedException {

		for (int i = 0 ; i < 1000 ; i++) {
			subject.setHealth(false);
		}
		for (int i = 0 ; i < 1000 ; i++) {
			subject.setHealth(true);
		}

		assertEquals(0, subject.health().getNbFailedRequests());
	}
	
	@Test
	void shouldRingWorks2() throws HealthCheckException, InterruptedException {

		for (int i = 0 ; i < 1000 ; i++) {
			subject.setHealth(true);
		}
		for (int i = 0 ; i < 1000 ; i++) {
			subject.setHealth(false);
		}

		assertEquals(1000, subject.health().getNbFailedRequests());
	}
}
