package com.microsoft.voiceapps.testcluster.healthcheck;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class Partition {
	private String namespace;
	private String partition;
	
	public Partition(String namespace, String partition) {
		super();
		this.namespace = requireNonNull(namespace);
		this.partition = requireNonNull(partition);
	}
	
	public String getNamespace() {
		return namespace;
	}

	public String getPartition() {
		return partition;
	}

	@Override
	public int hashCode() {
		return Objects.hash(namespace, partition);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Partition other = (Partition) obj;
		return Objects.equals(namespace, other.namespace) && Objects.equals(partition, other.partition);
	}
	
	@Override
	public String toString() {
		return "Partition [namespace=" + namespace + ", partition=" + partition + "]";
	}
}