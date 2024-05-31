package com.microsoft.voiceapps.testcluster;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckRepository;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping(value="/cluster")
public class ClusterResource {
	private static final String CONFIG_FILE_NAME = "config.json";
	private final HealthCheckService healthCheckService;
	private final HealthCheckRepository directory;
	private final MeterRegistry meterRegistry;
	
	private static final Logger logger = LoggerFactory.getLogger(HealthResource.class);
	private static final Set<String> registered = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final Object lock = new Object();
	private static boolean initialized;
	
	@Autowired
	public ClusterResource(HealthCheckService healthCheckService, HealthCheckRepository directory, MeterRegistry meterRegistry) {
		super();
		this.healthCheckService = healthCheckService;
		this.directory = directory;
		this.meterRegistry = meterRegistry;
	}
	
	@PostConstruct
	public void init() {
		synchronized(lock) {
			if (!initialized) {
				ObjectMapper mapper = new ObjectMapper();
				Request req;
				try {
					req = mapper.readValue(new File(CONFIG_FILE_NAME), Request.class);
					register(req);
				} catch (IOException e) {
					logger.error("Init failed", e);
				}
				initialized = true;
			}
		}
	}
	
	@DeleteMapping("/{namespace}/{partition}/{datacenter}")
	ResponseEntity<?> delete(@PathVariable String namespace, @PathVariable String partition, @PathVariable String datacenter) {
		logger.info("DELETE /health/"+namespace+"/" + partition);
	    Optional<HealthCheck> res =  directory.remove(new Location(new Partition(namespace, partition), datacenter));
	    if (res.isEmpty()) {
	    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
	    } else {
	    	return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null); 
	    }
	}
	
	@PostMapping("/from-uris")
	ResponseEntity<?> register(@RequestBody Request request) {
		if (request.getUris() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); 
		}
		logger.info("/register: " + request.toString());
		
	    List<URI> uris = request.uris.stream().map(URI::create).toList();
	    
	    for (URI uri : uris) {
	    	Location location = Location.from(uri);
	    	HealthCheck healthcCheck = new HealthCheck(uri, healthCheckService, 100_000, meterRegistry);
	    	healthcCheck.start();
	    	directory.add(location, healthcCheck);
	    	
	    	registered.addAll(request.getUris());
	    	
	    	save();
	    }  
	    
	    return ResponseEntity.status(HttpStatus.CREATED).body(null); 
	}
	
	private void save() {
		Request req = new Request();
		req.setUris(new ArrayList<>(registered));
		
		ObjectMapper mapper = new ObjectMapper();
		synchronized(lock)
		{
			try {
				mapper.writeValue(new File(CONFIG_FILE_NAME), req);
			} catch (IOException e) {
				logger.error("Can't save", e);
			}
		}
	}

	@ExceptionHandler({ IllegalArgumentException.class })
	public ResponseEntity<?> handleDataIntegrityViolationException(IllegalArgumentException e) {
		logger.warn("Parsing exception:" + e.toString());
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.toString(), "timestamp", LocalDateTime.now()));
	}
}

class Request {
	List<String> uris;

	public Request() {
		
	}
	
	Request(List<String> uris) {
		super();
		this.uris = uris;
	}

	public List<String> getUris() {
		return uris;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	@Override
	public String toString() {
		return "Request [uris=" + uris + "]";
	}
}

