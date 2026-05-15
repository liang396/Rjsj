package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class SeckillVoucherKafkaListener {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @KafkaListener(topics = "seckill.voucher.order", groupId = "hmdp-seckill-order-group")
    public void consumeOrder(String message) {
        VoucherOrder voucherOrder = JSONUtil.toBean(message, VoucherOrder.class);
        log.info("Received seckill order from kafka, orderId={}", voucherOrder.getId());
        voucherOrderService.handleVoucherOrder(voucherOrder);
    }
}
