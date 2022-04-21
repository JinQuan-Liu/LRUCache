package com.stark.cache;

import com.stark.cache.lruCache.LRUCache;
import com.stark.cache.lruCache.SalePriceCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class CacheApplicationTests {

	@Test
	void contextLoads() {
	}

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
		System.out.println("cache：" + cache);
		System.out.println("first get key：" + cache.get(buildKey(cacheVo.getMid(), cacheVo.getSalePrice())));

		// 判断
	}

	private String buildKey(String mid, BigDecimal salePrice) {
		return mid + "_" + salePrice;
	}

}
