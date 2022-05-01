package com.stark.cache.lruCache;

import java.util.Date;
import java.util.HashMap;

public class LRUCache<K, V> {
	// 最大分片数量，默认为16
	private final int MAX_NUMS_OF_SLICES;
	// 每个分片的缓存大小，默认为1000
	private final int MAX_SLICE_CACHE_SIZE;
	// 缓存过期时间，单位毫秒，默认60秒
	private final int EXPIRED_TIME;

	private HashMap<Integer, LRUHashMap<K, LRUNode<K, V>>> map;

	public LRUCache() {
		MAX_NUMS_OF_SLICES = 16;
		MAX_SLICE_CACHE_SIZE = 1000;
		EXPIRED_TIME = 60 * 1000;
		new LRUCache(MAX_NUMS_OF_SLICES, MAX_SLICE_CACHE_SIZE, EXPIRED_TIME);
	}

	public LRUCache(int maxSliesNum, int maxSliceCacheSize, int expiredTime) {
		if (maxSliesNum < 1)
			maxSliesNum = 16;
		MAX_NUMS_OF_SLICES = maxSliesNum;
		if (maxSliceCacheSize < 1)
			maxSliceCacheSize = 1000;
		MAX_SLICE_CACHE_SIZE = maxSliceCacheSize;
		if (expiredTime < 1)
			expiredTime = 60;
		EXPIRED_TIME = expiredTime * 1000;

		map = new HashMap<Integer, LRUHashMap<K, LRUNode<K, V>>>();

		// 初始化分片
		for (int i = 0; i < MAX_NUMS_OF_SLICES; i++) {
			LRUHashMap<K, LRUNode<K, V>> slice = new LRUHashMap<>(MAX_SLICE_CACHE_SIZE);
			map.put(i, slice);
		}
	}

	public void put(K key, V value) {
		LRUHashMap<K, LRUNode<K, V>> sliceMap = switchToSlice(key);

		LRUNode<K, V> node = new LRUNode<>();
		node.setKey(key);
		node.setValue(value);

		synchronized (sliceMap) {
			System.out.println(Thread.currentThread().getName() + " begin put key");
			// 如果已存在该key, 更新value值
			LRUNode<K, V> existNode = sliceMap.get(key);
			if (null != existNode) {
				existNode.setValue(value);
				return;
			}

			// 缓存达到预设上限，删除尾节点，添加新元素到头节点;没达到上限，直接添加为头节点
			if (sliceMap.size() >= MAX_SLICE_CACHE_SIZE) {
				removeTailNode(sliceMap);
			}
			addToHead(sliceMap, node);
			System.out.println(Thread.currentThread().getName() + " end put key");
		}
	}

	public V get(K key) {
		LRUHashMap<K, LRUNode<K, V>> sliceMap = switchToSlice(key);

		System.out.println(Thread.currentThread().getName() + " begin get key");

		// 1.获取节点
		LRUNode<K, V> node = sliceMap.get(key);
		if (null != node) {
			// 2.将该节点移动为头结点
			synchronized (sliceMap) {
				// 安全操作，已被其他线程删除，直接返回
				if (sliceMap.get(key) == null)
					return node.getValue();
				if (sliceMap.getHead().equals(sliceMap.getTail())) {
				} else if (sliceMap.getTail() != null && sliceMap.getTail().equals(node)) {
					// 如果是尾节点
					LRUNode<K, V> preTail = node.getPre();
					preTail.setNext(null);
					sliceMap.setTail(preTail);
					node.setPre(null);
					sliceMap.getHead().setPre(node);
					node.setNext(sliceMap.getHead());
					sliceMap.setHead(node);
				} else if (sliceMap.getHead() != null && sliceMap.getHead().equals(node)) {
				} else {
					// 如果是中间节点
					LRUNode<K, V> preNode = node.getPre();
					LRUNode<K, V> nextNode = node.getNext();
					nextNode.setPre(preNode);
					preNode.setNext(nextNode);

					sliceMap.getHead().setPre(node);
					node.setNext(sliceMap.getHead());
					node.setPre(null);
					sliceMap.setHead(node);
				}
			}
			System.out.println(Thread.currentThread().getName() + " end get key");
			return node.getValue();
		}
		System.out.println(Thread.currentThread().getName() + " end get key");
		return null;
	}

	public void removeExpired(K key) {
		LRUHashMap<K, LRUNode<K, V>> sliceMap = switchToSlice(key);

		System.out.println(Thread.currentThread().getName() + " begin remove key");

		// 1.获取节点
		LRUNode<K, V> node = sliceMap.get(key);
		if (null != node) {
			synchronized (sliceMap) {
				if (sliceMap.get(key) == null)
					return;
				sliceMap.remove(key);
				removeNode(sliceMap, node);
			}
		}
		System.out.println(Thread.currentThread().getName() + " end remove key");
	}

	public boolean isExpired(Date createTime) {
		long now = new Date().getTime();
		long createTimeValue = createTime.getTime();
		return now - createTimeValue >= EXPIRED_TIME;
	}

	private LRUHashMap<K, LRUNode<K, V>> switchToSlice(K key) {
		int slice = (key.hashCode() & Integer.MAX_VALUE) % MAX_NUMS_OF_SLICES;
		return map.get(slice);
	}

	private void addToHead(LRUHashMap<K, LRUNode<K, V>> sliceMap, LRUNode<K, V> node) {
		if (null == sliceMap.getHead()) {
			sliceMap.setHead(node);
			sliceMap.setTail(node);
			sliceMap.put(node.getKey(), node);
		} else {
			LRUNode<K, V> head = sliceMap.getHead();
			head.setPre(node);
			node.setNext(head);
			sliceMap.setHead(node);
			sliceMap.put(node.getKey(), node);
		}
	}

	private void removeNode(LRUHashMap<K, LRUNode<K, V>> sliceMap, LRUNode<K, V> node) {
		if (sliceMap.getHead() == null || sliceMap.getTail() == null)
			return;
		// 如果是尾节点
		if (sliceMap.getTail() != null && sliceMap.getTail().equals(node)) {
			removeTailNode(sliceMap);
			return;
		}
		// 如果是头节点
		if (sliceMap.getHead() != null && sliceMap.getHead().equals(node)) {
			removeHeadNode(sliceMap);
			return;
		}
		// 如果是中间节点
		LRUNode<K, V> preNode = node.getPre();
		LRUNode<K, V> nextNode = node.getNext();
		nextNode.setPre(preNode);
		preNode.setNext(nextNode);
		node.setNext(null);
		node.setPre(null);
		sliceMap.remove(node.getKey());
	}

	private void removeHeadNode(LRUHashMap<K, LRUNode<K, V>> sliceMap) {
		if (null == sliceMap.getHead()) {
			return;
		}
		LRUNode<K, V> head = sliceMap.getHead();
		if (head.equals(sliceMap.getTail())) {
			head.setPre(null);
			head.setNext(null);
			sliceMap.setHead(null);
			sliceMap.setTail(null);
			sliceMap.remove(head.getKey());
			return;
		}
		LRUNode<K, V> nextHead = head.getNext();
		if (nextHead != null) {
			nextHead.setPre(null);
		}
		head.setPre(null);
		head.setNext(null);
		sliceMap.setHead(nextHead);
		sliceMap.remove(head.getKey());
	}

	private void removeTailNode(LRUHashMap<K, LRUNode<K, V>> sliceMap) {
		if (null == sliceMap.getTail()) {
			return;
		}
		LRUNode<K, V> tail = sliceMap.getTail();
		if (tail.equals(sliceMap.getHead())) {
			tail.setNext(null);
			tail.setPre(null);
			sliceMap.setHead(null);
			sliceMap.setTail(null);
			sliceMap.remove(tail.getKey());
			return;
		}
		LRUNode<K, V> preTail = tail.getPre();
		if (preTail != null) {
			preTail.setNext(null);
		}
		tail.setNext(null);
		tail.setPre(null);
		sliceMap.setTail(preTail);
		sliceMap.remove(tail.getKey());
	}

	static class LRUHashMap<K, LRUNode> extends HashMap<K, LRUNode> {
		private LRUNode head;
		private LRUNode tail;

		public LRUHashMap(int initialCapacity) {
			super(initialCapacity);
		}

		public LRUNode getHead() {
			return head;
		}

		public void setHead(LRUNode head) {
			this.head = head;
		}

		public LRUNode getTail() {
			return tail;
		}

		public void setTail(LRUNode tail) {
			this.tail = tail;
		}
	}

	static class LRUNode<K, V> {
		private LRUNode<K, V> pre;
		private LRUNode<K, V> next;
		private K key;
		private V value;

		public LRUNode<K, V> getPre() {
			return pre;
		}

		public void setPre(LRUNode<K, V> pre) {
			this.pre = pre;
		}

		public LRUNode<K, V> getNext() {
			return next;
		}

		public void setNext(LRUNode<K, V> next) {
			this.next = next;
		}

		public K getKey() {
			return key;
		}

		public void setKey(K key) {
			this.key = key;
		}

		public V getValue() {
			return value;
		}

		public void setValue(V value) {
			this.value = value;
		}
	}
}
