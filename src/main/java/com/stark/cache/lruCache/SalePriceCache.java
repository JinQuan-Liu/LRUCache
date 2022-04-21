package com.stark.cache.lruCache;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalePriceCache {
	private String mid;
	private BigDecimal salePrice;
}
