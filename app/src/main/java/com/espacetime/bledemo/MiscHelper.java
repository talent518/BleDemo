package com.espacetime.bledemo;

import android.support.annotation.NonNull;

import java.util.UUID;

/**
 * Created by abao on 2017/7/7.
 */

public abstract class MiscHelper {
    private static final String UUID_LONG_STYLE_PREFIX = "0000";
    private static final String UUID_LONG_STYLE_POSTFIX = "-0000-1000-8000-00805F9B34FB";

    public static UUID fromShortValue(final int uuidShortValue) {
        return UUID.fromString(UUID_LONG_STYLE_PREFIX + String.format("%04X", uuidShortValue & 0xffff) + UUID_LONG_STYLE_POSTFIX);
    }

    /**
     * check if full style or short (16bits) style UUID matches
     *
     * @param src the UUID to be compared
     * @param dst the UUID to be compared
     * @return true if the both of UUIDs matches
     */
    public static boolean matches(@NonNull final UUID src, @NonNull final UUID dst) {
        if (isShortUuid(src) || isShortUuid(dst)) {
            // at least one instance is short style: check only 16bits
            final long srcShortUUID = src.getMostSignificantBits() & 0x0000ffff00000000L;
            final long dstShortUUID = dst.getMostSignificantBits() & 0x0000ffff00000000L;

            return srcShortUUID == dstShortUUID;
        } else {
            return src.equals(dst);
        }
    }

    /**
     * Check if the specified UUID style is short style.
     *
     * @param src the UUID
     * @return true if the UUID is short style
     */
    private static boolean isShortUuid(@NonNull final UUID src) {
        return (src.getMostSignificantBits() & 0xffff0000ffffffffL) == 0L && src.getLeastSignificantBits() == 0L;
    }

    public static String bytes2hex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte i : bytes) {
            sb.append(" ");
            sb.append(String.format("%02x", i));
            if (i >= 0x21 && i <= 0x7e) {
                sb.append(String.format("(%c)", i));
            }
        }

        return sb.substring(1);
    }
}
