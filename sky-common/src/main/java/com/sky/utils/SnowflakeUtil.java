package com.sky.utils;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * 雪花算法 ID 生成器（简化版，适用于单机多实例）
 *
 * 替代 System.currentTimeMillis() 生成订单号，
 * 避免高并发下毫秒级重复。
 *
 * 结构（64 bits）：
 *   [41-bit timestamp] [10-bit worker] [12-bit sequence]
 */
public class SnowflakeUtil {

    private static final SnowflakeUtil INSTANCE = new SnowflakeUtil(generateWorkerId());

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static final long EPOCH = 1700000000000L;  // 2023-11-14 起
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private SnowflakeUtil(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId must be 0.." + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    public static long nextId() {
        return INSTANCE.next();
    }

    /**
     * 生成下一个 ID（String 格式，用于订单号）
     */
    public static String nextIdStr() {
        return String.valueOf(INSTANCE.next());
    }

    private synchronized long next() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for "
                    + (lastTimestamp - timestamp) + " ms");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 基于 MAC 地址生成 workerId（0 ~ 1023）。
     * 容器 / 同机多实例下保证不同 workerId。
     */
    private static long generateWorkerId() {
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        for (byte b : mac) {
                            sb.append(String.format("%02x", b));
                        }
                    }
                }
            }
            int hash = sb.toString().hashCode();
            return Math.abs((long) hash) & MAX_WORKER_ID;
        } catch (Exception e) {
            return new SecureRandom().nextInt((int) MAX_WORKER_ID + 1);
        }
    }
}
