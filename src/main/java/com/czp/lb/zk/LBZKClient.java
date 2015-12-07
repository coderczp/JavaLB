package com.czp.lb.zk;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Function:ZK操作管理类<br>
 *
 * Date :2015年12月7日 <br>
 * Author :coder_czp@126.com<br>
 * Copyright (c) 2015,coder_czp@126.com All Rights Reserved.
 */
public class LBZKClient implements Watcher {

	private ConcurrentHashMap<String, Watcher> watchers = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(LBZKClient.class);
	private volatile boolean isConnected;
	private int sessionTimeOut = 10000;
	private ZooKeeper zooKeeper;
	private String rootNode;
	private String server;

	public LBZKClient(String server, String rootNode) {
		this.rootNode = rootNode;
		this.server = server;
	}

	/**
	 * 连接zookeeper
	 * 
	 * @param host
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void connectZookeeper() {
		try {
			zooKeeper = new ZooKeeper(server, sessionTimeOut, this, true);
			while (!isConnected) {
				synchronized (this) {
					this.wait();
				}
			}
			log.info("dove has connected:" + zooKeeper);
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e);
		}

	}

	/**
	 * 实现watcher的接口方法，当连接zookeeper成功后，zookeeper会通过此方法通知watcher<br>
	 */
	@Override
	public void process(WatchedEvent event) {
		try {
			log.info("watcher received  event:" + event);
			KeeperState state = event.getState();
			if (state == KeeperState.SyncConnected) {
				synchronized (this) {
					isConnected = true;
					this.notifyAll();
				}
				registWatcher();
			} else if (state == KeeperState.Expired
					|| state == KeeperState.Disconnected) {
				log.error("zookeepoor is Disconnected,will reconnect");
				isConnected = false;
				closeConnect();
				connectZookeeper();
			}
		} catch (Exception e) {
			log.error("process event error", e);
		}
	}

	private void registWatcher() {
		for (Entry<String, Watcher> item : watchers.entrySet()) {
			Watcher value = item.getValue();
			String key = item.getKey();
			getData(key, value);
			// 重连后通知本地刷新数据
			value.process(new WatchedEvent(EventType.NodeDataChanged,
					KeeperState.SyncConnected, key));
			log.info("regist Watche after reconn:" + item);
		}

	}

	/**
	 * 根据路径创建节点，并且设置节点数据
	 * 
	 * @param path
	 * @param data
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public String createNode(String path, byte[] data, CreateMode mode)
			throws KeeperException, InterruptedException {
		return this.zooKeeper.create(path, data, Ids.OPEN_ACL_UNSAFE, mode);
	}

	/**
	 * 
	 * 检查节点是否存在
	 * 
	 * @param path
	 * @return
	 */
	public boolean isExist(String path) {
		try {
			Stat exists = this.zooKeeper.exists(path, false);
			return exists != null;
		} catch (Exception e) {
			log.error("check exist error:" + path, e);
			return false;
		}
	}

	/**
	 * 根据路径获取所有孩子节点
	 * 
	 * @param path
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public List<String> getChildren(String path) throws KeeperException,
			InterruptedException {
		return this.zooKeeper.getChildren(buildPath(path), false);
	}

	/**
	 * 更改节点数据
	 * 
	 * @param path
	 * @param data
	 * @param version
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public Stat setData(String path, byte[] data, int version)
			throws KeeperException, InterruptedException {
		return this.zooKeeper.setData(buildPath(path), data, version);
	}

	/**
	 * 根据路径获取节点数据并监听
	 * 
	 * @param path
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] getData(String path, Watcher watcher) {
		try {
			String buildPath = buildPath(path);
			watchers.put(buildPath, watcher);
			return this.zooKeeper.getData(buildPath, watcher, null);
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e.getCause());
		}
	}

	public byte[] getDataNotCacheWatcher(String path, Watcher watcher)
			throws KeeperException, InterruptedException {
		return this.zooKeeper.getData(buildPath(path), watcher, null);
	}

	/**
	 * 删除节点
	 * 
	 * @param path
	 * @param version
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public void deletNode(String path, int version)
			throws InterruptedException, KeeperException {
		this.zooKeeper.delete(buildPath(path), version);
	}

	/**
	 * 关闭zookeeper连接
	 * 
	 * @throws InterruptedException
	 */
	public void closeConnect() {
		if (null != zooKeeper) {
			try {
				zooKeeper.close();
			} catch (InterruptedException e) {
				log.error("close error", e);
			}
		}
	}

	public String getRootNode() {
		return rootNode;
	}

	private String buildPath(String path) {
		if (path.startsWith(rootNode))
			return path;
		if (path.startsWith("/"))
			return rootNode + path;
		else
			return rootNode + '/' + path;
	}

}
