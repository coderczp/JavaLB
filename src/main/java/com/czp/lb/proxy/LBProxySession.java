package com.czp.lb.proxy;

import java.io.IOException;
import java.net.SocketAddress;

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
 * Function:代理会话类,持有前后端连接<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
@SuppressWarnings("rawtypes")
public class LBProxySession extends BaseFilter {

	private Connection front;
	private Connection backend;
	private static Logger proxyLog = LoggerFactory.getLogger("proxy.log");
	private static Logger log = LoggerFactory.getLogger(LBProxySession.class);

	public LBProxySession(Connection conn) {
		this.front = conn;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		Object message = ctx.getMessage();
		if (message instanceof Buffer) {
			((Buffer) message).allowBufferDispose(true);
		}
		front.write(message);
		return ctx.getStopAction();
	}

	@Override
	public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
		log.error("proxy error:", error);
		front.closeSilently();
	}

	/**
	 * 建立后端连接并记录日志
	 * 
	 * @param serverAddr
	 * @throws Exception
	 */
	public void createBackendConn(SocketAddress serverAddr) throws Exception {
		FilterChainBuilder fcBuilder = FilterChainBuilder.stateless();
		fcBuilder.add(new TransportFilter());
		fcBuilder.add(this);

		TCPNIOTransport tsp = TCPNIOTransportBuilder.newInstance().build();
		tsp.setProcessor(fcBuilder.build());
		tsp.start();
		backend = tsp.connect(serverAddr).get();
		proxyLog.info("[{}][{}][{}]", front.getPeerAddress(),
				front.getLocalAddress(), serverAddr);
	}

	public Connection getBackendConn() {
		return backend;
	}
}