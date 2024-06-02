package com.microsoft.voiceapps.testcluster.healthcheck;

import lombok.Getter;

public class HealthCheckException extends Exception {
	
	@Getter
	private String remoteMessage;

	/**
	 * 
	 */
	private static final long serialVersionUID = 5385712623634780444L;

	public HealthCheckException(String message, String remoteMessage, Throwable cause) {
		super(message, cause);
		this.remoteMessage = remoteMessage;
	}

	public HealthCheckException(String message, String remoteMessage) {
		super(message);
		this.remoteMessage = remoteMessage;
	}

	public String getRemoteMessage() {
		return remoteMessage;
	}
}
