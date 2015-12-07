package com.czp.lb.web;

public interface LBSQL {

	String sql_host = "CREATE TABLE IF NOT EXISTS host_tbl(host varchar,port varchar,weight varchar,primary key(host,port));";
	String sql_glob = "CREATE TABLE IF NOT EXISTS glb_tbl (lb_type varchar,port varchar, primary key(port));";
	String sql_check = "SELECT name FROM sqlite_master where type='table' and name='host_tbl'";
	String add_glb = "INSERT INTO host_tbl (host,port,weight) VALUES('%s','%s','%s');";
	String sql_dft = "INSERT INTO glb_tbl (lb_type,port) VALUES('round','1025');";
	String query_host = "SELECT * FROM host_tbl";
	String query_glb = "SELECT * FROM glb_tbl";

}
