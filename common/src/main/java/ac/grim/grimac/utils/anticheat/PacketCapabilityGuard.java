package ac.grim.grimac.utils.anticheat;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central guard for packet types whose 26.1 wire format has not been
 * confirmed via live capture.  Listeners/checks call {@link #isSafe}
 * before constructing wrappers for these packet types.
 *
 * When an unconfirmed packet arrives at runtime, the guard logs a
 * throttled warning instead of allowing a potentially crashing parse.
 */
public final class PacketCapabilityGuard {
    private static final boolean ENFORCE_GUARDS = false;

    private static final Set<PacketTypeCommon> UNCONFIRMED = ConcurrentHashMap.newKeySet();
    private static final AtomicLong LAST_WARN_MS = new AtomicLong(0);
    private static final long WARN_THROTTLE_MS = 30_000;

    static {
        addUnconfirmed(PacketType.Play.Client.CHAT_COMMAND);
        addUnconfirmed(PacketType.Play.Client.EDIT_BOOK);
        addUnconfirmed(PacketType.Play.Client.PICK_ITEM);
        addUnconfirmed(PacketType.Play.Client.SELECT_BUNDLE_ITEM);
        addUnconfirmed(PacketType.Play.Client.SPECTATE);
        addUnconfirmed(PacketType.Play.Client.STEER_VEHICLE);
        addUnconfirmed(PacketType.Play.Client.WINDOW_CONFIRMATION);
        addUnconfirmed(PacketType.Play.Server.ACKNOWLEDGE_PLAYER_DIGGING);
        addUnconfirmed(PacketType.Play.Server.CAMERA);
        addUnconfirmed(PacketType.Play.Server.COMBAT_EVENT);
        addUnconfirmed(PacketType.Play.Server.MAP_CHUNK_BULK);
        addUnconfirmed(PacketType.Play.Server.PLAYER_INFO);
        addUnconfirmed(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        addUnconfirmed(PacketType.Play.Server.PLAYER_ROTATION);
        addUnconfirmed(PacketType.Play.Server.SPAWN_LIVING_ENTITY);
        addUnconfirmed(PacketType.Play.Server.SPAWN_PAINTING);
        addUnconfirmed(PacketType.Play.Server.SPAWN_PLAYER);
        addUnconfirmed(PacketType.Play.Server.USE_BED);
        addUnconfirmed(PacketType.Play.Server.WINDOW_CONFIRMATION);
        addUnconfirmed(PacketType.Play.Server.WORLD_BORDER);
    }

    private PacketCapabilityGuard() {}

    private static void addUnconfirmed(PacketTypeCommon type) {
        if (type != null) UNCONFIRMED.add(type);
    }

    /**
     * Returns true when the packet type has been confirmed safe on 26.1.
     * Returns false (and logs a throttled warning) for unconfirmed types.
     */
    public static boolean isSafe(PacketTypeCommon packetType) {
        if (packetType == null) return false;
        if (!ENFORCE_GUARDS) return true;
        if (!UNCONFIRMED.contains(packetType)) return true;

        long now = System.currentTimeMillis();
        long last = LAST_WARN_MS.get();
        if (now - last > WARN_THROTTLE_MS && LAST_WARN_MS.compareAndSet(last, now)) {
            LogUtil.warn("[26.1-guard] Skipping unconfirmed packet branch: " + packetType.getName()
                    + " — wrapper parse suppressed to prevent crash.");
        }
        return false;
    }

    /**
     * Marks a packet type as confirmed safe at runtime (e.g. after
     * observing it decode cleanly).  Removes it from the unconfirmed set.
     */
    public static void markConfirmed(PacketTypeCommon packetType) {
        if (packetType == null) return;
        UNCONFIRMED.remove(packetType);
    }

    private static final ConcurrentHashMap<String, AtomicLong> LAST_PARSE_WARN_MS_BY_KEY = new ConcurrentHashMap<>();

    private static boolean shouldLogParseKey(String key) {
        AtomicLong lastRef = LAST_PARSE_WARN_MS_BY_KEY.computeIfAbsent(key, ignored -> new AtomicLong(0));
        long now = System.currentTimeMillis();
        long last = lastRef.get();
        return now - last > WARN_THROTTLE_MS && lastRef.compareAndSet(last, now);
    }

    /**
     * Logs a throttled warning when wrapper construction fails for an
     * unconfirmed packet.  Used as a fail-open safety net after
     * {@link #markConfirmed} promotes a packet type that still has
     * wire-format issues.
     */
    public static void logParseFailure(PacketTypeCommon packetType, Exception e) {
        String packetName = packetType == null ? "unknown" : packetType.getName();
        if (shouldLogParseKey("parse:" + packetName)) {
            LogUtil.warn("[26.1-guard] Wrapper parse failed for " + packetName
                    + ": " + e.getClass().getSimpleName() + " — branch skipped.");
        }
    }

    public static void logBranchFailure(String source, PacketTypeCommon packetType, Throwable throwable) {
        String packetName = packetType == null ? "unknown" : packetType.getName();
        String normalizedSource = source == null ? "unknown" : source;
        if (shouldLogParseKey("branch:" + normalizedSource + ":" + packetName)) {
            LogUtil.warn("[26.1-guard] Branch failed in " + normalizedSource
                    + " packet=" + packetName
                    + " cause=" + throwable.getClass().getSimpleName()
                    + ": " + throwable.getMessage());
        }
    }
}
