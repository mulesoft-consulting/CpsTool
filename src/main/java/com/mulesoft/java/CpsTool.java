package com.mulesoft.java;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.mule.consulting.cps.encryption.CpsEncryptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class CpsTool {

	public static void main(String[] args) {
		System.err.println("CpsTool version 1.1\n");
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
			} else if (args[0].equals("re-encrypt")) {
				String argKeyId = (args.length > 1) ? args[1] : null;
				String data = IOUtils.toString(System.in, "UTF8");
				String jsonData = decrypt(data);
				String json = encrypt(argKeyId, jsonData);
				System.out.println(json);
			} else if (args[0].equals("property-file")) {
				Properties properties = new Properties();
				properties.load(System.in);
				String json = propertyFile(properties, (args.length>1)?args[1]:"", (args.length>2)?args[2]:"", 
						(args.length>3)?args[3]:"", (args.length>4)?args[4]:"", 
								(args.length>5)?args[5]:"");
				System.out.println(json);
			} else {
				printHelp();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(500);
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
		System.out.println("\nUsage: java -jar CpsTool {operation} [parameters]\n");
		System.out.println("  operations:");
		System.out.println("    decrypt           Read stdin and decrypt to stdout");
		System.out.println("    encrypt           Read stdin and encrypt to stdout");
		System.out.println("      parameters:");
		System.out.println(
				"        keyId         The keyId to use for unencrypted file, will be ignored if the file contains encrypted data");
		System.out.println("    re-encrypt         Read stdin and re-encrypt with new key to stdout");
		System.out.println("      parameters:");
		System.out.println("        keyId         The keyId to use for new encryption (required)");
		System.out.println("    pretty            Read stdin and send a pretty version to stdout");
		System.out.println("    property-file     Read a property file from stdin and print config to stdout");
		System.out.println("\n");
	}
}
