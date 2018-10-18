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

## Required environment variables
The encryption depends on keys stored in a keystore file. The keys are RSA key pairs where the keystore alias is the keyId. To point to the correct keystore, set these variables:

```
  mule_cps_keystore_filename=path to keystore.jks file
  mule_cps_keystore_password=the keystore's password (defaults to "")
  mule_cps_key_password=the password for the keys (note that all the keys use the same password)
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