package com.microsoft.voiceapps.testcluster.service;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest(httpsEnabled = true)
class HealthCheckServiceTest {

	private HealthCheckService subject = new HealthCheckService(Duration.ofSeconds(1));
	
	@Test
	void testOK(WireMockRuntimeInfo wmRuntimeInfo) throws HealthCheckException {
        stubFor(get("/api/voicemail/probe").willReturn(ok()));		
		URI uri = URI.create("https://localhost:" + wmRuntimeInfo.getHttpsPort() + "/api/voicemail/probe");
		
		subject.testHealth(uri);
	}
	
	@Test
	void testTimeout(WireMockRuntimeInfo wmRuntimeInfo) {
		stubFor(get("/api/voicemail/probe").willReturn(ok().withFixedDelay(2000)));
		URI uri = URI.create("https://localhost:" + wmRuntimeInfo.getHttpsPort() + "/api/voicemail/probe");
		
		assertThrows(HealthCheckException.class, () -> subject.testHealth(uri));
	}

	@Test
	void testKO(WireMockRuntimeInfo wmRuntimeInfo) throws HealthCheckException {
        stubFor(get("/api/voicemail/probe").willReturn(WireMock.forbidden()));		
		URI uri = URI.create("https://localhost:" + wmRuntimeInfo.getHttpsPort() + "/api/voicemail/probe");
		
		assertThrows(HealthCheckException.class, () -> subject.testHealth(uri));
	}
}
