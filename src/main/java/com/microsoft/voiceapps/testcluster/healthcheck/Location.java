package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Objects;

public class Location  {
	private String datacenter;
	private Partition partition;
	
	public Location(Partition partition, String datacenter) {
		super();
		this.datacenter = requireNonNull(datacenter);
		this.partition = requireNonNull(partition);
	}

	public String getDatacenter() {
		return datacenter;
	}

	public Partition getPartition() {
		return partition;
	}

	@Override
	public int hashCode() {
		return Objects.hash(datacenter, partition);
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
		return Objects.equals(datacenter, other.datacenter) && Objects.equals(partition, other.partition);
	}
	
	@Override
	public String toString() {
		return "Location [datacenter=" + datacenter + ", partition=" + partition + "]";
	}

	public static Location from(URI uri) {
		try {
			String[] parts = uri.getHost().split("\\.");

			String namespace = parts[2];
			String partition = parts[1];
			String datacenter = parts[3];

			return new Location(new Partition(namespace, partition), datacenter);
		} catch(Exception e) {
			throw new IllegalArgumentException(uri.toString());
		}
	}
}