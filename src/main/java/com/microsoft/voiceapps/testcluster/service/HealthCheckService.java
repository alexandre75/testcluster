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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckService {
	
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
	
	SSLContext sslContext;
	
	
	
	@Autowired
	public HealthCheckService() {
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void testHealth(URI uri) throws HealthCheckException {
		requireNonNull(uri);
		
		HttpClient client = HttpClient.newBuilder()
		        .connectTimeout(Duration.ofSeconds(15))
		        .sslContext(sslContext) // SSL context 'sc' initialised as earlier
		     //   .sslParameters(parameters) // ssl parameters if overriden
		        .build();
		
		HttpResponse<String> response;
		try {
			response = client.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new HealthCheckException("uri" + uri.toString(), "" + response.statusCode());
			}
		} catch (IOException e) {
			throw new HealthCheckException("uri :" + uri.toString(), e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
