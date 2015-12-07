package com.czp.lb.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.lb.proxy.LBBackendServer;
import com.czp.lb.proxy.LBProxyServer;
import com.czp.lb.zk.LBZKClient;

/**
 * 
 * Function:http server<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
public class WebStart {

	private static String proxy_log_key = "log4j.appender.proxy.File";
	private static Logger log = LoggerFactory.getLogger(WebStart.class);

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Usage: webdir webport log4j_cfg zk_ser");
			return;
		}

		String webRoot = args[0];
		Integer port = Integer.valueOf(args[1]);
		String proxyLog = initLogConfig(args[2]);

		LBDao dao = new LBDao("lb.db");
		initLBDB(dao);
		LBProxyServer proxy = startProxy(dao);
		// LBZKClient zkCli = startZkClient(args[3], proxy);
		LBServlet lbServer = new LBServlet(dao, webRoot, proxy, proxyLog);

		StaticHttpHandler sHandler = new StaticHttpHandler(webRoot);
		HttpServer server = HttpServer.createSimpleServer(webRoot, port);
		ServerConfiguration sercfg = server.getServerConfiguration();
		sercfg.addHttpHandler(lbServer, "/lb/*");
		sercfg.addHttpHandler(sHandler, "/*");
		server.start();

		System.out.println("press any key to exit");
		System.in.read();

		proxy.stop();
		// zkCli.closeConnect();
		server.shutdownNow();
	}

	/**
	 * @param zkSer
	 * @param proxy
	 */
	protected static LBZKClient startZkClient(String zkSer,
			final LBProxyServer proxy) {
		LBZKClient client = new LBZKClient(zkSer, "");
		client.getData("", new Watcher() {

			@Override
			public void process(WatchedEvent event) {

			}
		});
		return client;
	}

	/**
	 * 加载日志配置
	 * 
	 * @param logPath
	 * @return
	 * @throws IOException
	 */
	private static String initLogConfig(String logPath) throws IOException {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(logPath);
		props.load(fis);
		fis.close();
		PropertyConfigurator.configure(props);
		return props.getProperty(proxy_log_key);
	}

	/**
	 * 初始化数据库
	 * 
	 * @param dao
	 * @throws Exception
	 */
	public static void initLBDB(LBDao dao) throws Exception {
		if (dao.query(LBSQL.sql_check, 1).isEmpty()) {
			log.info("creat new table:{}", LBSQL.sql_host);
			dao.createTable(LBSQL.sql_host);
			dao.createTable(LBSQL.sql_glob);
			dao.insert(LBSQL.sql_dft);
		} else {
			log.info("table aready exist");
		}
	}

	/**
	 * 启动负载均衡服务
	 * 
	 * @param dao
	 * @return
	 * @throws Exception
	 */
	public static LBProxyServer startProxy(LBDao dao) throws Exception {

		List<Object[]> hosts = dao.query(LBSQL.query_host, 3);
		List<Object[]> globs = dao.query(LBSQL.query_glb, 2);

		String reouteType = (String) globs.get(0)[0];
		String lbPort = (String) globs.get(0)[1];
		Integer lbport = Integer.valueOf(lbPort);
		LBProxyServer proxy = new LBProxyServer(lbport, reouteType);

		for (Object[] obj : hosts) {
			String host = obj[0].toString();
			Integer port = Integer.valueOf(obj[1].toString());
			Integer weight = Integer.valueOf(obj[2].toString());
			proxy.addBackendServer(new LBBackendServer(host, port, weight));
			log.info("add backend server:{}:{} {}", host, port, weight);
		}
		proxy.start();
		return proxy;
	}
}
