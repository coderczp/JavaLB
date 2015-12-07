package com.czp.lb.web;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Function:负载均衡器的持久化信息操作类<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
public class LBDao {

	private String dbName;

	public LBDao(String dbName) {
		this.dbName = dbName;
	}

	public Connection getConn() throws Exception {
		Class.forName("org.sqlite.JDBC");
		return DriverManager.getConnection("jdbc:sqlite:" + dbName);
	}

	/**
	 * 创建table,sql必须要是create语句
	 * 
	 * @param sql
	 * @return true|false
	 * @throws Exception
	 */
	public boolean createTable(String sql) throws Exception {
		Connection conn = getConn();
		Statement st = conn.createStatement();
		boolean res = st.execute(sql);
		conn.close();
		return res;
	}

	/***
	 * 执行SQL查询
	 * 
	 * @param sql
	 *            SQL查询语句
	 * @param colCount
	 *            查询的列总数
	 * @return
	 * @throws Exception
	 */
	public List<Object[]> query(String sql, int colCount) throws Exception {
		Connection conn = getConn();
		Statement st = conn.createStatement();
		ResultSet res = st.executeQuery(sql);
		List<Object[]> all = new ArrayList<Object[]>();
		while (res.next()) {
			Object[] arr = new Object[colCount];
			for (int i = 1; i <= colCount; i++) {
				arr[i - 1] = res.getObject(i);
			}
			all.add(arr);
		}
		st.close();
		conn.close();
		return all;
	}

	/**
	 * 执行插入
	 * 
	 * @param sql
	 * @return
	 * @throws Exception
	 */
	public boolean insert(String sql) throws Exception {
		Connection conn = getConn();
		Statement st = conn.createStatement();
		int res = st.executeUpdate(sql);
		st.close();
		conn.close();
		return res > 0;
	}

	public boolean delete(String sql) throws Exception {
		return insert(sql);
	}
}
