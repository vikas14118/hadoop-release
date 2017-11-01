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

package org.apache.hadoop.fs.azuredfs;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.azuredfs.constants.FileSystemUriSchemes;
import org.apache.hadoop.fs.azuredfs.constants.TestConfigurationKeys;
import org.apache.hadoop.fs.azuredfs.contracts.exceptions.ServiceResolutionException;
import org.apache.hadoop.fs.azuredfs.contracts.services.AdfsBlobHandler;
import org.apache.hadoop.fs.azuredfs.contracts.services.AdfsHttpClientFactory;
import org.apache.hadoop.fs.azuredfs.contracts.services.AdfsHttpClientSession;
import org.apache.hadoop.fs.azuredfs.contracts.services.AdfsHttpClientSessionFactory;
import org.apache.hadoop.fs.azuredfs.contracts.services.ConfigurationService;
import org.apache.hadoop.fs.azuredfs.contracts.services.LoggingService;
import org.apache.hadoop.fs.azuredfs.services.MockAdfsBlobHandlerImpl;
import org.apache.hadoop.fs.azuredfs.services.MockAdfsHttpClientFactoryImpl;
import org.apache.hadoop.fs.azuredfs.services.MockAdfsHttpClientSessionFactoryImpl;
import org.apache.hadoop.fs.azuredfs.services.MockConfigurationServiceImpl;
import org.apache.hadoop.fs.azuredfs.services.MockLoggingServiceImpl;
import org.apache.hadoop.fs.azuredfs.services.MockServiceInjectorImpl;
import org.apache.hadoop.fs.azuredfs.services.MockServiceProviderImpl;
import org.apache.hadoop.fs.azuredfs.services.ServiceProviderImpl;

public abstract class DependencyInjectedTest {
  protected final MockServiceInjectorImpl mockServiceInjector;
  private final Configuration configuration;
  private final String fileSystemName;
  private AdfsHttpClientSession adfsHttpClientSession;

  protected DependencyInjectedTest() throws Exception {
    fileSystemName = UUID.randomUUID().toString();
    configuration = new Configuration();
    configuration.addResource("azure-adfs-test.xml");

    final URI defaultUri = new URI(FileSystemUriSchemes.ADFS_SCHEME, getTestUrl(), null, null, null);
    configuration.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, defaultUri.toString());
    this.mockServiceInjector = new MockServiceInjectorImpl(configuration);

    this.mockServiceInjector.replaceProvider(AdfsHttpClientSessionFactory.class, MockAdfsHttpClientSessionFactoryImpl
        .class);
    this.mockServiceInjector.replaceProvider(ConfigurationService.class, MockConfigurationServiceImpl.class);
    this.mockServiceInjector.replaceProvider(LoggingService.class, MockLoggingServiceImpl.class);
    this.mockServiceInjector.replaceProvider(AdfsHttpClientFactory.class, MockAdfsHttpClientFactoryImpl.class);
    this.mockServiceInjector.replaceProvider(AdfsBlobHandler.class, MockAdfsBlobHandlerImpl.class);
  }

  @Before
  public void initialize() throws ServiceResolutionException {
    MockServiceProviderImpl.Create(this.mockServiceInjector);
    MockAdfsHttpClientSessionFactoryImpl azureDistributedFileSystemClientFactory =
        (MockAdfsHttpClientSessionFactoryImpl) ServiceProviderImpl.instance().get(AdfsHttpClientSessionFactory.class);

    this.adfsHttpClientSession = azureDistributedFileSystemClientFactory.create(
        this.getAccountName(),
        this.getAccountKey(),
        this.getFileSystemName(),
        this.getHostName());
    azureDistributedFileSystemClientFactory.setSession(this.adfsHttpClientSession);
  }

  @After
  public void clearFileSystemCache() throws IOException {
    FileSystem.closeAll();
  }

  protected String getHostName() {
    return configuration.get(TestConfigurationKeys.FS_AZURE_TEST_HOST_NAME);
  }

  protected String getTestUrl() {
    return this.getFileSystemName() + "@" + this.getAccountName() + TestConfigurationKeys.FS_AZURE_TEST_ACCOUNT_KEY_SUFFIX;
  }

  protected String getFileSystemName() {
    return fileSystemName;
  }

  protected String getAccountName() {
    return configuration.get(TestConfigurationKeys.FS_AZURE_TEST_ACCOUNT_NAME);
  }

  protected String getAccountKey() {
    return configuration.get(
        TestConfigurationKeys.FS_AZURE_TEST_ACCOUNT_KEY_PREFIX
            + getAccountName()
            + TestConfigurationKeys.FS_AZURE_TEST_ACCOUNT_KEY_SUFFIX);
  }

  protected Configuration getConfiguration() {
    return this.configuration;
  }
}
