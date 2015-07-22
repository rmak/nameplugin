/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.security;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.junit.Test;


public class TestCompositeUserNameMapping {
  public static final Log LOG = LogFactory.getLog(TestCompositeUserNameMapping.class);
  private static Configuration conf = new Configuration();
  
  private static class TestUser {
    String name;
    String shortName;
    
    public TestUser(String name, String shortName) {
      this.name = name;
      this.shortName = shortName;
    }
  };
  
  private static TestUser joe = new TestUser("nobody@AD.COM", "joe");
  private static TestUser john = new TestUser("JackJohn@BC.COM", "john");
  private static TestUser jack = new TestUser("JackJohn@BC.COM", "jack");
  private static TestUser hdfs = new TestUser("cluster1hdfs/host@AD.COM", "hdfs");
  
  private static final String PROVIDER_SPECIFIC_CONF = ".test.prop";
  private static final String PROVIDER_SPECIFIC_CONF_KEY = 
    UserNameMappingServiceProvider.USER_NAME_MAPPING_CONFIG_PREFIX + PROVIDER_SPECIFIC_CONF;
  private static final String PROVIDER_SPECIFIC_CONF_VALUE_FOR_A = "value-for-A";
  private static final String PROVIDER_SPECIFIC_CONF_VALUE_FOR_B = "value-for-B";
  
  private static abstract class UserNameMappingProviderBase 
    implements UserNameMappingServiceProvider, Configurable {

    private Configuration conf;
    
    @Override
    public void setConf(Configuration conf) {
      this.conf = conf;
    }

    @Override
    public Configuration getConf() {
      return this.conf;
    }

    @Override
    public void cacheUserNameRefresh() throws IOException {
      
    }

    @Override
    public void cacheUserNameAdd(List<String> user) throws IOException {
      
    }
    
    protected void checkTestConf(String expectedValue) {
      String configValue = getConf().get(PROVIDER_SPECIFIC_CONF_KEY);
      if (configValue == null || !configValue.equals(expectedValue)) {
        throw new RuntimeException("Failed to find mandatory configuration of " + PROVIDER_SPECIFIC_CONF_KEY);
      }
    }
  };
  
  private static class UserNameProviderA extends UserNameMappingProviderBase {
    @Override
    public String getShortName(String user) throws IOException {
      checkTestConf(PROVIDER_SPECIFIC_CONF_VALUE_FOR_A);
      
      String shortName = null;
      if (user.equals(john.name)) {
        shortName = john.shortName;
      } else if (user.equals(joe.name)) {
        shortName = joe.shortName;
      }
      
      return shortName;
    }
  }
  
  private static class UserNameProviderB extends UserNameMappingProviderBase {    
    @Override
    public String getShortName(String user) throws IOException {
      checkTestConf(PROVIDER_SPECIFIC_CONF_VALUE_FOR_B);
      
      String shortName = null;
      if (user.equals(hdfs.name)) {
        shortName = hdfs.shortName;
      } else if (user.equals(jack.name)) { // jack has another shortName from clusterProvider
        shortName = jack.shortName;
      }
      
      return shortName;
    }
  }
  
  static {
    conf.setClass(CommonConfigurationKeys.HADOOP_SECURITY_USER_NAME_MAPPING,
      CompositeUserNameMapping.class, UserNameMappingServiceProvider.class);
    
    conf.set(CompositeUserNameMapping.MAPPING_PROVIDERS_CONFIG_KEY, "UserNameProviderA,UserNameProviderB");

    conf.setClass(CompositeUserNameMapping.MAPPING_PROVIDER_CONFIG_PREFIX + ".UserNameProviderA", 
      UserNameProviderA.class, UserNameMappingServiceProvider.class);

    conf.setClass(CompositeUserNameMapping.MAPPING_PROVIDER_CONFIG_PREFIX + ".UserNameProviderB", 
      UserNameProviderB.class, UserNameMappingServiceProvider.class);

    conf.set(CompositeUserNameMapping.MAPPING_PROVIDER_CONFIG_PREFIX + 
      ".UserNameProviderA" + PROVIDER_SPECIFIC_CONF, PROVIDER_SPECIFIC_CONF_VALUE_FOR_A);

    conf.set(CompositeUserNameMapping.MAPPING_PROVIDER_CONFIG_PREFIX + 
      ".UserNameProviderB" + PROVIDER_SPECIFIC_CONF, PROVIDER_SPECIFIC_CONF_VALUE_FOR_B);
  }

  @Test
  public void TestMultipleUserNameMapping() throws Exception {
    HadoopKerberosName.setConfiguration(conf);
    HadoopKerberosName joename = new HadoopKerberosName(joe.name);
    System.out.println("Name: " + joe.name + " to " + joename.getShortName());
    assertTrue(joename.getShortName().equals(joe.shortName));
    HadoopKerberosName johnname = new HadoopKerberosName(john.name);
    System.out.println("Name: " + john.name + " to " + johnname.getShortName());
    assertTrue(johnname.getShortName().equals(john.shortName));
    HadoopKerberosName jackname = new HadoopKerberosName(jack.name);
    System.out.println("Name: " + jack.name + " to " + jackname.getShortName());
    assertFalse(jackname.getShortName().equals(jack.shortName));
    HadoopKerberosName hdfsname = new HadoopKerberosName(hdfs.name);
    System.out.println("Name: " + hdfs.name + " to " + hdfsname.getShortName());
    assertTrue(hdfsname.getShortName().equals(hdfs.shortName));
  }

}
