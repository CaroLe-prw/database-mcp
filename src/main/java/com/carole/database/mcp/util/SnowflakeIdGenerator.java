package com.carole.database.mcp.util;

import java.util.Random;

import lombok.Getter;

/**
 * Snowflake ID generator utility
 * 
 * This utility generates unique distributed IDs using Twitter's Snowflake algorithm. The generated IDs are 64-bit
 * integers with the following structure: - 1 bit: unused (always 0) - 41 bits: timestamp (milliseconds since custom
 * epoch) - 10 bits: machine/worker ID - 12 bits: sequence number
 * 
 * @author CaroLe
 * @Date 2025/7/8
 * @Description Snowflake distributed ID generator for database test data
 */
@Getter
public class SnowflakeIdGenerator {

    // Snowflake ID structure: timestamp(41 bits) + machine(10 bits) + sequence(12 bits)
    private static final long EPOCH = 1609459200000L;
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private static final Random RANDOM = new Random();

    /**
     * Constructor with auto-generated machine ID
     */
    public SnowflakeIdGenerator() {
        // Use a simple machine ID based on current time and random
        this.machineId = (System.currentTimeMillis() + RANDOM.nextInt(1000)) & MAX_MACHINE_ID;
    }

    /**
     * Constructor with specified machine ID
     * 
     * @param machineId Machine/Worker ID (0-1023)
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("Machine ID must be between 0 and " + MAX_MACHINE_ID);
        }
        this.machineId = machineId;
    }

    /**
     * Generate next unique ID
     * 
     * @return 64-bit unique ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (machineId << MACHINE_ID_SHIFT) | sequence;
    }

    /**
     * Generate next unique ID as string
     * 
     * @return String representation of unique ID
     */
    public String nextIdAsString() {
        return String.valueOf(nextId());
    }

    /**
     * Wait for next millisecond
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

}