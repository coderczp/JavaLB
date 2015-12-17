package com.czp.lb.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Function:负载均衡服务类,负责接入前端并按策略路由<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
@SuppressWarnings("rawtypes")
public class LBProxyServer extends BaseFilter {

	private int port;
	private TCPNIOTransport server;
	private LBRouteStrategy strategy;
	private static Logger log = LoggerFactory.getLogger(LBProxyServer.class);
	private List<LBBackendServer> backends = new ArrayList<LBBackendServer>();
	private Map<String, LBProxySession> sessions = new ConcurrentHashMap<String, LBProxySession>();

	public LBProxyServer(int port, LBRouteStrategy strategy) {
		this.port = port;
		this.strategy = strategy;
	}

	/**
	 * 添加后端服务,会根据权重计算后添加多个引用
	 * 
	 * @param host
	 * @param port
	 */
	public synchronized boolean addBackendServer(LBBackendServer addr) {
		if (backends.isEmpty()) {
			return backends.add(addr);
		}
		float minWeight = 1;
		for (LBBackendServer ser : backends) {
			int weight = ser.getWeight();
			if (weight > 0 && weight < minWeight)
				minWeight = weight;
		}
		int addCount = (int) Math.ceil(addr.getWeight() / minWeight);
		while (addCount-- > 0) {
			backends.add(addr);
		}
		return true;

	}

	/**
	 * 删除后端服务
	 * 
	 * @param host
	 * @param port
	 * @return
	 */
	public synchronized boolean delBackendServer(LBBackendServer addr) {
		Iterator<LBBackendServer> it = backends.iterator();
		while (it.hasNext()) {
			if (it.next().equals(addr))
				it.remove();
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		Object message = ctx.getMessage();
		if (message instanceof Buffer) {
			((Buffer) message).allowBufferDispose(true);
		}
		Connection conn = ctx.getConnection();
		String addr = conn.getPeerAddress().toString();
		LBProxySession session = sessions.get(addr);
		session.getBackendConn().write(message);
		return ctx.getStopAction();
	}

	@Override
	public NextAction handleAccept(FilterChainContext ctx) throws IOException {
		if (backends.isEmpty()) {
			log.error("backend server list is empty,so close client conn");
			ctx.getConnection().closeSilently();
			return ctx.getStopAction();
		}
		Connection conn = ctx.getConnection();
		LBProxySession session = new LBProxySession(conn);
		try {
			Object add = conn.getPeerAddress();
			log.debug("accept client: {}", add);
			InetSocketAddress addr = (InetSocketAddress) add;
			session.createBackendConn(strategy.doReoute(backends, addr));
			sessions.put(add.toString(), session);
		} catch (Exception e) {
			log.error("accept error:", e);
			conn.closeSilently();
			if (session.getBackendConn() != null)
				session.getBackendConn().closeSilently();
		}
		return ctx.getStopAction();
	}

	@Override
	public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
		log.error("proxy error:", error);
		Connection conn = ctx.getConnection();
		String addr = conn.getPeerAddress().toString();
		LBProxySession session = sessions.remove(addr);
		session.getBackendConn().closeSilently();
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		Connection conn = ctx.getConnection();
		Object client = conn.getPeerAddress();
		log.debug("close:[{}-{}]", conn.getLocalAddress(), client);
		if (client == null)
			return ctx.getStopAction();

		LBProxySession session = sessions.remove(client.toString());
		if (session == null)
			return ctx.getStopAction();

		session.getBackendConn().closeSilently();
		return ctx.getStopAction();
	}

	public void start() throws Exception {
		server = TCPNIOTransportBuilder.newInstance().build();
		FilterChainBuilder fcBuilder = FilterChainBuilder.stateless();
		fcBuilder.add(new TransportFilter());
		fcBuilder.add(this);

		server.setProcessor(fcBuilder.build());
		server.bind(port);
		server.start();

		log.info("proxy is running at :{}", port);
	}

	/**
	 * 停止代理服务,释放资源
	 * 
	 * @throws IOException
	 */
	public void stop() throws IOException {
		server.shutdownNow();
		backends.clear();
		sessions.clear();
	}
}
