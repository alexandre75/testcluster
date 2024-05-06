package com.microsoft.voiceapps.testcluster;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheck.Health;
import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckRepository;
import com.microsoft.voiceapps.testcluster.healthcheck.Location;
import com.microsoft.voiceapps.testcluster.healthcheck.Partition;

@RestController
@RequestMapping("healths")
public class HealthResource {
	private final HealthCheckRepository directory;
	
	private static final Logger logger = LoggerFactory.getLogger(HealthResource.class);
	
	@Autowired
	public HealthResource(HealthCheckRepository directory) {
		super();
		this.directory = directory;
	}
	
	@GetMapping("/{namespace}/{partition}/{datacenter}")
	ResponseEntity<EntityModel<Health>> health(@PathVariable String namespace, @PathVariable String partition, @PathVariable String datacenter) {
		logger.info("GET /health/"+namespace+"/" + partition + "/" + datacenter);
		datacenter = datacenter.split("-")[0];
	    Optional<HealthCheck> res =  directory.findOne(new Location(new Partition(namespace, partition), datacenter));

	    if (res.isEmpty()) {
	    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
	    } else {
	    	Link link = linkTo(HealthResource.class).slash(namespace).slash(partition).slash(datacenter).withSelfRel();
	    	return ResponseEntity.status(HttpStatus.OK)
	    			             .cacheControl(CacheControl.maxAge(3, TimeUnit.MINUTES))
	    			             .body(EntityModel.of(res.get().health(), link)); 
	    }
	}
	
	@GetMapping("/{namespace}")
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
}


