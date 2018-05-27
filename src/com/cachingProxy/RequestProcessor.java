package com.cachingProxy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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

	final String SUCCESS_MSG_FROM_CAHCHE = "HTTP/1.1 200 OK\n"
			+ "Proxy-agent: ProxyServer\n" + "X-CACHE: HIT\n" + "\r\n";
	final String FAILURE_MSG_FROM_CAHCHE = "HTTP/1.1 404 NOT FOUND\n"
			+ "Proxy-agent: ProxyServer\n" + "X-CACHE: HIT\n" + "\r\n";
	final String SUCCESS_MSG_FROM_URL = "HTTP/1.1 200 OK\n"
			+ "Proxy-agent: ProxyServer\n" + "X-CACHE: MISS\n" + "\r\n";
	final String FAILURE_MSG_FROM_URL = "HTTP/1.1 404 NOT FOUND\n"
			+ "Proxy-agent: ProxyServer\n" + "X-CACHE: HIT\n" + "\r\n";

	public RequestProcessor(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			this.clientSocket.setSoTimeout(4000);
			clientBR = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
			clientBW = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {

			String requestString = clientBR.readLine();
			System.out.println("Received Request: " + requestString);

			String requestType = requestString.substring(0,
					requestString.indexOf(' '));
			System.out.println("Request Type: " + requestType);

			String requestURL = requestString.substring(requestString
					.indexOf(' ') + 1);
			requestURL = requestURL.substring(0, requestURL.indexOf(' '));
			if (!requestURL.substring(0, 4).equals("http")) {
				String temp = "http://";
				requestURL = temp + requestURL;
			}
			System.out.println("Request URL: " + requestURL);

			File file;
			if ((file = CachingProxy.getCachedData(requestURL)) != null) {
				System.out.println("Cache found for : " + requestURL);
				readDataFromCache(file);
			} else {
				System.out.println("Cache not found for : " + requestURL
						+ ", hence performing HTTP request");
				readDataFromURL(requestURL);
			}

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void readDataFromURL(String urlString) {

		try {

			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;
			fileExtension = urlString.substring(fileExtensionIndex,
					urlString.length());
			String fileName = urlString.substring(0, fileExtensionIndex);
			fileName = fileName.substring(fileName.indexOf('.') + 1);
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.', '_');
			if (fileExtension.contains("/")) {
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.', '_');
				fileExtension += ".html";
			}
			fileName = fileName + fileExtension;
			System.out.println("file name: " + fileName);

			boolean caching = true;
			File fileToCache = null;
			BufferedWriter fileToCacheBW = null;
			try {
				File directory = new File("cached");
				if (!directory.exists()) {
					directory.mkdir();
				}
				fileToCache = new File(directory + "/" + fileName);
				if (!fileToCache.exists()) {
					fileToCache.createNewFile();
				}
				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			} catch (IOException e) {
				caching = false;
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}

			if ((fileExtension.contains(".png"))
					|| fileExtension.contains(".jpg")
					|| fileExtension.contains(".jpeg")
					|| fileExtension.contains(".gif")) {
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if (image != null) {
					ImageIO.write(image, fileExtension.substring(1),
							fileToCache);
					String line = SUCCESS_MSG_FROM_URL;
					clientBW.write(line);
					clientBW.flush();
					ImageIO.write(image, fileExtension.substring(1),
							clientSocket.getOutputStream());
				} else {
					System.out
							.println("Sending 404 to client as image wasn't received from server"
									+ fileName);
					String error = FAILURE_MSG_FROM_URL;
					clientBW.write(error);
					clientBW.flush();
					return;
				}
			}

			else {

				URL remoteURL = new URL(urlString);
				HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL
						.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				proxyToServerCon
						.setRequestProperty("Content-Language", "en-US");
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
				BufferedReader proxyToServerBR = new BufferedReader(
						new InputStreamReader(proxyToServerCon.getInputStream()));

				// Send success code to client
				String line = SUCCESS_MSG_FROM_URL;
				clientBW.write(line);

				while ((line = proxyToServerBR.readLine()) != null) {
					clientBW.write(line);
					if (caching) {
						fileToCacheBW.write(line);
					}
				}

				clientBW.flush();
				if (proxyToServerBR != null) {
					proxyToServerBR.close();
				}
			}

			if (caching) {
				fileToCacheBW.flush();
				CachingProxy.addToCache(urlString, fileToCache);
			}

			if (fileToCacheBW != null) {
				fileToCacheBW.close();
			}

			if (clientBW != null) {
				clientBW.close();
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readDataFromCache(File cachedFile) {
		try {
			String fileExtension = cachedFile.getName().substring(
					cachedFile.getName().lastIndexOf('.'));

			// Response that will be sent to the server
			String response;
			if ((fileExtension.contains(".png"))
					|| fileExtension.contains(".jpg")
					|| fileExtension.contains(".jpeg")
					|| fileExtension.contains(".gif")) {
				BufferedImage image = ImageIO.read(cachedFile);
				if (image == null) {
					System.out.println("Image " + cachedFile.getName()
							+ " was null");
					response = FAILURE_MSG_FROM_CAHCHE;
					clientBW.write(response);
					clientBW.flush();
				} else {
					response = SUCCESS_MSG_FROM_CAHCHE;
					clientBW.write(response);
					clientBW.flush();
					ImageIO.write(image, fileExtension.substring(1),
							clientSocket.getOutputStream());
				}
			}

			else {
				BufferedReader cachedFileBufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(cachedFile)));
				response = SUCCESS_MSG_FROM_CAHCHE;
				clientBW.write(response);
				clientBW.flush();

				String line;
				while ((line = cachedFileBufferedReader.readLine()) != null) {
					clientBW.write(line);
				}
				clientBW.flush();
				if (cachedFileBufferedReader != null) {
					cachedFileBufferedReader.close();
				}
			}
			if (clientBW != null) {
				clientBW.close();
			}
		} catch (IOException e) {
			System.out.println("Error Sending Cached file to client");
			e.printStackTrace();
		}
	}

}