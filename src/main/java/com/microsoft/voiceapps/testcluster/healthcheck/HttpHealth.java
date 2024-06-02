package com.microsoft.voiceapps.testcluster.healthcheck;

import java.net.URI;

public interface HttpHealth {
	public void testHealth(URI uri) throws HealthCheckException;
}
