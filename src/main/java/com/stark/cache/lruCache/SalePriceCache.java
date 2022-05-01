package com.stark.cache.lruCache;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SalePriceCache {
	private String mid;
	private BigDecimal salePrice;
	private Date createTime;
}
