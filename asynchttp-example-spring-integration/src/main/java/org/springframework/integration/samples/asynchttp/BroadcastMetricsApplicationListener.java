/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.samples.asynchttp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.asynchttp.AsyncHttpApplicationEvent;

public class BroadcastMetricsApplicationListener implements
		ApplicationListener<AsyncHttpApplicationEvent> {

	private static final Log LOG = LogFactory.getLog(BroadcastMetricsApplicationListener.class);
	
	@Override
	public void onApplicationEvent(AsyncHttpApplicationEvent event) {

		if (LOG.isInfoEnabled()) {
			LOG.info(String.format("Event: %s; Number of AsynHttp Subscribers: ",
					event.getAsyncHttpEventType().name(), 
					event.getBroadcastMetrics().getNumberOfSubscribers()));
		}

	}

}
