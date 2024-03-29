package com.stark.cache;

import com.stark.cache.lruCache.LruCache;
import com.stark.cache.lruCache.SalePriceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

@SpringBootTest
class CacheApplicationTests {

	@Test
	void contextLoads() {
	}

	/**
	 * 判断存取数据是否正常、过期机制是否正常
	 * @throws InterruptedException
	 */
	@Test
	public void testLRUCache() throws InterruptedException {
		LruCache<String, SalePriceCache> cache = new LruCache<>(16, 1000, 5);
		SalePriceCache cacheVo = new SalePriceCache();
		cacheVo.setMid("11111111");
		cacheVo.setSalePrice(new BigDecimal("23.5"));
		cacheVo.setCreateTime(new Date());
		cache.put(buildKey(cacheVo.getMid(), cacheVo.getSalePrice()), cacheVo);
		System.out.println("cache：" + cache);
		System.out.println("first get key：" + cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())));

		// 判断过期机制是否生效

		Thread.sleep(7000);
		cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice()));
		Thread.sleep(2000);
		System.out.println();
		Assert.isNull(cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())), "参数不为空");
	}

	/**
	 * 判断存入数据，超过最大缓存空间，是否自动清理旧数据
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testMaxCacheSizeAutoClearTailNode() throws InterruptedException {
		LruCache<String, SalePriceCache> cache = new LruCache<>(1, 3, 100);

		for (int i = 0; i < 4; i++) {
			SalePriceCache cacheVo = new SalePriceCache();
			cacheVo.setMid("" + i);
			cacheVo.setSalePrice(new BigDecimal("23.5"));
			cache.put(buildKey(cacheVo.getMid(), cacheVo.getSalePrice()), cacheVo);

			if (i == 3) {
				Assert.isTrue(cache.get(buildKey("" + 0, cacheVo.getSalePrice())) == null, "没有自动清理旧数据");
			}
		}
	}

	/**
	 * 测试并发存取cache
	 *
	 */
	@Test
	public void test() throws InterruptedException {
		LruCache<String, String> cache = new LruCache<>(4, 400, 10);
		// 存cache
		for (int i = 0; i < 1600; i++) {
			int finalI = i;
			Thread thread = new Thread(() -> {
				cache.put("" + finalI, "test");
			});
			thread.setName("thread_" + i);
			thread.start();
		}

		// 100组数据，统计平均耗时
		long sum = 0;
		for (int j = 0; j < 100; j++) {
			CountDownLatch countDownLatch = new CountDownLatch(1600);
			long beginTime = new Date().getTime();
			// 取cache
			for (int i = 0; i < 1600; i++) {
				int finalI = i;
				Thread thread = new Thread(() -> {
					cache.get("" + finalI);
					countDownLatch.countDown();
				});
				thread.setName("thread_" + i);
				thread.start();
			}
			countDownLatch.await();
			long endTime = new Date().getTime();
			sum = sum + (endTime - beginTime);
		}

		long beginTime = new Date().getTime();
		for (int i = 0; i < 2000; i++) {
			cache.get("" + i);
		}
		long endTime = new Date().getTime();
		System.out.println("纯调用平均耗时：" + (endTime - beginTime) * 1.0 / 2000);
		System.out.println("总耗时：" + (sum / 100));
	}

	private String buildKey(String mid, BigDecimal salePrice) {
		return mid + "_" + salePrice;
	}

}
