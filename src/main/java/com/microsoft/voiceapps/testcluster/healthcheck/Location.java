package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Objects;

public class Location  {
	private final String datacenter;
	private final Partition partition;
	private final String service;
	
	Location(Partition partition, String datacenter) {
		this(partition, datacenter, "default");
	}
	
	public Location(Partition partition, String datacenter, String service) {
		this.service = requireNonNull(service);
		this.partition = requireNonNull(partition);
		this.datacenter = requireNonNull(datacenter);
	}

	public String getDatacenter() {
		return datacenter;
	}

	public Partition getPartition() {
		return partition;
	}
	
	public String getService() {
		return service;
	}

	@Override
	public int hashCode() {
		return Objects.hash(datacenter, partition, service);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		return Objects.equals(datacenter, other.datacenter) && Objects.equals(partition, other.partition) && Objects.equals(service,  other.service);
	}
	
	@Override
	public String toString() {
		return "Location [datacenter=" + datacenter + ", partition=" + partition + "]";
	}

	public static Location from(URI uri) {
		return fromHost(uri.getHost());
	}
	
	public static Location fromHost(String hostname) {
		try {
			String[] parts = hostname.split("\\.");

			String namespace = parts[2];
			String partition = parts[1];
			String datacenter = parts[3];
			String service = parts[0];

			return new Location(new Partition(namespace, partition), datacenter.split("-")[0], service);
		} catch(Exception e) {
			throw new IllegalArgumentException(hostname, e);
		}
	}
}