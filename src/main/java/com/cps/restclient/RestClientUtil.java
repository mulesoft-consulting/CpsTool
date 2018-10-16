/**
 * 
 */
package com.cps.restclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author parthjogi
 *
 */
public class RestClientUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(RestClientUtil.class);
	
	public static CloseableHttpClient getClient() throws Exception {
		logger.warn("Using Insecure SSL configuration to connect to configuration property service.");
		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustAllStrategy()).build();
        HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(
        		sslSocketFactory).build();
		
		return httpclient;
		
	}

	/**
	 * @param uriParams
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public static HttpGet getHttpGetClient(StringBuilder uriParams, String token, Properties config) throws Exception {

		HttpGet httpGet = null;
		if(config != null && !config.isEmpty()) {
			/* Build URL */
			URI cpsServiceURL = getURI(uriParams, config);
			
			httpGet= new HttpGet(cpsServiceURL);
			/* Set Headers*/
			if(StringUtils.equalsIgnoreCase(config.getProperty("cps_pass_credentials_as_headers"), "false")) {
				httpGet.addHeader("Authorization", getAuthHeader(config).toString());
			} else {
				httpGet.addHeader("client_id", config.getProperty("cps_client_id"));
				httpGet.addHeader("client_secret", config.getProperty("cps_client_secret"));
			}
			httpGet.addHeader("Content-Type", "application/json");
			httpGet.addHeader("edit_token", token);
		}
		return httpGet;
	}

	/**
	 * @param config
	 * @return
	 */
	private static StringBuilder getAuthHeader(Properties config) {
		StringBuilder aheader = new StringBuilder();
		aheader.append(config.getProperty("cps_client_id")).append(":")
		       .append(config.getProperty("cps_client_secret"));
		String authString = Base64.getEncoder().encodeToString(aheader.toString().getBytes());
		aheader = new StringBuilder();
		aheader.append("Basic ").append(authString);
		
		return aheader;
	}

	/**
	 * @param uriParams
	 * @param config
	 * @return
	 * @throws URISyntaxException
	 */
	private static URI getURI(StringBuilder uriParams, Properties config) throws URISyntaxException {
		URIBuilder builder = new URIBuilder();
		builder.setScheme("https");
		builder.setHost(config.getProperty("cps_host"));
		builder.setPort(Integer.parseInt(config.getProperty("cps_port")));
		if(uriParams != null && StringUtils.isNotBlank(uriParams.toString())) {
			builder.setPath(config.getProperty("cps_path")+uriParams.toString());
		} else {
			builder.setPath(config.getProperty("cps_path"));
		}
		URI cpsServiceURL = builder.build();
		return cpsServiceURL;
	}

	/**
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public static HttpPost getHttpPostClient(String token, Properties config) throws Exception {
		/*Get Properties*/
		HttpPost httpPost = null;
		if(config != null && !config.isEmpty()) {
			/* Build URL */
			URI cpsServiceURL = getURI(null, config);
			httpPost = new HttpPost(cpsServiceURL);
			/* Set Headers*/
			if(StringUtils.equalsIgnoreCase(config.getProperty("cps_pass_credentials_as_headers"), "false")) {
				httpPost.addHeader("Authorization", getAuthHeader(config).toString());
			} else {
				httpPost.addHeader("client_id", config.getProperty("cps_client_id"));
				httpPost.addHeader("client_secret", config.getProperty("cps_client_secret"));
			}
			httpPost.addHeader("Content-Type", "application/json");
			httpPost.addHeader("edit_token", token);
		}
		return httpPost;
	}

}
