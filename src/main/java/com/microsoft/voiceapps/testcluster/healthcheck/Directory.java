package com.microsoft.voiceapps.testcluster.healthcheck;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
	
	public Optional<HealthCheck> findOne(Location location) {
		Map<String, HealthCheck> inPart = locations.get(location.getPartition());
		
		if (inPart == null) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(inPart.get(location.getDatacenter()));
	}
	
	public Collection<HealthCheck> partition(Partition partition) {
		return Collections.unmodifiableCollection(locations.getOrDefault(partition, Map.of()).values());
	}

	public Collection<HealthCheck> remove(Partition partition) {
		Map<String, HealthCheck> deleted = locations.remove(partition);
		
		if (deleted == null) {
			return Collections.emptyList();
		} else {
			return deleted.values();
		}
	}

	public Collection<Partition> partitions() {
		return Collections.unmodifiableCollection(locations.keySet());
	}

	public void clear() {
		locations.clear();
	}
}