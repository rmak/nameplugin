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
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;

/**
 * An interface for the implementation of a Kerberos-principle-name-to-unix-name mapping service
 * used by {@link HadoopKerberosName}.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public interface UserNameMappingServiceProvider {
  public static final String USER_NAME_MAPPING_CONFIG_PREFIX = CommonConfigurationKeysPublic.HADOOP_SECURITY_USER_NAME_MAPPING;
  
  /**
   * Get the translation of the principal name into an operating system
   * user name.
   * Returns EMPTY list in case of non-existing user
   * @param user User's Kerberos principle name
   * @return user's short name
   * @throws IOException
   */
  public String getShortName(String user) throws IOException;
  /**
   * Refresh the cache of user Kerberos principle name to unix name mapping
   * @throws IOException
   */
  public void cacheUserNameRefresh() throws IOException;
  /**
   * Caches the user name information
   * @param user User to add to cache
   * @throws IOException
   */
  public void cacheUserNameAdd(List<String> User) throws IOException;
}
