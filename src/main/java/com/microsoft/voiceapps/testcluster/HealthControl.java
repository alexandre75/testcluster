package com.microsoft.voiceapps.testcluster;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.voiceapps.testcluster.healthcheck.Directory;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck.Health;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;
import com.microsoft.voiceapps.testcluster.service.HealthCheckService;

import jakarta.annotation.PostConstruct;

@RestController
public class HealthControl {
	private static final String CONFIG_FILE_NAME = "config.json";
	private final HealthCheckService healthCheckService;
	private final Directory directory;
	
	private static final Logger logger = LoggerFactory.getLogger(HealthControl.class);
	private static final Set<String> registered = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final Object lock = new Object();
	private static boolean initialized;
	
	@Autowired
	public HealthControl(HealthCheckService healthCheckService, Directory directory) {
		super();
		this.healthCheckService = healthCheckService;
		this.directory = directory;
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
	
	@GetMapping("/healths/{namespace}/{partition}/{datacenter}")
	ResponseEntity<Health> health(@PathVariable String namespace, @PathVariable String partition, @PathVariable String datacenter) {
		logger.info("GET /health/"+namespace+"/" + partition + "/" + datacenter);
	    Optional<HealthCheck> res =  directory.findOne(new Location(new Partition(namespace, partition), datacenter));

	    if (res.isEmpty()) {
	    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
	    } else {
	    	return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(3, TimeUnit.MINUTES)).body(res.get().health()); 
	    }
	}
	
	@GetMapping("/healths/{namespace}")
	ResponseEntity<List<Health>> healthNamespace(@PathVariable String namespace, 
			@RequestParam("partition-contains") Optional<String> partitionFilter, 
			@RequestParam("error-rate") Optional<Float> errorRate) {
		logger.info("GET /health/"+namespace + "?" + partitionFilter + "&errorRate=" + errorRate);
		Objects.requireNonNull(namespace);
		
		if (!directory.exists(namespace)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
		}
		
		var res = directory.find(namespace, partitionFilter.orElse(null))
				        .stream()
	    		        .map(healthCheck -> healthCheck.health())
	    		        .filter(health -> compareErrorRate(errorRate, health)) // no point in showing 100% fail
	    		        .collect(Collectors.toList());
	    return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES)).body(res); 
	}

	private boolean compareErrorRate(Optional<Float> errorRate, Health health) {
		if (errorRate.isEmpty()) return true;
		
		return health.getErrorRate() >= errorRate.orElse(0F) && health.getErrorRate() != 1F;
	}
	
	@DeleteMapping("/cluster/{namespace}/{partition}/{datacenter}")
	ResponseEntity<?> delete(@PathVariable String namespace, @PathVariable String partition, @PathVariable String datacenter) {
		logger.info("DELETE /health/"+namespace+"/" + partition);
	    Optional<HealthCheck> res =  directory.remove(new Location(new Partition(namespace, partition), datacenter));
	    if (res.isEmpty()) {
	    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
	    } else {
	    	return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null); 
	    }
	}
	
	@PostMapping("/cluster/from-uris")
	ResponseEntity<?> register(@RequestBody Request request) {
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

