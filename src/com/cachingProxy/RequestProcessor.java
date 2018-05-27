package com.cachingProxy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import javax.imageio.ImageIO;

public class RequestProcessor implements Runnable {

	Socket clientSocket;
	BufferedReader clientBR;
	BufferedWriter clientBW;
	
	final String SUCCESS_MSG = "HTTP/1.1 200 OK\n" + "Proxy-agent: ProxyServer\n" + "\r\n";
	final String FAILURE_MSG = "HTTP/1.1 404 NOT FOUND\n" + "Proxy-agent: ProxyServer\n"
		+ "\r\n";
	
	public RequestProcessor(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			this.clientSocket.setSoTimeout(4000);
			clientBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientBW = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			
			String requestString = clientBR.readLine();
			System.out.println("Received Request: " + requestString);
			
			String requestType = requestString.substring(0, requestString.indexOf(' '));
			System.out.println("Request Type: " + requestType);

			String requestURL = requestString.substring(requestString.indexOf(' ') + 1);
			requestURL = requestURL.substring(0, requestURL.indexOf(' '));
			if (!requestURL.substring(0, 4).equals("http")) {
				String temp = "http://";
				requestURL = temp + requestURL;
			}
			System.out.println("Request URL: " + requestURL);
			
			readData(requestURL);
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void readData(String urlString) {
		try {
			int indexOfFileExtension = urlString.lastIndexOf(".");
			String fileExtension;
			fileExtension = urlString.substring(indexOfFileExtension, urlString.length());
			if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || fileExtension.contains(".jpeg")
				|| fileExtension.contains(".gif")) {
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if (image != null) {
//					ImageIO.write(image, fileExtension.substring(1), fileToCache);
					String line = SUCCESS_MSG;
					clientBW.write(line);
					clientBW.flush();
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
				} else {
					System.out.println("Sending 404 to client as image wasn't received from server" + remoteURL);
					String error = FAILURE_MSG;
					clientBW.write(error);
					clientBW.flush();
					return;
				}
			}

			else {

				URL remoteURL = new URL(urlString);
				HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
				BufferedReader proxyToServerBR = new BufferedReader(
				new InputStreamReader(proxyToServerCon.getInputStream()));

				String line = SUCCESS_MSG;
				clientBW.write(line);

				while ((line = proxyToServerBR.readLine()) != null) {
					clientBW.write(line);
				}

				clientBW.flush();
				if (proxyToServerBR != null) {
					proxyToServerBR.close();
				}
			}

			if (clientBW != null) {
				clientBW.close();
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	
	}

}