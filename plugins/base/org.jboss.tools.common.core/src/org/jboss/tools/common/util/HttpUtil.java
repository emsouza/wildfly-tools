package org.jboss.tools.common.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.jboss.tools.common.core.CommonCorePlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class HttpUtil {

	private static IProxyService proxyService;

	@Deprecated
	public static InputStream getInputStreamFromUrlByGetMethod(String url) throws IOException {
		return getInputStreamFromUrlByGetMethod(url, false);
	}

	@Deprecated
	public static InputStream getInputStreamFromUrlByGetMethod(String url, boolean checkStatusCode) throws IOException {
		HttpURLConnection connection = executeGetMethod(url);
		if (connection != null) {
			int statusCode = connection.getResponseCode();
			if (!checkStatusCode || statusCode == HttpURLConnection.HTTP_OK) {
				return connection.getInputStream();
			}
		}
		return null;
	}

	public static InputStreamReader getInputStreamReader(String httpUrl, int timeout) throws IOException {
		HttpURLConnection connection = createHttpURLConnection(httpUrl, timeout);
		if (connection != null) {
			return getInputStreamReader(connection);
		}
		return null;
	}

	public static InputStreamReader getInputStreamReader(HttpURLConnection connection) throws IOException {
		return getInputStreamReader(connection, 0);
	}

	public static HttpURLConnection createHttpURLConnection(String urlString, int timeout) throws IOException {
		URL url = new URL(urlString);
		URLConnection connetion = url.openConnection();
		HttpURLConnection httpConnection = null;
		if (connetion instanceof HttpURLConnection) {
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setInstanceFollowRedirects(true);
			httpConnection.setRequestMethod("GET");
			httpConnection.setConnectTimeout(timeout);
		}
		return httpConnection;
	}

	private static InputStreamReader getInputStreamReader(HttpURLConnection connection, int redirectAttemptCount) throws IOException {
		InputStreamReader responseReader = null;
		connection.connect();
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			String contentTypeCharset = null;
			String contentType = connection.getContentType();
			if (contentType != null) {
				Matcher matcher = Pattern.compile("charset=(.+)").matcher(contentType);
				if (matcher.find()) {
					contentTypeCharset = matcher.group(1);
				}
			}
			InputStream inputStream = connection.getInputStream();
			if (contentTypeCharset != null && contentTypeCharset.length() > 0) {
				responseReader = new InputStreamReader(new BufferedInputStream(inputStream), contentTypeCharset);
			} else {
				responseReader = new InputStreamReader(new BufferedInputStream(inputStream));
			}
		} else if (responseCode >= 300 && responseCode < 400) {
			String redirectLocation = connection.getHeaderField("location");
			if (redirectLocation != null && !connection.getURL().toString().equalsIgnoreCase(redirectLocation.trim())) {
				redirectAttemptCount++;
				if (redirectAttemptCount > 1) {
					HttpURLConnection redirectConnection = createHttpURLConnection(redirectLocation, connection.getConnectTimeout());
					redirectConnection.setIfModifiedSince(connection.getIfModifiedSince());
					return getInputStreamReader(redirectConnection, redirectAttemptCount);
				}
			}
		}
		return responseReader;
	}

	public static InputStream getInputStreamFromUrlByGetMethod(String url, IProxyService proxyService) throws IOException {
		return executeGetMethod(url, proxyService).getInputStream();
	}

	public static int getStatusCodeFromUrlByGetMethod(String url, IProxyService proxyService) throws IOException {
		return executeGetMethod(url, proxyService).getResponseCode();
	}

	public static int getStatusCodeFromUrlByGetMethod(String url) throws IOException {
		return executeGetMethod(url).getResponseCode();
	}

	public static InputStream getInputStreamFromUrlByPostMethod(String url) throws IOException {
		return executePostMethod(url).getInputStream();
	}

	public static int getStatusCodeFromUrlByPostMethod(String url, IProxyService proxyService) throws IOException {
		return executePostMethod(url, proxyService).getResponseCode();
	}

	public static int getStatusCodeFromUrlByPostMethod(String url) throws IOException {
		return executePostMethod(url).getResponseCode();
	}

	private static HttpURLConnection executeGetMethod(String url) throws IOException {
		IProxyService proxyService = getProxyService();
		return executeGetMethod(url, proxyService);
	}

	private static HttpURLConnection executePostMethod(String url) throws IOException {
		IProxyService proxyService = getProxyService();
		return executePostMethod(url, proxyService);
	}

	public static HttpURLConnection executeGetMethod(String url, IProxyService proxyService) throws IOException {
		HttpURLConnection connection = openConnection(url, proxyService);
		connection.setRequestMethod("GET");
		connection.setDoInput(true);
		connection.connect();
		return connection;
	}

	private static HttpURLConnection executePostMethod(String url, IProxyService proxyService) throws IOException {
		HttpURLConnection connection = openConnection(url, proxyService);
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.connect();
		return connection;
	}

	private static HttpURLConnection openConnection(String urlString, IProxyService proxyService) throws IOException {
		URL url = new URL(urlString);
		Proxy proxy = null;
		String proxyAuth = null;

		if (proxyService != null && proxyService.isProxiesEnabled()) {
			IProxyData[] proxyData = proxyService.getProxyData();
			String hostName = url.getHost();
			String[] nonProxiedHosts = proxyService.getNonProxiedHosts();
			boolean nonProxiedHost = false;
			for (int i = 0; i < nonProxiedHosts.length; i++) {
				if (nonProxiedHosts[i].equalsIgnoreCase(hostName)) {
					nonProxiedHost = true;
					break;
				}
			}
			if (!nonProxiedHost) {
				for (int i = 0; i < proxyData.length; i++) {
					IProxyData data = proxyData[i];
					if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType())) {
						String proxyHost = data.getHost();
						if (proxyHost == null) break;
						int port = data.getPort();
						if (port == -1) port = 80;
						proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
						if (data.isRequiresAuthentication()) {
							String user = data.getUserId();
							if (user != null) {
								String password = data.getPassword();
								String creds = user + ":" + (password != null ? password : "");
								proxyAuth = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes("UTF-8"));
							}
						}
						break;
					}
				}
			}
		}

		HttpURLConnection connection;
		if (proxy != null) {
			connection = (HttpURLConnection) url.openConnection(proxy);
		} else {
			connection = (HttpURLConnection) url.openConnection();
		}
		connection.setConnectTimeout(30000);
		if (proxyAuth != null) {
			connection.setRequestProperty("Proxy-Authorization", proxyAuth);
		}
		return connection;
	}

	private static IProxyService getProxyService() {
		if (proxyService == null) {
			BundleContext bundleContext = CommonCorePlugin.getInstance().getBundle().getBundleContext();
			ServiceReference serviceReference = bundleContext.getServiceReference(IProxyService.class.getName());
			proxyService = (IProxyService) bundleContext.getService(serviceReference);
		}
		return proxyService;
	}
}
