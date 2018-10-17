package com.mulesoft.java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mule.consulting.cps.encryption.CpsEncryptor;
import org.mule.consulting.cps.encryption.KeyStoreHelper;



import com.cps.restclient.RestClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;



public class CpsTool {
	
	private static Logger logger = LoggerFactory.getLogger(CpsTool.class);
	private static String verboseLogger = (StringUtils.isNotBlank(System.getenv("mule_cps_verbose_logging"))) 
											? (System.getenv("mule_cps_verbose_logging")) : "";
	
	public static void main(String[] args) {
		System.err.println("CpsTool version 1.2\n");
		try {
			if (args.length <= 0) {
				printHelp();
			} else if (args[0].equals("pretty")) {
				String data = IOUtils.toString(System.in, "UTF8");
				String json = pretty(data);
				System.out.println(json);
			} else if (args[0].equals("decrypt")) {
				String data = IOUtils.toString(System.in, "UTF8");
				String json = decrypt(data);
				System.out.println(json);
			} else if (args[0].equals("encrypt")) {
				String argKeyId = (args.length > 1) ? args[1] : null;
				String data = IOUtils.toString(System.in, "UTF8");
				String json = encrypt(argKeyId, data);
				System.out.println(json);
			} else if (args[0].equals("encrypt-with-pem")) {
				String argKeyId = (args.length > 1) ? args[1] : null;
				if (argKeyId == null) {
					String msg = "Need a keyId to be specified, cannot continue with encrypt";
					System.err.println(msg);
					throw new Exception(msg);
				}
				File pemFile = new File(argKeyId + ".pem");
				if (!pemFile.exists() || !pemFile.isFile()) {
					String msg = "Need the file " + pemFile.getAbsolutePath()
							+ " to be present, cannot continue with encrypt";
					System.err.println(msg);
					throw new Exception(msg);
				}
				InputStream is = FileUtils.openInputStream(pemFile);
				PublicKey publicKey = KeyStoreHelper.getPublicKeyFromPEM(is);
				String data = IOUtils.toString(System.in, "UTF8");
				String json = encrypt(publicKey, argKeyId, data);
				System.out.println(json);
			} else if (args[0].equals("re-encrypt")) {
				String argKeyId = (args.length > 1) ? args[1] : null;
				String data = IOUtils.toString(System.in, "UTF8");
				String jsonData = decrypt(data);
				String json = encrypt(argKeyId, jsonData);
				System.out.println(json);
			} else if (args[0].equals("property-file")) {
				Properties properties = new Properties();
				properties.load(System.in);
				String json = propertyFile(properties, (args.length > 1) ? args[1] : "",
						(args.length > 2) ? args[2] : "", (args.length > 3) ? args[3] : "",
						(args.length > 4) ? args[4] : "", (args.length > 5) ? args[5] : "");
				System.out.println(json);
			} else if (args[0].equals("push-file")) { 
				String editToken = (args.length > 2) ? args[2] : null;
				String fileName = (args.length > 1) ? args[1] : null;
				String argKeyId = (args.length > 3) ? args[3] : null;
				/* Check if fileName is provided*/
				if (fileName == null) {
					String msg = "Need a config json file to push to the CPS service";
					System.err.println(msg);
					throw new Exception(msg);
				}
				Properties config = getConfigProperties();
				/* Read file and call service*/
				pushConfigToCPSService(fileName, false, argKeyId, config, editToken);
			} else if (args[0].equals("push-file-encrypt")) { 
				String editToken = (args.length > 2) ? args[2] : null;
				String fileName = (args.length > 1) ? args[1] : null;
				String argKeyId = (args.length > 3) ? args[3] : null;
				/* Check if fileName is provided*/
				if (fileName == null) {
					String msg = "Need a config json file to push to the CPS service";
					System.err.println(msg);
					throw new Exception(msg);
				}
				Properties config = getConfigProperties();
				/* Read file and call service*/
				pushConfigToCPSService(fileName, true, argKeyId, config, editToken);
			} else if(args[0].equals("pull-decrypt")) {
				Properties config = getConfigProperties();
				pullAndDecryptConfig(args, config);
			} else {
				printHelp();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(500);
		}
	}
	
	/**
	 * This method will read the properties from the config.properties file
	 * @throws Exception 
	 * 
	 */
	private static Properties getConfigProperties() throws Exception {
		Properties configProperties = new Properties();
		
		Map<String, String> osEnvVars = System.getenv();
		String cps_host = (osEnvVars.get("mule_cps_host") != null)
				? osEnvVars.get("mule_cps_host") : System.getProperty("mule_cps_host", "");
				
		String cps_port = (osEnvVars.get("mule_cps_port") != null)
				? osEnvVars.get("mule_cps_port") : System.getProperty("mule_cps_port", "");
				
		String cps_path = (osEnvVars.get("mule_cps_path") != null) 
				? osEnvVars.get("mule_cps_path") : System.getProperty("mule_cps_path", "");
				
		String cps_client_id = (osEnvVars.get("mule_cps_client_id") != null) 
				? osEnvVars.get("mule_cps_client_id") : System.getProperty("mule_cps_client_id", "");
				
		String cps_client_secret = (osEnvVars.get("mule_cps_client_secret") != null) 
				? osEnvVars.get("mule_cps_client_secret") : System.getProperty("mule_cps_client_secret", "");
		
		String cps_pass_credentials_as_headers = (osEnvVars.get("mule_cps_pass_credentials_as_headers") != null) 
				? osEnvVars.get("mule_cps_pass_credentials_as_headers") : System.getProperty("mule_cps_pass_credentials_as_headers", "");
		
		if(StringUtils.equalsIgnoreCase(verboseLogger, "true")) {
			System.err.println("Using mule cps hosted on= " + cps_host);
		}
		if (cps_host.equals("")) {
			logger.warn("mule_cps_host is not specified");
			String msg = "Need mule_cps_host to be defined in environment variables";
			throw new Exception(msg); 
		} else {
			configProperties.setProperty("cps_host", cps_host);
		}
		if (cps_port.equals("")) {
			logger.warn("mule_cps_port is not specified");
			String msg = "Need mule_cps_port to be defined in environment variables";
			throw new Exception(msg); 
		} else {
			configProperties.setProperty("cps_port", cps_port);
		}
		if (cps_path.equals("")) {
			logger.warn("mule_cps_path is not specified");
			String msg = "Need mule_cps_path to be defined in environment variables";
			throw new Exception(msg); 
		} else {
			configProperties.setProperty("cps_path", cps_path);
		}
		if (cps_client_id.equals("")) {
			logger.warn("mule_cps_client_id is not specified");
			String msg = "Need mule_cps_client_id to be defined in environment variables";
			throw new Exception(msg); 
		} else {
			configProperties.setProperty("cps_client_id", cps_client_id);
		}
		if (cps_client_secret.equals("")) {
			logger.warn("mule_cps_client_secret is not specified");
			String msg = "Need mule_cps_client_secret to be defined in environment variables";
			throw new Exception(msg); 
		} else {
			configProperties.setProperty("cps_client_secret", cps_client_secret);
		}
		if(cps_pass_credentials_as_headers.equals("")) {
			logger.warn("cps_pass_credentials_as_headers is not specified");
			String msg = "Need mule_cps_pass_credentials_as_headers to be defined in environment variables";
			throw new Exception(msg); 
		}else {
			configProperties.setProperty("cps_pass_credentials_as_headers", cps_pass_credentials_as_headers);
		}
		
		return configProperties;
	}

	/**
	 * This method will check all arguments for input, if not provided
	 * user will be prompted for required input.
	 * CPS GET/config service will be called to get the required config
	 * @param args
	 */
	private static void pullAndDecryptConfig(String[] args, Properties config) {
		
		/*Initiate Scanner and Check all Inputs*/
		Scanner userInput = new Scanner(System.in);
		String projectName = (args.length > 1) ? args[1] : null;
		String branchName = (args.length > 2) ? args[2] : null;
		String instanceId = (args.length > 3) ? args[3] : null;
		String envName = (args.length > 4) ? args[4] : null;
		String keyId = (args.length > 5) ? args[5] : null;
		String editToken = (args.length > 6) ? args[6] : "99";
		StringBuilder resources = new StringBuilder();
		resources.append("/");
		
		if(StringUtils.isBlank(projectName)) {
			System.err.println("Please enter project name");
			resources.append(userInput.nextLine() + "/");
		} else {
			resources.append(projectName + "/");
		}
		
		if(StringUtils.isBlank(branchName)) {
			System.err.println("Please enter branch name");
			resources.append(userInput.nextLine() + "/");
		} else {
			resources.append(branchName + "/");
		}
		
		if(StringUtils.isBlank(instanceId)) {
			System.err.println("Please enter instance id");
			resources.append(userInput.nextLine() + "/");
		} else {
			resources.append(instanceId + "/");
		}
		
		if(StringUtils.isBlank(envName)) {
			System.err.println("Please enter environment name");
			resources.append(userInput.nextLine() + "/");
		} else {
			resources.append(envName + "/");
		}
		
		if(StringUtils.isBlank(keyId)) {
			System.err.println("Please enter key id");
			resources.append(userInput.nextLine());
		} else {
			resources.append(keyId);
		}
		/*Ask for totp security token*/
//		if(StringUtils.isBlank(editToken)) {
//			System.err.println("Please enter the edit token");
//			editToken = userInput.nextLine();
//		}
		userInput.close();
		
		/*Call CPS Service*/
		if(StringUtils.isNotBlank(resources.toString())) {
			try {
				callCpsGetConfigService(resources,editToken, config);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	/**
	 * This method will call the CPS GET/config service to retrieve
	 * and decrypt the required config file.
	 *  
	 * @param config
	 * @param uriParams
	 * @param token
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private static void callCpsGetConfigService(StringBuilder uriParams, String token, Properties config) throws URISyntaxException, IOException {

		/* Initialize HttpGet Call*/
		CloseableHttpClient httpclient = null;
		HttpGet httpGet = null;
		try {
			httpclient = RestClientUtil.getClient();
			httpGet = RestClientUtil.getHttpGetClient(uriParams, token, config);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(httpclient != null && httpGet != null) {
			if(StringUtils.equalsIgnoreCase(verboseLogger, "true")) {
				System.err.println("*********Calling the CPS Service URL********* -> " + httpGet.getURI());
			}
			try {
				CloseableHttpResponse response = httpclient.execute(httpGet);
				HttpEntity responseEntity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == 200) {
					String content = EntityUtils.toString(responseEntity);
					if(StringUtils.equalsIgnoreCase(verboseLogger, "true")) {
						System.err.println("*********Decrypting the properties*********");
					}
					String json = decrypt(content);
					System.out.println(json);
				} else {
					String content = EntityUtils.toString(responseEntity);
			        throw new Exception(content);
				}
				
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				httpclient.close();	
			}
		}
		
	}

	/**
	 * This method will post the config file to the CPS service.
	 * If encrypt = true, the secure.properites within the config will
	 * be encrypted before posting to the CPS service.
	 * 
	 * @param fileName
	 * @param encrypt
	 * @param argKeyId
	 * @param config
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private static void pushConfigToCPSService(String fileName, boolean encrypt, String argKeyId, Properties config, String editToken)
			throws URISyntaxException, UnsupportedEncodingException, IOException {
		File file = new File (fileName);
		Scanner userInput = null;
		String json = StringUtils.EMPTY;
		StringBuilder configFile = new StringBuilder();
		try {
			userInput = new Scanner(file);
			while (userInput.hasNextLine()) {
				configFile.append(userInput.nextLine());
				configFile.append("\n");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			userInput.close();
		}
		
		/*Input JSON*/
		json = configFile.toString();
		if(StringUtils.isBlank(editToken)) {
			System.err.println("Please enter the edit token");
			userInput = new Scanner(System.in);
			editToken = userInput.nextLine();
			userInput.close();		
		}
		/* Initialize HttpPost Call*/
		CloseableHttpClient httpclient = null;
		HttpPost httpPost = null;
		try {
			httpclient = RestClientUtil.getClient();
			httpPost = RestClientUtil.getHttpPostClient(editToken,config);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(httpclient != null && httpPost != null) {
			/*Check if Encrypt True*/
			if(encrypt) {
				try {
					 json = encrypt(argKeyId, json);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(StringUtils.equalsIgnoreCase(verboseLogger, "true")) {
				System.err.println("*********Input JSON*********" + json);
			}
			/* Generate request body and set it in the request*/
			StringEntity configJsonEntity = new StringEntity(json);
			httpPost.setEntity(configJsonEntity);
			
			if(StringUtils.equalsIgnoreCase(verboseLogger, "true")) {
				System.err.println("*********Calling the CPS Service URL********** -> " + httpPost.getURI());
			}
			try {
				CloseableHttpResponse response = httpclient.execute(httpPost);
				if (response.getStatusLine().getStatusCode() == 200) {
					System.err.println("*********Update Successful. Please check /navigate resource of CPS Service*********");
				} else {
					HttpEntity responseEntity = response.getEntity();
					String content = EntityUtils.toString(responseEntity);
			        System.err.println(content);
				}
				
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} finally {
				httpclient.close();	
			}
		}
	}

	private static String encrypt(String argKeyId, String data) throws Exception {
		Map<String, Object> payload;
		ObjectMapper mapper;
		TypeFactory factory;
		MapType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructMapType(LinkedHashMap.class, String.class, Object.class);
		mapper = new ObjectMapper();
		payload = mapper.readValue(data, type);

		/* Start determining keyId */
		String keyId = (String) payload.get("keyId");
		String cipherKey = (String) payload.get("cipherKey");
		if (keyId == null || keyId.isEmpty()) {
			keyId = argKeyId;
		}
		if (keyId == null || keyId.isEmpty()) {
			String msg = "Need a keyId to be specified in the configuration file, cannot continue with encrypt";
			System.err.println(msg);
			throw new Exception(msg);
		}

		Map<String, String> properties = (Map<String, String>) payload.get("properties");
		boolean priorEncryptions = false;
		for (String key : properties.keySet()) {
			String value = properties.get(key);
			if (value.startsWith("![")) {
				priorEncryptions = true;
			}
		}
		if (!priorEncryptions && argKeyId != null) {
			keyId = argKeyId;
			payload.put("keyId", keyId);
		}
		/* End determining keyId */

		/* Start determining cipherKey */
		boolean newCipherKeyGenerated = false;
		CpsEncryptor cpsEncryptor = null;
		if (cipherKey == null || cipherKey.isEmpty()) {
			String msg = "Generating a new cipherKey for encryption";
			System.err.println(msg);
			cpsEncryptor = new CpsEncryptor(keyId);
			cipherKey = cpsEncryptor.getCpsKey().getCipherKey();
			newCipherKeyGenerated = true;
		} else {
			cpsEncryptor = new CpsEncryptor(keyId, cipherKey);
		}
		/* End determining cipherKey */

		String securePropertyList = properties.get("secure.properties");
		if (securePropertyList == null || securePropertyList.isEmpty()) {
			/* Nothing to do */
			System.err.println("No properties listed in secure.properties");
		} else {
			String[] secureProperties = securePropertyList.split(",");
			boolean canEncrypt = true;
			for (String key : secureProperties) {
				String plainTextValue = properties.get(key.trim());
				if (cpsEncryptor.isEncrypted(plainTextValue)) {
					System.err.println(key.trim() + " is already encrypted.");
					canEncrypt = (newCipherKeyGenerated) ? false : true;
				}
			}

			if (!canEncrypt) {
				String msg = "Input configuration file has problems with prior encryptions, cannot continue the encrypt";
				System.err.println(msg);
				throw new Exception(msg);
			}

			boolean propertiesChanged = false;
			for (String key : secureProperties) {
				String plainTextValue = properties.get(key.trim());
				String value = cpsEncryptor.encrypt(plainTextValue);
				properties.put(key, value);
				propertiesChanged = true;
			}
			if (propertiesChanged) {
				if (newCipherKeyGenerated) {
					payload.put("cipherKey", cipherKey);
				}
				payload.put("properties", properties);
			} else {
				System.err.println("No properties encrypted");
			}
		}

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload);
	}

	@SuppressWarnings("unchecked")
	private static String encrypt(PublicKey publicKey, String keyId, String data) throws Exception {
		Map<String, Object> payload;
		ObjectMapper mapper;
		TypeFactory factory;
		MapType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructMapType(LinkedHashMap.class, String.class, Object.class);
		mapper = new ObjectMapper();
		payload = mapper.readValue(data, type);

		/* Start determining keyId */
		payload.put("keyId", keyId);
		String cipherKey = (String) payload.get("cipherKey");
		if (cipherKey != null && !cipherKey.isEmpty()) {
			String msg = "Data is already encrypted, decrypt it before using public key encrypt";
			System.err.println(msg);
			throw new Exception(msg);
		}

		Map<String, String> properties = (Map<String, String>) payload.get("properties");
		for (String key : properties.keySet()) {
			String value = properties.get(key);
			if (value.startsWith("![")) {
				String msg = "Data is already encrypted, decrypt it before using public key encrypt";
				System.err.println(msg);
				throw new Exception(msg);
			}
		}
		/* End determining keyId */

		/* Start determining cipherKey */
		CpsEncryptor cpsEncryptor = new CpsEncryptor(publicKey);
		cipherKey = cpsEncryptor.getCpsKey().getCipherKey();
		/* End determining cipherKey */

		String securePropertyList = properties.get("secure.properties");
		if (securePropertyList == null || securePropertyList.isEmpty()) {
			/* Nothing to do */
			System.err.println("No properties listed in secure.properties");
		} else {
			String[] secureProperties = securePropertyList.split(",");
			for (String key : secureProperties) {
				String plainTextValue = properties.get(key.trim());
				if (cpsEncryptor.isEncrypted(plainTextValue)) {
					System.err.println(key.trim() + " is already encrypted.");
				}
			}

			boolean propertiesChanged = false;
			for (String key : secureProperties) {
				String plainTextValue = properties.get(key.trim());
				String value = cpsEncryptor.encrypt(plainTextValue);
				properties.put(key, value);
				propertiesChanged = true;
			}
			if (propertiesChanged) {
				payload.put("cipherKey", cipherKey);
				payload.put("properties", properties);
			} else {
				System.err.println("No properties encrypted");
			}
		}

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload);
	}

	private static String decrypt(String data) throws Exception {
		Map<String, Object> payload;
		ObjectMapper mapper;
		TypeFactory factory;
		MapType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructMapType(LinkedHashMap.class, String.class, Object.class);
		mapper = new ObjectMapper();
		payload = mapper.readValue(data, type);

		String keyId = (String) payload.get("keyId");
		String cipherKey = (String) payload.get("cipherKey");
		if (keyId == null || keyId.isEmpty()) {
			String msg = "Need a keyId to be specified in the configuration file, cannot continue with decrypt";
			System.err.println(msg);
			throw new Exception(msg);
		}

		Map<String, String> properties = (Map<String, String>) payload.get("properties");
		boolean priorEncryptions = false;
		for (String key : properties.keySet()) {
			String value = properties.get(key);
			if (value.startsWith("![")) {
				priorEncryptions = true;
			}
		}
		if (!priorEncryptions) {
			return data;
		}

		if (cipherKey == null || cipherKey.isEmpty()) {
			String msg = "Need a cipherKey to be specified in the configuration file, cannot continue with decrypt";
			System.err.println(msg);
			throw new Exception(msg);
		}

		CpsEncryptor cpsEncryptor = new CpsEncryptor(keyId, cipherKey);
		for (String key : properties.keySet()) {
			String encryptedValue = properties.get(key);
			String value = cpsEncryptor.decrypt(encryptedValue);
			properties.put(key, value);
		}
		payload.remove("cipherKey");

		return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(payload);
	}

	private static String propertyFile(Properties properties, String projectName, String branchName, String instanceId,
			String envName, String keyId) throws Exception {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();

		payload.put("projectName", projectName);
		payload.put("branchName", branchName);
		payload.put("instanceId", instanceId);
		payload.put("envName", envName);
		payload.put("keyId", keyId);

		ArrayList<Object> empty = new ArrayList<Object>();
		payload.put("imports", empty.toArray());
		for (Object k : properties.keySet()) {
			String key = (String) k;
			String value = properties.getProperty(key);
			map.put(key, value);
		}
		payload.put("properties", map);

		ObjectMapper mapperw = new ObjectMapper();
		String result = mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
		return result;
	}

	private static String pretty(String data) throws Exception {
		Map<String, Object> payload;
		ObjectMapper mapper;
		TypeFactory factory;
		MapType type;

		factory = TypeFactory.defaultInstance();
		type = factory.constructMapType(LinkedHashMap.class, String.class, Object.class);
		mapper = new ObjectMapper();
		payload = mapper.readValue(data, type);

		ObjectMapper mapperw = new ObjectMapper();
		String result = mapperw.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
		return result;
	}

	private static void printHelp() {
		System.err.println("\nUsage: java -jar CpsTool {operation} [parameters]\n");
		System.err.println("  operations:");
		
		
		System.err.println("    decrypt           Read stdin and decrypt to stdout");
		
		
		System.err.println("\n    encrypt-with-pem  Read stdin and encrypt to stdout using <keyId>.pem file containing the public key");
		System.err.println("      parameters:");
		System.err.println(
				"        keyId         The keyId to use for encrypting the data and for providing the name of the pem file.");
		
		
		System.err.println("\n    encrypt           Read stdin and encrypt to stdout using configured keystore");
		System.err.println("      parameters:");
		System.err.println(
				"        keyId         The keyId to use for encrypting data, will be ignored if the file contains encrypted data");
		
		
		System.err.println("\n    re-encrypt         Read stdin and re-encrypt with new key to stdout");
		System.err.println("      parameters:");
		System.err.println("        keyId         The keyId to use for new encryption (required)");
		
		
		System.err.println("\n    pretty            Read stdin and send a pretty version to stdout");
		
		System.err.println("\n    property-file     Read a property file from stdin and print config to stdout");
		System.err.println("      parameters:");
		System.err.println(
				"        projectName   The project name for the config coordinate key.");
		System.err.println(
				"        branchName    The branch name for the config coordinate key.");
		System.err.println(
				"        instanceId    The instance id for the config coordinate key.");
		System.err.println(
				"        envName       The deployment environment name for the config coordinate key.");
		System.err.println(
				"        keyId         The encryption keyId for the config coordinate key.");
		
		
		System.err.println("\n    push-file         Read a config json provided in the arguments and post it to CPS service");
		System.err.println("      parameters:");
		System.err.println(
				"        fileName   The file containing the config to push.");
		System.err.println(
				"        edit_token    The two-factor token from the authenticator device.");
		
		
		System.err.println("\n    push-file-encrypt Read a config json provided in the arguments, encrypt and post it to CPS service");
		System.err.println("      parameters:");
		System.err.println(
				"        fileName   The file containing the config to push.");
		System.err.println(
				"        edit_token    The two-factor token from the authenticator device.");
		
		
		System.err.println("\n    pull-decrypt      Retrieve and decrypt a config json file from the CPS service");
		System.err.println("      parameters:");
		System.err.println(
				"        projectName   The project name for the config coordinate key.");
		System.err.println(
				"        branchName    The branch name for the config coordinate key.");
		System.err.println(
				"        instanceId    The instance id for the config coordinate key.");
		System.err.println(
				"        envName       The deployment environment name for the config coordinate key.");
		System.err.println(
				"        keyId         The encryption keyId for the config coordinate key.");
		System.err.println("\n");
	}
}
