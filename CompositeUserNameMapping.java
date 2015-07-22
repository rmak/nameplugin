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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * An implementation of {@link UserNameMappingServiceProvider} which
 * composites other user name mapping providers for determining user short name.
 * This allows to combine existing provider implementations and composite 
 * a virtually new provider without customized development to deal with complex situation. 
 */
@InterfaceAudience.LimitedPrivate({"HDFS", "MapReduce"})
@InterfaceStability.Evolving
public class CompositeUserNameMapping
  implements UserNameMappingServiceProvider, Configurable {
  
  public static final String MAPPING_PROVIDERS_CONFIG_KEY = USER_NAME_MAPPING_CONFIG_PREFIX + ".providers";
  public static final String MAPPING_PROVIDER_CONFIG_PREFIX = USER_NAME_MAPPING_CONFIG_PREFIX + ".provider";
  
  private static final Log LOG = LogFactory.getLog(CompositeUserNameMapping.class);
  
  private List<UserNameMappingServiceProvider> providersList = 
    new ArrayList<UserNameMappingServiceProvider>();
  
  private Configuration conf;
  
  /**
   * Get the short name of a given user.
   * 
   * @param user get short name of this user
   * @return short name a given user
   */
  @Override
  public synchronized String getShortName(String user) throws IOException {
  
    String userShortName = null;
    for (UserNameMappingServiceProvider provider : providersList) {
      try {
        userShortName = provider.getShortName(user);
      } catch (Exception e) {
        if(LOG.isDebugEnabled())
          LOG.debug("Exception trying to get short name of user " + user, e);      
      }        
      if (userShortName != null && ! userShortName.isEmpty()) {
        if(LOG.isDebugEnabled())
          LOG.debug("get short name of user " + user + " found by provider " + provider.getClass().getName());      
        break;
      }
    }
    
    return userShortName;
  }
  
  /**
   * Caches user names, no need to do that for this provider
   */
  @Override
  public void cacheUserNameRefresh() throws IOException {
    // does nothing in this provider of user name mapping
  }
  
  /** 
   * Adds user name to cache, no need to do that for this provider
   *
   * @param user unused
   */
  @Override
  public void cacheUserNameAdd(List<String> user) throws IOException {
    // does nothing in this provider of user name mapping
  }
  
  @Override
  public synchronized Configuration getConf() {
    return conf;
  }
  
  @Override
  public synchronized void setConf(Configuration conf) {
    this.conf = conf;
    
    loadMappingProviders();
  }
  
  private void loadMappingProviders() {
    String[] providerNames = conf.getStrings(MAPPING_PROVIDERS_CONFIG_KEY, new String[]{});
    
    String providerKey;
    for (String name : providerNames) {
      providerKey = MAPPING_PROVIDER_CONFIG_PREFIX + "." + name;
      Class<?> providerClass = conf.getClass(providerKey, null);
      if (providerClass == null) {
        LOG.error("The mapping provider, " + name + " does not have a valid class");
      } else {
        addMappingProvider(name, providerClass);
      }
    }
  }
  
  private void addMappingProvider(String providerName, Class<?> providerClass) {
    Configuration newConf = prepareConf(providerName);
    UserNameMappingServiceProvider provider = 
      (UserNameMappingServiceProvider) ReflectionUtils.newInstance(providerClass, newConf);
    providersList.add(provider);
  
  }
  
  /*
   * For any provider specific configuration properties, such as "hadoop.security.user.name.mapping.ldap.url" 
   * and the like, allow them to be configured as "hadoop.security.user.name.mapping.provider.PROVIDER-X.ldap.url",
   * so that a provider such as LdapUserNameMapping can be used to composite a complex one with other providers.
   */
  private Configuration prepareConf(String providerName) {
    Configuration newConf = new Configuration();
    Iterator<Map.Entry<String, String>> entries = conf.iterator();
    String providerKey = MAPPING_PROVIDER_CONFIG_PREFIX + "." + providerName;
    while (entries.hasNext()) {
      Map.Entry<String, String> entry = entries.next();
      String key = entry.getKey();
      // get a property like "hadoop.security.user.name.mapping.provider.PROVIDER-X.ldap.url"
      if (key.startsWith(providerKey) && !key.equals(providerKey)) {
        // restore to be the one like "hadoop.security.user.mapping.ldap.url" 
        // so that can be used by original provider.
        key = key.replace(".provider." + providerName, "");
        newConf.set(key, entry.getValue());
      }
    }
    return newConf;
  }  
}
