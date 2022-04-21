package com.stark.cache;

import com.stark.cache.lruCache.LRUCache;
import com.stark.cache.lruCache.SalePriceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.math.BigDecimal;

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

	private String buildKey(String mid, BigDecimal salePrice) {
		return mid + "_" + salePrice;
	}

}
