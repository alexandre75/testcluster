package com.microsoft.voiceapps.testcluster.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckException;
import com.microsoft.voiceapps.testcluster.healthcheck.HttpHealth;

@Service
@Scope("singleton")
@ThreadSafe
public class HealthCheckService implements HttpHealth {
	private static final int MAX_TRY = 6;
	
	X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
	    @Override
	    public X509Certificate[] getAcceptedIssuers() {
	        return new X509Certificate[]{};
	    }

	    @Override
	    public void checkClientTrusted(X509Certificate[] chain, String authType) {
	    }

	    @Override
	    public void checkServerTrusted(X509Certificate[] chain, String authType) {
	    }

	    @Override
	    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
	    }

	    @Override
	    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
	    }

	    @Override
	    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
	    }

	    @Override
	    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
	    }
	};
	
	private final SSLContext sslContext;
	private final Duration timeout;
	
	@GuardedBy("httpClientLock") private LocalDateTime threshold;
	@GuardedBy("httpClientLock") private HttpClient httpClient;
	private Object httpClientLock = new Object();
	
	@Autowired
	public HealthCheckService() {
		this(Duration.ofSeconds(15));
	}
	
	public HealthCheckService(Duration timeout) {
		System.setProperty("jdk.httpclient.keepalive.timeout", "20");
		
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		this.timeout = requireNonNull(timeout);
		
		httpClient = createHttpClient();
		threshold = LocalDateTime.now().plusMinutes(2);
	}

	private HttpClient createHttpClient() {	
			 return HttpClient.newBuilder()
					        .connectTimeout(Duration.ofSeconds(1))
					        .sslContext(sslContext) // SSL context 'sc' initialised as earlier
					     //   .sslParameters(parameters) // ssl parameters if overriden
					        .build();
	}
	
	@Override
	public void testHealth(URI uri) throws HealthCheckException {
		requireNonNull(uri);
		
		long endMax = System.nanoTime() + 10L * 1_000_000_000L;
		int trial = 0;
		while (true) {
			trial++;
			HttpResponse<String> response;
			try {
				response = getHttpClient()
						.send(HttpRequest.newBuilder(uri).timeout(timeout).build(), HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() != 200) {
					throw new HealthCheckException("uri" + uri.toString(), "" + response.statusCode());
				}
				return;
			} catch (IOException e) {
				if (trial >= MAX_TRY || System.nanoTime() > endMax) {
					throw new HealthCheckException("uri :" + uri.toString(), e.getMessage(), e);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private HttpClient getHttpClient() {
//		synchronized(httpClientLock) {
//			if (LocalDateTime.now().isAfter(threshold)) {
//				httpClient = createHttpClient();
//				threshold = LocalDateTime.now().plusMinutes(2);
//			}
			return httpClient;
//		}
	}
}
