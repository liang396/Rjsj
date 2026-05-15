package com.hmdp.utils;

public final class BloomFilterConstants {

    private BloomFilterConstants() {
    }

    public static final String SHOP_BLOOM_FILTER_KEY = "bloom:shop:id";
    public static final long SHOP_BLOOM_EXPECTED_INSERTIONS = 100000L;
    public static final double SHOP_BLOOM_FALSE_PROBABILITY = 0.01D;
}
