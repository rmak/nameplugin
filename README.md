# nameplugin
introduce a hook to name mapping

CHANGES.txt
HADOOP-11683 Need a plugin API to translate long principal names to local OS user names arbitrarily

Changed files
CommonConfigurationKeysPublic.java
- added the new configuration parameter for user name mapping
CompositeUserNameMapping.java
- new class to support multiple user name mapping plugins
core-default.xml
- usage example
HadoopKerberosName.java
- the main modification for name mapping
KerberosName.java
- related class but not touched
TestCompositeUserNameMapping.java
- new test classs for CompositeUserNameMapping
UserNameMappingServiceProvider.java
- new base class for usr name mapping plugin

