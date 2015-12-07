package com.czp.lb.proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 
 * Function:后端服务的实体类<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */

public class LBBackendServer {

	private SocketAddress addr;
	private int weight;

	public LBBackendServer(String host, int port, int weight) {
		this.weight = weight;
		this.addr = new InetSocketAddress(host, port);
	}

	public int getWeight() {
		return weight;
	}

	public SocketAddress getAddr() {
		return addr;
	}

	@Override
	public int hashCode() {
		return 31 + ((addr == null) ? 0 : addr.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LBBackendServer other = (LBBackendServer) obj;
		if (addr == null) {
			if (other.addr != null)
				return false;
		} else if (!addr.equals(other.addr))
			return false;
		return true;
	}

}
