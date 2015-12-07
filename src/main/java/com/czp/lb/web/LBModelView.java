package com.czp.lb.web;

/**
 * 
 * Function:请求结果的简单封装<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
public class LBModelView {

	/**
	 * 替换htmlFile里{x}的具体值
	 */
	private Object[] values;

	/**
	 * 执行逻辑要读取那个文件返回客户端
	 */
	private String htmlFile;

	/**
	 * 如果是true,会将htmlFile以json格式回写客户端
	 */
	private boolean isJson;

	public LBModelView(String htmlFile, Object[] values) {
		this.htmlFile = htmlFile;
		this.values = values;
	}

	public LBModelView(String htmlFile, boolean isJson) {
		this.htmlFile = htmlFile;
		this.isJson = isJson;
	}

	public String getHtmlFile() {
		return htmlFile;
	}

	public Object[] getValues() {
		return values;
	}

	public boolean isJson() {
		return isJson;
	}

}
