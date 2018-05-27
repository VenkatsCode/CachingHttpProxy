package com.cachingProxy;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

public class CachingProxy {

	private ServerSocket serverSocket;
	private volatile boolean running = true;
	static HashMap<String, File> cacheData;
	static ArrayList<Thread> requestsInProgress;
	
	public CachingProxy(int port) {
		cacheData = new HashMap<>();
		requestsInProgress = new ArrayList<>();

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Waiting on port " + serverSocket.getLocalPort() + "..");
			running = true;
		} 
		catch (SocketException se) {
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			ste.printStackTrace();
		} 
		catch (IOException io) {
			io.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CachingProxy cahicngProxy = new CachingProxy(3128);
		cahicngProxy.openSocket();
	}
	
	public void openSocket(){
		while(running){
			try {
				Socket socket = serverSocket.accept();
				Thread thread = new Thread(new RequestProcessor(socket));
				requestsInProgress.add(thread);
				thread.start();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
