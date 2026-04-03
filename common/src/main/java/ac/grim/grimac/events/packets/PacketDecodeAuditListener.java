package ac.grim.grimac.events.packets;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.PacketSide;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTags;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight runtime decoder audit:
 * - no packet payload logging
 * - no NDJSON output
 * - validates wrapper decode path once per packet type (retry throttled on failures)
 */
public final class PacketDecodeAuditListener extends PacketListenerAbstract {
    private static final String ENABLE_ENV = "GRIM_DECODE_AUDIT";
    private static final String ENABLE_PROP = "grim.decodeAudit";
    private static final long RETRY_INTERVAL_MS = 30_000L;

    private static final Map<String, DecodeState> STATES = new ConcurrentHashMap<>();

    private static final class DecodeState {
        volatile boolean success;
        volatile long lastAttemptMs;
        volatile int failCount;
    }

    public PacketDecodeAuditListener() {
        super(PacketListenerPriority.MONITOR);
        LogUtil.info("[decode-audit] Enabled lightweight decode verification.");
    }

    private static boolean isTruthy(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase();
        return normalized.equals("1")
                || normalized.equals("true")
                || normalized.equals("yes")
                || normalized.equals("on");
    }

    public static boolean isEnabled() {
        return isTruthy(System.getProperty(ENABLE_PROP)) || isTruthy(System.getenv(ENABLE_ENV));
    }

    private static String packetKey(ProtocolPacketEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == null) return null;
        String side = type.getSide() == PacketSide.CLIENT ? "Client" : "Server";
        return side + "." + type.getName();
    }

    private static boolean shouldAttempt(String key) {
        DecodeState state = STATES.computeIfAbsent(key, k -> new DecodeState());
        if (state.success) return false;
        long now = System.currentTimeMillis();
        if (now - state.lastAttemptMs < RETRY_INTERVAL_MS) return false;
        state.lastAttemptMs = now;
        return true;
    }

    private static void markSuccess(String key) {
        DecodeState state = STATES.computeIfAbsent(key, k -> new DecodeState());
        if (!state.success) {
            state.success = true;
            LogUtil.info("[decode-audit] OK " + key);
        }
    }

    private static void markFailure(String key, Throwable throwable) {
        DecodeState state = STATES.computeIfAbsent(key, k -> new DecodeState());
        state.failCount++;
        if (state.failCount <= 3 || state.failCount % 10 == 0) {
            LogUtil.warn("[decode-audit] FAIL " + key + " (" + throwable.getClass().getSimpleName()
                    + "): " + throwable.getMessage());
        }
    }

    private static PacketWrapper<?> createWrapper(ProtocolPacketEvent event) throws Exception {
        PacketTypeCommon common = event.getPacketType();
        Class<? extends PacketWrapper<?>> wrapperClass = common.getWrapperClass();
        if (wrapperClass == null) {
            throw new IllegalStateException("wrapperClass=null");
        }
        for (Constructor<?> constructor : wrapperClass.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(event.getClass())) {
                Object wrapper = constructor.newInstance(event);
                if (wrapper instanceof PacketWrapper<?> packetWrapper) {
                    return packetWrapper;
                }
            }
        }
        throw new IllegalStateException("no matching constructor");
    }

    private static void forceRead(PacketWrapper<?> wrapper) {
        try {
            Method read = wrapper.getClass().getMethod("read");
            read.invoke(wrapper);
        } catch (Throwable ignored) {
        }
    }

    private static void probeGetters(PacketWrapper<?> wrapper) {
        int probed = 0;
        for (Method method : wrapper.getClass().getMethods()) {
            if (method.getParameterCount() != 0) continue;
            String name = method.getName();
            if (!(name.startsWith("get") || name.startsWith("is") || name.startsWith("has"))) continue;
            if (name.equals("getClass")
                    || name.equals("getPacketTypeData")
                    || name.equals("getServerVersion")
                    || name.equals("getClientVersion")
                    || name.equals("getBuffer")
                    || name.equals("getReadIndex")
                    || name.equals("getWriteIndex")) continue;
            try {
                method.invoke(wrapper);
                probed++;
            } catch (Throwable ignored) {
            }
            if (probed >= 8) break;
        }
    }

    private static void audit(ProtocolPacketEvent event) {
        String key = packetKey(event);
        if (key == null || !shouldAttempt(key)) return;
        try {
            if (event instanceof PacketSendEvent sendEvent
                    && (event.getPacketType() == PacketType.Play.Server.TAGS
                    || event.getPacketType() == PacketType.Configuration.Server.UPDATE_TAGS)) {
                // Mirror Grim's explicit path for tag packets.
                WrapperPlayServerTags tags = new WrapperPlayServerTags(sendEvent);
                tags.getTagMap();
                markSuccess(key);
                return;
            }

            PacketWrapper<?> wrapper = createWrapper(event);
            forceRead(wrapper);
            probeGetters(wrapper);
            markSuccess(key);
        } catch (Throwable throwable) {
            markFailure(key, throwable);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        audit(event);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        audit(event);
    }
}
