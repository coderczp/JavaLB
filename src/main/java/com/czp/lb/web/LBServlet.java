package com.czp.lb.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.lb.proxy.LBBackendServer;
import com.czp.lb.proxy.LBProxyServer;

/**
 * 
 * Function:前端查询/添加/删除主机/查询路由信息的实现类<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
public class LBServlet extends HttpHandler {

	private LBDao dao;
	private String webRoot;
	private LBProxyServer proxy;
	private String proxyLogPath;
	public static Object[] EMPTY = new Object[0];
	private static Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]");
	private static Logger log = LoggerFactory.getLogger(LBServlet.class);

	public LBServlet(LBDao dao, String webRoot, LBProxyServer proxy,
			String proxyLog) {
		this.proxyLogPath = proxyLog;
		this.webRoot = webRoot;
		this.proxy = proxy;
		this.dao = dao;
	}

	@Override
	public void service(Request req, Response resp) throws Exception {
		try {
			resp.setCharacterEncoding("UTF-8");
			Writer writer = resp.getWriter();
			String uri = req.getRequestURI();
			LBModelView res1 = null;

			if (uri.endsWith("/lbquery"))
				res1 = doBackendServerQuery();
			else if (uri.endsWith("/route"))
				res1 = doReouteLogQuery();
			else if (uri.endsWith("/lbadd"))
				res1 = doAddBanckendServer(req);

			if (res1.isJson()) {
				writer.write(res1.getHtmlFile());
				return;
			}
			String foot = webRoot + "/view/foot.html";
			String header = webRoot + "/view/head.html";
			StringBuilder res = new StringBuilder();
			readFile(header, res);

			StringBuilder html = new StringBuilder();
			Object[] values = res1.getValues();
			String file = res1.getHtmlFile();
			readFile(webRoot + file, html);
			res.append(MessageFormat.format(html.toString(), values));
			readFile(foot, res);
			writer.write(res.toString());
		} catch (Exception e) {
			log.error("request error", e);
			resp.sendError(500, e.getMessage());
		}
	}

	/**
	 * 添加新的后端服务
	 * 
	 * @param req
	 * @return
	 * @throws Exception
	 */
	private LBModelView doAddBanckendServer(Request req) throws Exception {
		String weightStr = req.getParameter("weight");
		String portStr = req.getParameter("port");
		String host = req.getParameter("host");

		Integer weight = Integer.valueOf(weightStr);
		Integer port = Integer.valueOf(portStr);

		dao.insert(String.format(LBSQL.add_glb, host, port, weight));
		LBBackendServer addr = new LBBackendServer(host, port, weight);
		proxy.addBackendServer(addr);

		JSONObject res = new JSONObject();
		res.put("info", "add success");
		res.put("code", 200);

		return new LBModelView(res.toJSONString(), true);
	}

	/**
	 * 查询路由日志
	 * 
	 * @return
	 * @throws Exception
	 */
	private LBModelView doReouteLogQuery() throws Exception {
		int size = 0;
		String line = null;
		String htmTpl = "/view/route_mgr.html";
		StringBuilder td = new StringBuilder();
		FileReader fr = new FileReader(proxyLogPath);
		BufferedReader br = new BufferedReader(fr);
		while ((line = br.readLine()) != null) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				td.append("<tr><td>").append(matcher.group()).append("</td>");
				while (matcher.find()) {
					td.append("<td>").append(matcher.group()).append("</td>");
				}
				td.append("</tr>");
				size++;
			}
		}
		br.close();
		fr.close();
		Object[] values = { td, size };
		return new LBModelView(htmTpl, values);
	}

	/**
	 * 查询后端服务列表
	 * 
	 * @return
	 * @throws Exception
	 */
	private LBModelView doBackendServerQuery() throws Exception {
		List<Object[]> resHosts = dao.query(LBSQL.query_host, 3);
		List<Object[]> globs = dao.query(LBSQL.query_glb, 2);
		String htmlTpl = "/view/host_mgr.html";
		StringBuilder td = new StringBuilder();
		for (Object[] objs : resHosts) {
			td.append("<tr>");
			for (Object obj : objs) {
				td.append("<td>").append(obj).append("</td>");
			}
			td.append("</tr>");
		}
		String roundModel = "";
		String hashModel = "";
		String lbPort = globs.get(0)[1].toString();
		String lbModel = globs.get(0)[0].toString();
		if (lbModel.equals("round")) {
			roundModel = "checked";
		} else {
			hashModel = "checked";
		}
		Object[] values = { td, hashModel, roundModel, lbPort };
		LBModelView result = new LBModelView(htmlTpl, values);
		return result;
	}

	private void readFile(String path, StringBuilder res) throws IOException {
		String line = null;
		FileReader fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		while ((line = br.readLine()) != null) {
			res.append(line);
		}
		br.close();
		fr.close();
	}
}
