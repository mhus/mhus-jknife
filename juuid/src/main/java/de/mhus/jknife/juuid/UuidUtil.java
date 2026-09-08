/*

    Copyright (C) 2002 Mike Hummel (mh@mhus.de)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

*/
package de.mhus.jknife.juuid;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * UUID helpers: v7 generation (RFC 9562) with a millisecond timestamp prefix, so UUIDs are sortable by creation time.
 */
public final class UuidUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** state for monotonic v7 generation within the same millisecond (RFC 9562) */
    private static long lastMs = -1;
    private static final byte[] lastRand = new byte[10];

    private UuidUtil() {
    }

    /**
     * Generates a UUIDv7: 48 bit unix timestamp (ms) + version + random bits. Multiple generations within the same
     * millisecond use a monotonic counter, so the UUIDs stay sortable by creation time (RFC 9562 monotonicity).
     */
    public static synchronized UUID generateV7() {
        byte[] value = new byte[16];

        long ts = System.currentTimeMillis();
        byte[] rand;
        if (ts == lastMs) {
            increment(lastRand);
            rand = lastRand;
        } else {
            rand = new byte[10];
            RANDOM.nextBytes(rand);
            System.arraycopy(rand, 0, lastRand, 0, 10);
            lastMs = ts;
        }
        System.arraycopy(rand, 0, value, 6, 10);

        // first 48 bits: unix timestamp in milliseconds
        value[0] = (byte) (ts >>> 40);
        value[1] = (byte) (ts >>> 32);
        value[2] = (byte) (ts >>> 24);
        value[3] = (byte) (ts >>> 16);
        value[4] = (byte) (ts >>> 8);
        value[5] = (byte) ts;
        // version 7
        value[6] = (byte) ((value[6] & 0x0f) | 0x70);
        // RFC 9562 variant (10xx)
        value[8] = (byte) ((value[8] & 0x3f) | 0x80);

        ByteBuffer buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    /** big-endian increment of the 10 random bytes */
    private static void increment(byte[] b) {
        for (int i = b.length - 1; i >= 0; i--) {
            if (++b[i] != 0)
                break; // no carry
        }
    }

    /**
     * Extracts the embedded creation timestamp (milliseconds) of a UUIDv7.
     */
    public static Instant v7Timestamp(UUID uuid) {
        long ms = uuid.getMostSignificantBits() >>> 16;
        return Instant.ofEpochMilli(ms);
    }
}
