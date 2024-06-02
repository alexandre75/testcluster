package com.microsoft.voiceapps.testcluster.service;

import static java.util.Objects.requireNonNull;

import java.net.Socket;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.microsoft.voiceapps.testcluster.healthcheck.HealthCheckException;
import com.microsoft.voiceapps.testcluster.healthcheck.HttpHealth;

@Service("Rest")
@ThreadSafe
public class MeteredHttpService implements HttpHealth {
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
	private RestTemplate restTemplate;
	
	@Autowired
	public MeteredHttpService(RestTemplateBuilder restTemplateBuilder) {
		this(restTemplateBuilder, Duration.ofSeconds(15));
	}
	
	public MeteredHttpService(RestTemplateBuilder restTemplateBuilder, Duration timeout) {
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		restTemplate = restTemplateBuilder.setConnectTimeout(timeout)
				                          .setReadTimeout(timeout)
				                          .build();
	}
	
	@Override
	public void testHealth(URI uri) throws HealthCheckException {
		requireNonNull(uri);

		try {
			ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

			if (response.getStatusCode().isError()) {
				throw new HealthCheckException("uri" + uri.toString(), "" + response.getStatusCode());
			}
			return;
		} catch (RestClientException e) {
			throw new HealthCheckException("uri :" + uri.toString(), e.getMessage(), e);
		}
	}
}
