package com.microsoft.voiceapps.testcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;

import com.microsoft.voiceapps.testcluster.healthcheck.Directory;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

class HealthControlTest {
	HealthControl subject;
	
	@Mock
	HealthCheckService healthCheckService;
	Directory directory = new Directory();
	
	@BeforeEach
	void init() {
		MockitoAnnotations.openMocks(this);
		
		subject = new HealthControl(healthCheckService, directory);
	}
	
	@Test
	void shouldRegister() {
		Request request = new Request(List.of("https://envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net/api/voicemail/probe"));
		
		subject.register(request);
	
		HealthCheck health = directory.partition(new Partition("ic3-sbvmessaging-vms", "df-a")).iterator().next();
		assertEquals("envoy.df-a.ic3-sbvmessaging-vms.eastus-msit.cosmic.office.net", health.health().getCluster());
	}

	
	@Test
	void shouldReturnHealth() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.partitionHealth("namespace", "partition");
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().size());
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
	void shouldFilteNamespace() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("unkown", Optional.empty(), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
	}
	
	@Test
	void shouldReturnNamespaceHealth() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().size());
	}
	
	@Test
	void shouldReturnNamespaceHealthFilterWork() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("namespace", Optional.of("rti"), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().size());
	}
	
	@Test
	void shouldReturnNamespaceHealthFilterExclude() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("namespace", Optional.of("unknown"), Optional.empty());
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertTrue(response.getBody().isEmpty());
	}
	
	@Test
	void shouldFilterLowErrorRate() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.of(0.1F));
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(0, response.getBody().size());
	}
	
	@Test
	void shouldShowHighErrorRate() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.healthNamespace("namespace", Optional.empty(), Optional.of(0.0F));
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertEquals(1, response.getBody().size());
	}
	
	@Test
	void shouldReaturnHealth() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.health("namespace", "partition", "region");
	    
	    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
	    assertNotNull(response.getBody());
	}
	
	@Test
	void shouldReturn404() {
		Partition partition = new Partition("namespace", "partition");
	    directory.add(new Location(partition, "region"), new HealthCheck(URI.create("http://alex"), healthCheckService));
	    
	    var response = subject.health("namespace", "partition", "region2");
	    
	    assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
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
