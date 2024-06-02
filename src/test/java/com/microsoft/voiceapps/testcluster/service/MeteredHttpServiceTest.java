package com.microsoft.voiceapps.testcluster.service;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckException;

@WireMockTest(httpsEnabled = true)
class MeteredHttpServiceTest {
	MeteredHttpService subject = new MeteredHttpService(new RestTemplateBuilder(), Duration.ofSeconds(1));

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
	
	@Test
	void realTest() throws HealthCheckException {	
		URI uri = URI.create("https://voicemail.eur-a.ic3-sbvmessaging-vms.northeurope-prod.cosmic.office.net/api/voicemail/healthcheck");
		
		subject.testHealth(uri);
	}
}
