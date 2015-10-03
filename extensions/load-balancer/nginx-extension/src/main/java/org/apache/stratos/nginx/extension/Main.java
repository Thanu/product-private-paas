/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.nginx.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.load.balancer.extension.api.LoadBalancerExtension;

import java.util.concurrent.ExecutorService;

/**
 * Nginx extension main class.
 */
public class Main {
	private static final Log log = LogFactory.getLog(Main.class);
	private static ExecutorService executorService;

	public static void main(String[] args) {

		LoadBalancerExtension extension = null;
		try {
			// Configure log4j properties
			PropertyConfigurator.configure(System.getProperty("log4j.properties.file.path"));

			if (log.isInfoEnabled()) {
				log.info("Nginx extension started");
			}

            // Add shutdown hook
            final Thread mainThread = Thread.currentThread();
            final LoadBalancerExtension finalExtension = extension;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        if(finalExtension != null) {
                            log.info("Shutting nginx instance...");
                            finalExtension.stop();
                        }
                        mainThread.join();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            });

			executorService = StratosThreadPool.getExecutorService("nginx.extension.thread.pool", 10);
			// Validate runtime parameters
			NginxContext.getInstance().validate();
            TopologyProvider topologyProvider = new TopologyProvider();
            NginxStatisticsReader statisticsReader = NginxContext.getInstance().isCEPStatsPublisherEnabled() ?
                    new NginxStatisticsReader(topologyProvider) : null;
            extension = new LoadBalancerExtension(new Nginx(), statisticsReader, topologyProvider);
			extension.setExecutorService(executorService);
			extension.execute();
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error(e);
			}
			if (extension != null) {
                log.info("Shutting nginx instance...");
				extension.stop();
			}
		}
	}
}
