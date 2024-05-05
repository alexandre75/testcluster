package com.microsoft.voiceapps.testcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckRepository;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

public class ClusterResourceTest {
	ClusterResource subject;
	
	@Mock
	HealthCheckService healthCheckService;
	HealthCheckRepository directory = new HealthCheckRepository();
	
	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
		
		subject = new ClusterResource(healthCheckService, directory);
	}
	
	@Test
	void shouldRegister() {
		Request request = new Request(List.of("https://envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net/api/voicemail/probe"));
		
		subject.register(request);
	
		HealthCheck health = directory.partition(new Partition("ic3-sbvmessaging-vms", "df-a")).iterator().next();
		assertEquals("envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net", health.health().getCluster());
	}
	
	@Test
	void shouldPersistConfig() {
		Request request = new Request(List.of("https://envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net/api/voicemail/probe"));
		subject.register(request);
	
		directory.clear();
		subject.init();
		
		HealthCheck health = directory.partition(new Partition("ic3-sbvmessaging-vms", "df-a")).iterator().next();
		assertEquals("envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net", health.health().getCluster());
	}
	
	@Test
	void shouldDeleteHealthCheck() {
		Partition partition = new Partition("namespace", "partition");
	    Location location = new Location(partition, "region");
		directory.add(location, new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.delete("namespace", "partition", "region");
	    
	    assertTrue(directory.findOne(location).isEmpty());
	    assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());
	}
	
	@Test
	void deleteShouldReturn404() {	    
	    var response = subject.delete("namespace", "partition", "region1");
	    
	    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
	}
}
