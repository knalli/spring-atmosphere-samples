/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.mvc.samples.atmosphere;

import javax.servlet.http.HttpServletRequest;

import org.atmosphere.cpr.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mvc.samples.atmosphere.controller.Session;

/**
 * @author Gunnar Hillert
 * @since  1.0
 *
 */
public final class AtmosphereUtils {

	public static final Logger LOG = LoggerFactory.getLogger(AtmosphereUtils.class);

	public static AtmosphereResource getAtmosphereResource(HttpServletRequest request) {
		return getMeteor(request).getAtmosphereResource();
	}

	public static Meteor getMeteor(HttpServletRequest request) {
		return Meteor.build(request);
	}

	public static void suspend(final AtmosphereResource resource, Session session) {

		final Broadcaster rootBroadcaster = AtmosphereUtils.lookupBroadcaster();
		rootBroadcaster.addAtmosphereResource(resource);

		if (session.getUsername().startsWith("admin")) {
			final Broadcaster broadcaster = BroadcasterFactory.getDefault().lookup("/admins/" + session.getUsername(), true);
			broadcaster.addAtmosphereResource(resource);
		} else {
			final Broadcaster broadcaster = BroadcasterFactory.getDefault().lookup("/users/" + session.getUsername(), true);
			broadcaster.addAtmosphereResource(resource);
		}

		if (AtmosphereResource.TRANSPORT.LONG_POLLING.equals(resource.transport())) {
			resource.resumeOnBroadcast(true).suspend(-1, false);
		} else {
			resource.suspend(-1);
		}
	}

	public static Broadcaster lookupBroadcaster() {
		Broadcaster b = BroadcasterFactory.getDefault().get();
		return b;
	}

}
