package org.springframework.mvc.samples.atmosphere.redis;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.atmosphere.cpr.AtmosphereConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisUtil {

	private static final Logger logger = LoggerFactory.getLogger(RedisBroadcaster.class);

	private static final String REDIS_AUTH = RedisBroadcaster.class.getName() + ".authorization";
	private static final String REDIS_SERVER = RedisBroadcaster.class.getName() + ".server";
	private static final String REDIS_SHARED_POOL = RedisBroadcaster.class.getName() + ".sharedPool";

	// NEW >>
	private static final String REDIS_CONFIG = RedisBroadcaster.class.getName() + ".config";
	// << NEW

	private Jedis jedisSubscriber;
	private Jedis jedisPublisher;
	private String authToken = null;

	private boolean sharedPool = false;
	private JedisPool jedisPool;
	private final AtmosphereConfig config;
	private URI uri;
	private final AtomicBoolean destroyed = new AtomicBoolean();
	private final Callback callback;

	public RedisUtil(URI uri, AtmosphereConfig config, Callback callback) {
		this.config = config;
		this.callback = callback;
		this.uri = uri;
	}

	public String getAuth() {
		return authToken;
	}

	public void setAuth(String auth) {
		authToken = auth;
	}

	public void configure() {

		// NEW >>
		if (config.getServletConfig().getInitParameter(REDIS_CONFIG) != null) {
			try {
				final Properties props = resolveConfig(config.getServletConfig().getInitParameter(REDIS_CONFIG));
				if (props.containsKey("authorization")) {
					authToken = props.getProperty("authorization");
				}
				if (props.containsKey("server")) {
					uri = URI.create(props.getProperty("server"));
				}
				if (props.containsKey("sharedPool")) {
					sharedPool = Boolean.parseBoolean(props.getProperty("sharedPool"));
				}
			} catch (Exception e) {
				logger.error("Could not load and read the config file {}", REDIS_CONFIG);
			}
		}
		// << NEW

		if (config.getServletConfig().getInitParameter(REDIS_AUTH) != null) {
			authToken = config.getServletConfig().getInitParameter(REDIS_AUTH);
		}

		if (config.getServletConfig().getInitParameter(REDIS_SERVER) != null) {
			uri = URI.create(config.getServletConfig().getInitParameter(REDIS_SERVER));
		} else if (uri == null) {
			throw new NullPointerException("uri cannot be null");
		}

		if (config.getServletConfig().getInitParameter(REDIS_SHARED_POOL) != null) {
			sharedPool = Boolean.parseBoolean(config.getServletConfig().getInitParameter(REDIS_SHARED_POOL));
		}

		logger.info("{} shared connection pool {}", getClass().getName(), sharedPool);

		if (sharedPool) {
			if (config.properties().get(REDIS_SHARED_POOL) != null) {
				jedisPool = (JedisPool) config.properties().get(REDIS_SHARED_POOL);
			}

			// setup is synchronized, no need to sync here as well.
			if (jedisPool == null) {
				GenericObjectPool.Config gConfig = new GenericObjectPool.Config();
				gConfig.testOnBorrow = true;
				gConfig.testWhileIdle = true;

				jedisPool = new JedisPool(gConfig, uri.getHost(), uri.getPort());

				config.properties().put(REDIS_SHARED_POOL, jedisPool);
			} else {
				disconnectSubscriber();
			}
		}

		// We use the pool only for publishing
		jedisSubscriber = new Jedis(uri.getHost(), uri.getPort());
		try {
			jedisSubscriber.connect();
			auth(jedisSubscriber);
		} catch (JedisException e) {
			logger.error("failed to connect subscriber", e);
			disconnectSubscriber();
		}

		jedisPublisher = sharedPool ? null : new Jedis(uri.getHost(), uri.getPort());
		if (!sharedPool) {
			try {
				jedisPublisher.connect();
				auth(jedisPublisher);
			} catch (JedisException e) {
				logger.error("failed to connect publisher", e);
				disconnectPublisher();
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuration:");
			logger.debug("RedisBroadcaster.uri={}", uri.toString());
			logger.debug("RedisBroadcaster.authToken={}", authToken);
			logger.debug("RedisBroadcaster.sharedPool={}", sharedPool);
		}
	}

	private Properties resolveConfig(String configLocation) throws IOException {
		final Properties props = new Properties();

		if (props.isEmpty()) {
			// Absolute path
			try {
				props.load(new FileReader(configLocation));
				logger.info("Found config at using absolute path.");
			} catch (Exception e) {
			}
		}

		if (props.isEmpty()) {
			// in home dir
			try {
				props.load(new FileReader(System.getProperty("user.home") + "/" + configLocation));
				logger.info("Found config at using home dir.");
			} catch (Exception e) {
			}
		}

		if (props.isEmpty()) {
			// class path
			try {
				props.load(RedisUtil.class.getClassLoader().getResourceAsStream(configLocation));
				logger.info("Found config at using class loader.");
			} catch (Exception e) {
			}
		}

		return props;
	}

	public synchronized void setID(String id) {
		disconnectPublisher();
		disconnectSubscriber();
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
		if (!sharedPool) {
			Object lockingObject = getLockingObject();
			synchronized (lockingObject) {
				try {
					disconnectPublisher();
					disconnectSubscriber();
					if (jedisPool != null) {
						jedisPool.destroy();
					}
				} catch (Throwable t) {
					logger.warn("Jedis error on close", t);
				} finally {
					config.properties().put(REDIS_SHARED_POOL, null);
				}
			}
		}
	}

	public boolean isShared() {
		return sharedPool;
	}

	/**
	 * {@inheritDoc}
	 */
	public void incomingBroadcast() {
		logger.info("Subscribing to: {}", callback.getID());

		jedisSubscriber.subscribe(new JedisPubSub() {

			public void onMessage(String channel, String message) {
				callback.broadcastReceivedMessage(message);
			}

			public void onSubscribe(String channel, int subscribedChannels) {
				logger.debug("onSubscribe: {}", channel);
			}

			public void onUnsubscribe(String channel, int subscribedChannels) {
				logger.debug("onUnsubscribe: {}", channel);
			}

			public void onPSubscribe(String pattern, int subscribedChannels) {
				logger.debug("onPSubscribe: {}", pattern);
			}

			public void onPUnsubscribe(String pattern, int subscribedChannels) {
				logger.debug("onPUnsubscribe: {}", pattern);
			}

			public void onPMessage(String pattern, String channel, String message) {
				logger.debug("onPMessage: pattern: {}, channel: {}, message: {}",
						new Object[]{pattern, channel, message});
			}
		}, callback.getID());
	}

	public void outgoingBroadcast(Object message) {
		// Marshal the message outside of the sync block.
		String contents = message.toString();

		Object lockingObject = getLockingObject();
		synchronized (lockingObject) {
			if (destroyed.get()) {
				logger.debug("JedisPool closed. Re-opening");
				setID(callback.getID());
			}

			if (sharedPool) {
				for (int i = 0; i < 10; ++i) {
					boolean valid = true;
					Jedis jedis = jedisPool.getResource();

					try {
						auth(jedis);
						jedis.publish(callback.getID(), contents);
					} catch (JedisException e) {
						valid = false;
						logger.warn("outgoingBroadcast exception", e);
					} finally {
						if (valid) {
							jedisPool.returnResource(jedis);
						} else {
							jedisPool.returnBrokenResource(jedis);
						}
					}

					if (valid) {
						break;
					}
				}
			} else {
				try {
					jedisPublisher.publish(callback.getID(), contents);
				} catch (JedisException e) {
					logger.warn("outgoingBroadcast exception", e);
				}
			}
		}
	}

	private void auth(Jedis jedis) {
		if (authToken != null) {
			jedis.auth(authToken);
		}
	}

	private void disconnectSubscriber() {
		if (jedisSubscriber == null) return;

		synchronized (jedisSubscriber) {
			try {
				jedisSubscriber.disconnect();
			} catch (JedisException e) {
				logger.error("failed to disconnect subscriber", e);
			}
		}
	}

	private void disconnectPublisher() {
		if (jedisPublisher == null) return;

		synchronized (jedisPublisher) {
			try {
				jedisPublisher.disconnect();
			} catch (JedisException e) {
				logger.error("failed to disconnect publisher", e);
			}
		}
	}

	private Object getLockingObject() {
		return sharedPool ? jedisPool : jedisPublisher;
	}

	public static interface Callback {

		String getID();

		void broadcastReceivedMessage(String message);

	}
}
