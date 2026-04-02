package ac.grim.grimac.events.packets;

import ac.grim.grimac.utils.anticheat.LogUtil;

import java.util.concurrent.atomic.AtomicLong;

final class PacketDecodeUtils {
    private static final long PACKET_DECODE_WARN_INTERVAL_MS = 10_000L;
    private static final AtomicLong LAST_PACKET_DECODE_WARN_AT = new AtomicLong(0L);

    private PacketDecodeUtils() {
    }

    static boolean isPacketDecodeDesync(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (!isDecodeThrowableType(current)) {
                continue;
            }

            final String message = String.valueOf(current.getMessage());
            if (hasKnownDecodeMarker(message) || hasKnownDecodeStack(current)) {
                return true;
            }
        }
        return false;
    }

    static void logSuppressedDecode(String source, Object packetType, Throwable throwable) {
        final long now = System.currentTimeMillis();
        final long previous = LAST_PACKET_DECODE_WARN_AT.get();
        if (now - previous < PACKET_DECODE_WARN_INTERVAL_MS) {
            return;
        }
        if (!LAST_PACKET_DECODE_WARN_AT.compareAndSet(previous, now)) {
            return;
        }

        LogUtil.warn("Suppressed PacketEvents decode exception in " + source
                + " packet=" + packetType
                + " cause=" + throwable.getClass().getSimpleName()
                + ": " + throwable.getMessage());
    }

    private static boolean isDecodeThrowableType(Throwable throwable) {
        return throwable instanceof IllegalStateException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof IndexOutOfBoundsException
                || throwable instanceof ArrayIndexOutOfBoundsException;
    }

    private static boolean hasKnownDecodeMarker(String message) {
        return message.contains("Unknown entity metadata type id")
                || message.contains("readerIndex(")
                || message.contains("writerIndex(")
                || message.contains("expected: range(")
                || message.contains("Can't find mapped entity")
                || message.contains("Can't resolve #")
                || message.contains("Unknown nbt type id")
                || message.contains("readableBytes()")
                || message.contains("unexpected EOF")
                || message.contains("out of bounds for length")
                || message.contains("dimensionType is null");
    }

    private static boolean hasKnownDecodeStack(Throwable throwable) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            final String className = element.getClassName();
            if (className.startsWith("com.github.retrooper.packetevents.wrapper.")
                    || className.startsWith("com.github.retrooper.packetevents.protocol.entity.")
                    || className.startsWith("com.github.retrooper.packetevents.protocol.nbt.")
                    || className.startsWith("com.github.retrooper.packetevents.util.mappings.")
                    || className.startsWith("com.github.retrooper.packetevents.netty.buffer.")
                    || className.startsWith("io.github.retrooper.packetevents.impl.netty.buffer.")
                    || className.startsWith("io.github.retrooper.packetevents.handler.")) {
                return true;
            }
        }
        return false;
    }
}
