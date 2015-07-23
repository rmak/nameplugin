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

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class implements parsing and handling of Kerberos principal names. In 
 * particular, it splits them apart and translates them down into local
 * operating system names.
 */
@SuppressWarnings("all")
@InterfaceAudience.LimitedPrivate({"HDFS", "MapReduce"})
@InterfaceStability.Evolving
public class HadoopKerberosName extends KerberosName {
  private static final Log LOG = LogFactory.getLog(HadoopKerberosName.class);
  
  private String mapping;
  private UserNameMappingServiceProvider impl;
  private static Configuration conf = null;
  
  /**
   * Create a name from the full Kerberos principal name.
   * @param name
   */
  public HadoopKerberosName(String name) {
    super(name);
    
    if (conf == null)
      conf = new Configuration();
    
    mapping = conf.getTrimmed(CommonConfigurationKeys.HADOOP_SECURITY_USER_NAME_MAPPING);
    if (mapping != null && !mapping.isEmpty()) {
      if(LOG.isDebugEnabled())
        LOG.debug("hadoop.security.user.name.mapping is " + mapping);
//      try {
        impl = 
        ReflectionUtils.newInstance(
          conf.getClass(CommonConfigurationKeys.HADOOP_SECURITY_USER_NAME_MAPPING, null, 
            UserNameMappingServiceProvider.class), conf);
//      } catch (Exception e) {
//        impl = null;
//        if(LOG.isDebugEnabled())
//          LOG.debug("caught exception and impl set to null");
//      }
    }
    else {
      impl = null;
      if(LOG.isDebugEnabled())
        LOG.debug("hadoop.security.user.name.mapping is not defined and hence impl is null");
    }
    
    if(LOG.isDebugEnabled()) {
      if (impl == null) 
        LOG.debug("user name mapping impl=null");
      else
        LOG.debug("user name mapping impl=" + impl.getClass().getName());
    }
  }
  
  /**
   * Get the short name of a given user.
   * 
   * @param user get short name of this user
   * @return short name a given user
   */
  @Override
  public synchronized String getShortName() throws IOException {
    String userShortName;
    if (impl == null) {
      if(LOG.isDebugEnabled())
        LOG.debug("impl is null - trying " + super.getClass().getName());
      userShortName = super.getShortName();
      if(LOG.isDebugEnabled())
        LOG.debug("user short name from " + super.getClass().getName() + " is " + userShortName);
    }
    else {
      userShortName = impl.getShortName(super.toString());
      if(LOG.isDebugEnabled())
        LOG.debug("user short name from impl " + impl.getClass().getName() + " = " + userShortName);
      if (userShortName == null || userShortName.isEmpty()) {
        if(LOG.isDebugEnabled())
          LOG.debug("user short name from impl is null or empty - trying " + super.getClass().getName());
        userShortName = super.getShortName();
        if(LOG.isDebugEnabled())
          LOG.debug("user short name from " + super.getClass().getName() + " is " + userShortName);
      }      
    }
    return userShortName;
  }
  
  /**
   * Set the static configuration to get the rules.
   * Also save configuration for initialization use if call before object created.
   * <p/>
   * IMPORTANT: This method does a NOP if the rules have been set already.
   * If there is a need to reset the rules, the {@link KerberosName#setRules(String)}
   * method should be invoked directly.
   * 
   * @param conf the new configuration
   * @throws IOException
   */
  public static void setConfiguration(Configuration conf) throws IOException {
    final String defaultRule;
    HadoopKerberosName.conf = conf;
    switch (SecurityUtil.getAuthenticationMethod(conf)) {
      case KERBEROS:
      case KERBEROS_SSL:
        try {
          KerberosUtil.getDefaultRealm();
        } catch (Exception ke) {
          throw new IllegalArgumentException("Can't get Kerberos realm", ke);
        }
        defaultRule = "DEFAULT";
        break;
      default:
        // just extract the simple user name
        defaultRule = "RULE:[1:$1] RULE:[2:$1]";
        break; 
    }
    String ruleString = conf.get(HADOOP_SECURITY_AUTH_TO_LOCAL, defaultRule);
    setRules(ruleString);

  }

  public static void main(String[] args) throws Exception {
    setConfiguration(new Configuration());
    for(String arg: args) {
      HadoopKerberosName name = new HadoopKerberosName(arg);
      System.out.println("Name: " + name + " to " + name.getShortName());
    }
  }
}
