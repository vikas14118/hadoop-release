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


package org.apache.hadoop.fs.azuredfs.services;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.azuredfs.contracts.exceptions.AzureDistributedFileSystemException;
import org.apache.hadoop.fs.azuredfs.contracts.log.LogLevel;
import org.apache.hadoop.fs.azuredfs.contracts.services.LoggingService;
import org.apache.hadoop.fs.azuredfs.contracts.services.TracingService;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.Sampler;
import org.apache.htrace.core.SpanId;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

@Singleton
@InterfaceAudience.Private
@InterfaceStability.Evolving
final class TracingServiceImpl implements TracingService {
  private final LoggingService loggingService;
  private final Tracer tracer;

  @Inject
  TracingServiceImpl(
      final Configuration configuration,
      final LoggingService loggingService) {
    Preconditions.checkNotNull("configuration", configuration);
    Preconditions.checkNotNull("loggingService", loggingService);

    this.loggingService = loggingService;

    this.tracer = new Tracer.Builder(TracingServiceImpl.class.getSimpleName()).
        conf(new HTraceConfiguration() {
          @Override
          public String get(String key) {
            if (key == Tracer.SPAN_RECEIVER_CLASSES_KEY) {
              return LoggerSpanReceiver.class.getName();
            }
            return null;
          }

          @Override
          public String get(String key, String defaultValue) {
            String value = get(key);
            if (value != null) {
              return value;
            }
            return defaultValue;
          }
        }).
        build();

    this.tracer.addSampler(Sampler.ALWAYS);
  }

  @Override
  public TraceScope traceBegin(String description) {
    if (this.loggingService.logLevelEnabled(LogLevel.Trace)) {
      TraceScope traceScope = this.tracer.newScope(description);
      traceScope.addTimelineAnnotation("Begin: " + description);
      return traceScope;
    }

    return null;
  }

  @Override
  public TraceScope traceBegin(String description, SpanId parentSpanId) {
    if (this.loggingService.logLevelEnabled(LogLevel.Trace)) {
      TraceScope traceScope = this.tracer.newScope(description, parentSpanId);
      traceScope.addTimelineAnnotation("End: " + description);
      return traceScope;
    }

    return null;
  }

  @Override
  public void traceException(TraceScope traceScope, AzureDistributedFileSystemException azureDistributedFileSystemException) {
    if (this.loggingService.logLevelEnabled(LogLevel.Trace)) {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      azureDistributedFileSystemException.printStackTrace(printWriter);
      printWriter.flush();

      traceScope.addKVAnnotation("Exception", stringWriter.toString());
    }
  }

  @Override
  public void traceEnd(TraceScope traceScope) {
    if (this.loggingService.logLevelEnabled(LogLevel.Trace)) {
      traceScope.close();
    }
  }
}