package com.microsoft.voiceapps.testcluster.healthcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * Repository for all the healthchecks.
 */
@Component
@Scope("singleton")
public class HealthCheckRepository {
	private Map<String, Map<Partition, Map<String, HealthCheck>>> locations = new ConcurrentHashMap<>();
	
	public void add(Location location, HealthCheck health) {
		locations.computeIfAbsent(location.getPartition().getNamespace(), k -> new ConcurrentHashMap<>())
		         .computeIfAbsent(location.getPartition(), k -> new ConcurrentHashMap<>())
		         .put(key(location), health);
	}

	private String key(Location location) {
		return location.getDatacenter() + "#" + location.getService();
	}
	
	public Optional<HealthCheck> findOne(Location location) {
		var partitions = locations.get(location.getPartition().getNamespace());
		
		if (partitions == null) {
			return Optional.empty();
		}
		
		Map<String, HealthCheck> inPart = partitions.get(location.getPartition());
		
		if (inPart == null) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(inPart.get(key(location)));
	}
	
	/**
	 * Return HealthChecks matching the parameters.
	 * @param namespace
	 * @param partitionPattern can be null
	 * @return HealthChecks matching the parameters.
	 */
	public List<HealthCheck> find(String namespace, String partitionPattern) {
		var partitions = locations.get(namespace);
		
		if (partitions == null) {
			return Collections.emptyList();
		}
		
		List<HealthCheck> result = new ArrayList<>();
		for (Partition partition : partitions.keySet()) {
			if (Strings.isNullOrEmpty(partitionPattern) || partition.getPartition().contains(partitionPattern)) {
				result.addAll(partitions.get(partition).values());
			}
		}
		return result;
	}

	public void clear() {
		locations.clear();
	}

	public Optional<HealthCheck> remove(Location location) {
		var partitions = locations.get(location.getPartition().getNamespace());
		
		if (partitions == null) {
			return Optional.empty();
		}
		
		Map<String, HealthCheck> inPart = partitions.get(location.getPartition());
		
		if (inPart == null) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(inPart.remove(key(location)));
	}

	public boolean exists(String namespace) {
		return locations.containsKey(namespace);
	}

	public List<HealthCheck> all() {
		return locations.values()
				        .stream()
				        .flatMap(map -> map.values().stream())
				        .flatMap(map -> map.values().stream())
				        .toList();
	}
}