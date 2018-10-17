set mule_cps_keystore_filename=/usr/local/cps-keystore.jks
set mule_cps_keystore_password=changeit
set mule_cps_key_password=changeit
set mule_cps_host=localhost
set mule_cps_port=9085
set mule_cps_path=/configuration-property-service/v1/config
set mule_cps_client_id=x
set mule_cps_client_secret=x
set mule_cps_pass_credentials_as_headers=true
set mule_cps_verbose_logging=false
set CPSTOOL_HOME=C:\CpsTool

java -jar %CPSTOOL_HOME%\CpsTool.jar %*
