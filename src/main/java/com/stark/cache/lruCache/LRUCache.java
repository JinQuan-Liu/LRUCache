package com.stark.cache.lruCache;

import java.lang.annotation.Native;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUCache<K, V> {
	// 最大分片数量，默认为16
	private final int MAX_NUMS_OF_SLICES;
	// 每个分片的缓存大小，默认为1000
	private final int MAX_SLICE_CACHE_SIZE;

	private HashMap<Integer, LRUHashMap<K, V>> map;

	public LRUCache(int maxSliesNum, int maxSliceCacheSize) {
		if (maxSliesNum < 1) maxSliesNum = 16;
		MAX_NUMS_OF_SLICES = maxSliesNum;
		if (maxSliceCacheSize < 1) maxSliceCacheSize = 1000;
		MAX_SLICE_CACHE_SIZE = maxSliceCacheSize;

		map = new HashMap();

		// 初始化分片
		for (int i = 0; i < MAX_NUMS_OF_SLICES; i++) {
			LRUHashMap<K, V> slice = new LRUHashMap<>(MAX_SLICE_CACHE_SIZE);
			map.put(i, slice);
		}
	}

	public void put(K key, V value) {
		LRUHashMap<K, V> sliceMap = switchToSlice(key);

		synchronized (sliceMap) {
			LRUNode<K, V> node = new LRUNode<>();
			node.setKey(key);
			node.setValue(value);

			// 缓存达到预设上限，删除尾节点，添加新元素到头节点;没达到上限，直接添加为头节点
			if (sliceMap.size() >= MAX_SLICE_CACHE_SIZE) {
				removeTailNode(sliceMap);
			}
			addToHead(sliceMap, node);
		}
	}

	public V get(K key) {
		LRUHashMap<K, V> sliceMap = switchToSlice(key);

		synchronized (sliceMap) {
			if (sliceMap.containsKey(key)) {
				// TODO
			}
		}

		return null;
	}

	private LRUHashMap<K, V> switchToSlice(K key) {
		int slice = (key.hashCode() & Integer.MAX_VALUE) % MAX_NUMS_OF_SLICES;
		return map.get(slice);
	}

	private void addToHead(LRUHashMap<K, V> sliceMap, LRUNode<K, V> node) {
		if (null == sliceMap.getHead()) {
			sliceMap.setHead(node);
			sliceMap.setTail(node);
		} else {
			LRUNode<K, V> head = sliceMap.getHead();
			head.setPre(node);
			node.setNext(head);
			sliceMap.setHead(node);
		}
	}

	private void removeTailNode(LRUHashMap<K, V> sliceMap) {
		if (null == sliceMap.getTail()) {
			return;
		}
		LRUNode<K, V> tail = sliceMap.getTail();
		if (tail.equals(sliceMap.getHead())) {
			tail.setNext(null);
			tail.setPre(null);
			sliceMap.setHead(null);
			sliceMap.setTail(null);
			return;
		}
		LRUNode<K, V> preTail = tail.getPre();
		if (preTail != null) {
			preTail.setNext(null);
		}
		tail.setNext(null);
		tail.setPre(null);
		sliceMap.setTail(preTail);
	}

	class LRUHashMap<K, V> extends HashMap<K, V> {
		private LRUNode<K, V> head;
		private LRUNode<K, V> tail;

		public LRUHashMap(int initialCapacity) {
			super(initialCapacity);
		}

		public LRUNode<K, V> getHead() {
			return head;
		}

		public void setHead(LRUNode<K, V> head) {
			this.head = head;
		}

		public LRUNode<K, V> getTail() {
			return tail;
		}

		public void setTail(LRUNode<K, V> tail) {
			this.tail = tail;
		}
	}

	class LRUNode<K, V> {
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
