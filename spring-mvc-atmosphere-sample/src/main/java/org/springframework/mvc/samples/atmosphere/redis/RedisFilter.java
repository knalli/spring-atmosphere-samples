package org.springframework.mvc.samples.atmosphere.redis;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.BroadcastFilter;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.ClusterBroadcastFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisFilter implements ClusterBroadcastFilter {

	private static final Logger logger = LoggerFactory.getLogger(RedisFilter.class);

	private Broadcaster bc;
	private final ExecutorService listener = Executors.newSingleThreadExecutor();
	private final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<String>();
	private URI uri;
	private RedisUtil redisUtil;
	private AtmosphereConfig config;
	private final ConcurrentLinkedQueue<String> localMessages = new  ConcurrentLinkedQueue<String>();
	private String auth;

	public RedisFilter() {
		this(URI.create("http://localhost:6379"));
	}

	public RedisFilter(URI uri) {
		this.uri = uri;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setUri(String address) {
		uri = URI.create(address);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(AtmosphereConfig config) {
		this.config = config;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		listener.shutdownNow();
		redisUtil.destroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BroadcastFilter.BroadcastAction filter(Object originalMessage, Object o) {
		String contents = originalMessage.toString();
		boolean local = localMessages.remove(contents);
		boolean received = receivedMessages.remove(contents);

		if (!local) {
			if (!received) {
				localMessages.offer(contents);
				redisUtil.outgoingBroadcast(originalMessage.toString());
			}

			return new BroadcastFilter.BroadcastAction(BroadcastAction.ACTION.CONTINUE, o);
		}

		return new BroadcastFilter.BroadcastAction(BroadcastAction.ACTION.ABORT, o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Broadcaster getBroadcaster() {
		return bc;
	}

	public String getAuth() {
		return redisUtil.getAuth();
	}

	public void setAuth(String auth) {
		if (redisUtil != null) {
			redisUtil.setAuth(auth);
		} else {
			this.auth = auth;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBroadcaster(final Broadcaster bc) {
		this.bc = bc;

		this.redisUtil = new RedisUtil(uri, config, new RedisUtil.Callback() {
			@Override
			public String getID() {
				return bc.getID();
			}

			@Override
			public void broadcastReceivedMessage(String message) {
				receivedMessages.offer(message);
				bc.broadcast(message);
			}
		});

		if (auth != null) redisUtil.setAuth(auth);
		redisUtil.configure();


		listener.submit(new Runnable() {
			public void run() {
				redisUtil.incomingBroadcast();
			}
		});
	}
}
