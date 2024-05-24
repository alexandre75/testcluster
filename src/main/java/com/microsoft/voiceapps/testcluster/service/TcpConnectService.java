package com.microsoft.voiceapps.testcluster.service;

import java.io.IOException;
import java.net.*;

public class TcpConnectService {
	private TcpConnectService() {	
	}
	
	public static void testConnect(InetSocketAddress addr) throws HealthCheckException {
		try (Socket socket = new Socket()) {
			socket.connect(addr, 5000);
		} catch(UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e1) {
			throw new HealthCheckException(addr.toString(), e1.getMessage(), e1);
		}
	}
}
