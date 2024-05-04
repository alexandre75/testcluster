package com.microsoft.voiceapps.testcluster.healthcheck;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class Directory {
	private Map<Partition, Map<String, HealthCheck>> locations = new ConcurrentHashMap<>();
	
	public void add(Location location, HealthCheck health) {
		locations.computeIfAbsent(location.getPartition(), k -> new ConcurrentHashMap<>())
		         .put(location.getDatacenter(), health);
	}
	
	public Collection<HealthCheck> partition(Partition partition) {
		return Collections.unmodifiableCollection(locations.getOrDefault(partition, Map.of()).values());
	}
}