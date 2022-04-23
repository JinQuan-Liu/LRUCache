package com.stark.cache;

import com.stark.cache.lruCache.LRUCache;
import com.stark.cache.lruCache.SalePriceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
		LRUCache<String, SalePriceCache> cache = new LRUCache<>(16, 1000, 5);
		SalePriceCache cacheVo = new SalePriceCache();
		cacheVo.setMid("11111111");
		cacheVo.setSalePrice(new BigDecimal("23.5"));
		cache.put(buildKey(cacheVo.getMid(), cacheVo.getSalePrice()), cacheVo);
		System.out.println("cache：" + cache);
		System.out.println("first get key：" + cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())));

		// 判断过期机制是否生效
		Thread.sleep(6000);
		System.out.println(cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())));
		Assert.isNull(cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())), "参数不为空");
	}

	/**
	 * 判断存入数据，超过最大缓存空间，是否自动清理旧数据
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testMaxCacheSizeAutoClearTailNode() throws InterruptedException {
		LRUCache<String, SalePriceCache> cache = new LRUCache<>(1, 3, 100);

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

	@Test
	public void test1() {
		BigDecimal threshold = new BigDecimal("-0.1");
		BigDecimal handPrice = new BigDecimal("88.00");
		BigDecimal externalPrice = new BigDecimal("93.00");
		System.out.println(handPrice.subtract(externalPrice).divide(externalPrice, 2, RoundingMode.HALF_UP));
		System.out.println(
				handPrice.subtract(externalPrice).divide(externalPrice, 2, RoundingMode.HALF_UP).compareTo(threshold)
						<= 0);
	}

	/**
	 * 测试并发存取cache
	 *
	 */
	@Test
	public void test() throws InterruptedException {
		LRUCache<String, String> cache = new LRUCache<>(1, 1000, 10);

		CountDownLatch countDownLatch = new CountDownLatch(10000);
		long beginTime = new Date().getTime();
		// 存cache
		for (int i = 0; i < 10000; i++) {
			int finalI = i;
			Thread thread = new Thread(() -> {
				cache.put("" + finalI, "test");
				cache.get("" + finalI);
				countDownLatch.countDown();
			});
			thread.setName("thread_" + i);
			thread.start();
		}
		countDownLatch.await();
		long endTime = new Date().getTime();
		System.out.println("共耗时：" + (endTime - beginTime) + " 毫秒");
	}

	private String buildKey(String mid, BigDecimal salePrice) {
		return mid + "_" + salePrice;
	}

}
