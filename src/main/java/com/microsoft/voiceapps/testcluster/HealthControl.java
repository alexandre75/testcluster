package com.microsoft.voiceapps.testcluster;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.voiceapps.testcluster.healthcheck.Directory;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck.Health;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

@RestController
public class HealthControl {
	private final HealthCheckService healthCheckService;
	private final Directory directory;
	
	private static final Logger logger = LoggerFactory.getLogger(HealthControl.class);
	
	@Autowired
	public HealthControl(HealthCheckService healthCheckService, Directory directory) {
		super();
		this.healthCheckService = healthCheckService;
		this.directory = directory;
	}
	
	@GetMapping("/health/{namespace}/{partition}")
	ResponseEntity<List<Health>> health(@PathVariable String namespace, @PathVariable String partition) {
		logger.info("GET /health/"+namespace+"/" + partition);
	    var res =  directory.partition(new Partition(namespace, partition))
	    		        .stream()
	    		        .map(healthCheck -> healthCheck.health())
	    		        .collect(Collectors.toList());
	    if (res.isEmpty()) {
	    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
	    } else {
	    	return ResponseEntity.status(HttpStatus.OK).body(res); 
	    }
	}
	
	@PostMapping("/register")
	ResponseEntity register(@RequestBody Request request) {
		if (request.getUris() == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); 
		}
		logger.info("/register: " + request.toString());
		
	    List<URI> uris = request.uris.stream().map(URI::create).toList();
	    
	    for (URI uri : uris) {
	    	Location location = Location.from(uri);
	    	HealthCheck healthcCheck = new HealthCheck(uri, healthCheckService);
	    	healthcCheck.start();
	    	directory.add(location, healthcCheck);
	    }  
	    return ResponseEntity.status(HttpStatus.CREATED).body(null); 
	}
	
	@ExceptionHandler({ IllegalArgumentException.class })
	public ResponseEntity<?> handleDataIntegrityViolationException(IllegalArgumentException e) {
		logger.warn("Parsing exception:" + e.toString());
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.toString(), "timestamp", LocalDateTime.now()));
	}
}

class Request {
	List<String> uris;

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

