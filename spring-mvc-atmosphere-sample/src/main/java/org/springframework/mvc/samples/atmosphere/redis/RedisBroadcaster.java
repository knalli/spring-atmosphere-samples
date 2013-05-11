package org.springframework.mvc.samples.atmosphere.redis;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.util.AbstractBroadcasterProxy;

import java.net.URI;

public class RedisBroadcaster extends AbstractBroadcasterProxy {

	private final RedisUtil redisUtil;

	public RedisBroadcaster(String id, AtmosphereConfig config) {
		this(URI.create("http://localhost:6379"), id, config);
	}

	public RedisBroadcaster(URI uri, String id, AtmosphereConfig config) {
		super(id, URI.create("http://localhost:6379"), config);
		this.redisUtil = new RedisUtil(uri, config, new RedisUtil.Callback() {
			@Override
			public String getID() {
				return RedisBroadcaster.this.getID();
			}

			@Override
			public void broadcastReceivedMessage(String message) {
				RedisBroadcaster.this.broadcastReceivedMessage(message);
			}
		});
	}

	public String getAuth() {
		return redisUtil.getAuth();
	}

	public void setAuth(String auth) {
		redisUtil.setAuth(auth);

	}

	public synchronized void setUp() {
		redisUtil.configure();
	}

	@Override
	public synchronized void setID(String id) {
		super.setID(id);
		setUp();
		reconfigure();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		super.destroy();
		redisUtil.destroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void incomingBroadcast() {
		redisUtil.incomingBroadcast();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void outgoingBroadcast(Object message) {
		redisUtil.outgoingBroadcast(message);
	}
}