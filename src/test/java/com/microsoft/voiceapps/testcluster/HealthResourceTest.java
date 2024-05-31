package com.microsoft.voiceapps.testcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckRepository;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

class HealthResourceTest {
	private static final URI CLUSTER_HEALTH_CHECK = URI.create("http://nds-webrole-svc.apac-b.ic3-sbvvoiceapps-nds.malaysiasouth-prod.cosmic.office.net/health");

	HealthResource subject;
	
	@Mock
	HealthCheckService healthCheckService;
	HealthCheckRepository directory = new HealthCheckRepository();
	
	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
		
		subject = new HealthResource(directory);
	}
	
	@Test
	void shouldFilteNamespace() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("unkown", Optional.empty(), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
	}
	
	@Test
	void shouldReturnNamespaceHealth() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().getContent().size());
	}
	
	@Test
	void shouldReturnNamespaceHealthFilterWork() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("namespace", Optional.of("rti"), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().getContent().size());
	}
	
	@Test
	void shouldReturnNamespaceHealthFilterExclude() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("namespace", Optional.of("unknown"), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertTrue(response.getBody().getContent().isEmpty());
	}
	
	@Test
	void shouldFilterLowErrorRate() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.of(0.1F));
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(0, response.getBody().getContent().size());
	}
	
	@Test
	void shouldShowHighErrorRate() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.of(0.0F));
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().getContent().size());
	}
	
	@Test
	void shouldReaturnHealth() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.health("namespace", "partition", "region");
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertNotNull(response.getBody());
	}
	
	@Test
	void shouldReturn404() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(CLUSTER_HEALTH_CHECK, healthCheckService, 1000));
	    
	    var response = subject.health("namespace", "partition", "region2");
	    
	    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
	}
}
