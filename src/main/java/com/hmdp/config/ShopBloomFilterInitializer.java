package com.hmdp.config;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.BloomFilterConstants.SHOP_BLOOM_EXPECTED_INSERTIONS;
import static com.hmdp.utils.BloomFilterConstants.SHOP_BLOOM_FALSE_PROBABILITY;
import static com.hmdp.utils.BloomFilterConstants.SHOP_BLOOM_FILTER_KEY;

@Slf4j
@Component
public class ShopBloomFilterInitializer {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IShopService shopService;

    @PostConstruct
    public void initShopBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(SHOP_BLOOM_FILTER_KEY);
        bloomFilter.tryInit(SHOP_BLOOM_EXPECTED_INSERTIONS, SHOP_BLOOM_FALSE_PROBABILITY);
        List<Shop> shops = shopService.list();
        for (Shop shop : shops) {
            bloomFilter.add(shop.getId());
        }
        log.info("Initialized shop bloom filter with {} shop ids", shops.size());
    }
}
