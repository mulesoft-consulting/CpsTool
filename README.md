# CpsTool
Encryption tool for configuration property service. This is a java command line tool to see help, run the command:

```
java -jar target/CpsTool.jar
```

Some examples of using the tool from a bash shell are:

## To pull and decrypt
curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" https://localhost:9084/configuration-property-service/v1/config/base/base/base/base/testkey | java -jar target/CpsTool.jar decrypt

## To pull and encrypt
curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" https://localhost:9084/configuration-property-service/v1/config/base/base/base/base/testkey | java -jar target/CpsTool.jar encrypt

## To pull, encrypt and push
curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" https://localhost:9084/configuration-property-service/v1/config/base/base/base/base/testkey | java -jar target/CpsTool.jar encrypt | curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" -H "edit_token:xx" -H "Content-Type:application/json" -X POST --data @-  https://localhost:9084/configuration-property-service/v1/config

## To pull, re-encrypt with new key and push
curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" https://localhost:9084/configuration-property-service/v1/config/base/base/base/base/testkey | java -jar target/CpsTool.jar re-encrypt testkey2 | curl -k -H "client_id:bc80c73ad5fb4f21b4fc620c443eecac" -H "client_secret:D2Df58ae2a0f46e6882A69D51463e411" -H "edit_token:xx" -H "Content-Type:application/json" -X POST --data @-  https://localhost:9084/configuration-property-service/v1/config

## To convert a property file to a config file
java -jar target/CpsTool.jar property-file myproject mybranch site01 local base <src/test/resources/test.properties

## Required environment variables for Keystore encryption/decryption
The encryption depends on keys which can be stored in a keystore file. The keys are RSA key pairs where the keystore alias is the keyId. To point to the correct keystore, set these variables:

```
  mule_cps_keystore_filename=path to keystore.jks file
  mule_cps_keystore_password=the keystore's password (defaults to "")
  mule_cps_key_password=the password for the keys (note that all the keys use the same password)
```

## Encrypting with a PEM file (containing a public key)
When the PEM options are used to encrypt a config, the public key is stored in a PEM file named <keyId>.pem the command push-file-encrypt-with-pem command uses the key stored in the PEM file to encrypt the properties.

The CpsTool command push-file-encrypt-with-pem when encrypting when the public key is stored in a PEM file.

Here is what a public key PEM file looks like:

```
-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAs3sn0rHQuaF0+Y2Fb0G0
WXxegRNeJZ5qfwpAWtJX0Gq1f4JrWvy6p5L2rUzP4ypVKeJL0+Nc81PmtO+fEyIu
ETi7PUyE/wviaOZlrMx6kdSBdgk/tBe5ETh+DmvluNYySISTszxQsbGdM4kAmzj/
H5awHzl1SXAGECKDUTue+FvmugPONOw8DkyxPyRHhoNdL2L4KKjO0VfwrSY8S5Dd
hS36sUMh5H9/kGL9psFY+SqEQiyiS5K1N0f9iionP15ankESfmtS6o1Hu8H19U5C
N14MT4knmjOG6RZ6HunQoRUc8zcbRLehXquTfDx+iX7/ljFGO7MbCOUwT2J4+v8A
mD4A/NPSBn5GmpYjlBbNT7Nu+ecQXt2SoXj3jYR5gHiU0UHxVYZk5ffTMnW/C2aA
HsrndFI92k4X6GBU0jcJgmqE7Bosl+JThoEzR75F04dcOuCPwsvOdcSnie9kImbp
Nv0UteaOvHWxA1ZmUTRyEepAKaQQhbVoxHTxEQuksHi/kEhpJPoqxGvFgPrQ50ue
ZSUcqnlPq3igYngiJNZ992cttnZl+zbGBB1o+yweVy7hLItKDSt2HwRqYT8uBOG9
eOcvsvvDtJVEzfe527kpeZ2RqcU9TGDL1913yiTXQysYxJoW20M1fjot5W4XBdW6
ptZXACRxNfHnStoSE/kZd40CAwEAAQ==
-----END PUBLIC KEY-----
```

## Decrypting with a PKCS8 file (containing a private key)
When the PEM option is used for decrypting a config, the private key is stored in a PKCS8 file named <keyId>.pkcs8 or in a system variable of the same name as the keyId. If found, the system variable will be used first, otherwise the file will be used.

The CpsTool command pull-decrypt-with-pem is used when the PKCS8 key file is to be used.

Here is what a private key PKCS8 file looks like:

```
-----BEGIN PRIVATE KEY-----
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
8bschWqSnuQYD0eBxoT7OC/epvYw0g==
-----END PRIVATE KEY-----
```

## Dependency
Note that the project cps-encryption contains the encryption library used by the tool.

## Testing with cps-encryption's keystore
If you setup the keystore environment variables with the keystore setup described in the cps-encryption project, you can test the encryption with the sample-cps-config.json file included in the project. Use the encrypt command:
 ```
 java -jar target/CpsTool.jar encrypt <sample-cps-config.json
 ```
 If the configuration property service is running, you can store the encrypted properties with this command:
 
 ```
 java -jar target/CpsTool.jar encrypt <sample-cps-config.json | curl -k -H "client_id:x" -H "client_secret:x" -H "edit_token:xx" -H "Content-Type:application/json" -X POST --data @-  http://localhost:9184/configuration-property-service/v1/config
 ```
The url host and port will need to reflect your configuration.
 
## Building CpsTool

```
mvn clean install
```