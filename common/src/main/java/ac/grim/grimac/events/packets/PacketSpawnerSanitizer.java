package ac.grim.grimac.events.packets;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class PacketSpawnerSanitizer extends PacketListenerAbstract {
    // 26.1-only fork: use the server's latest mapping id path by design.
    // We intentionally do not support legacy/multi-version client id remapping here.
    private static final ClientVersion SERVER_CLIENT_VERSION =
            PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    private static final int MOB_SPAWNER_TYPE_ID = BlockEntityTypes.MOB_SPAWNER.getId(SERVER_CLIENT_VERSION);
    private static final boolean DEBUG_COORD_LOGGING = Boolean.getBoolean("grim.spawnerSanitizer.debug");
    private static final AtomicLong LAST_DEBUG_LOG_AT_MS = new AtomicLong(0L);
    private static final long DEBUG_LOG_INTERVAL_MS = 2000L;

    public PacketSpawnerSanitizer() {
        // Run after world cache listeners to avoid affecting Grim internals.
        super(PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ENTITY_DATA) {
            sanitizeBlockEntityData(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            sanitizeChunkData(event);
        }
    }

    private void sanitizeBlockEntityData(PacketSendEvent event) {
        try {
            WrapperPlayServerBlockEntityData wrapper = new WrapperPlayServerBlockEntityData(event);
            if (wrapper.getBlockEntityType() == BlockEntityTypes.MOB_SPAWNER) {
                // Hide raw spawner block-entity updates entirely from clients.
                event.setCancelled(true);
                if (DEBUG_COORD_LOGGING) {
                    debugLog("blocked BLOCK_ENTITY_DATA spawner update at "
                            + wrapper.getPosition().getX() + " "
                            + wrapper.getPosition().getY() + " "
                            + wrapper.getPosition().getZ());
                }
            }
        } catch (RuntimeException ex) {
            if (PacketDecodeUtils.isPacketDecodeDesync(ex)) {
                PacketDecodeUtils.logSuppressedDecode("PacketSpawnerSanitizer(BLOCK_ENTITY_DATA)", event.getPacketType(), ex);
                return;
            }
            LogUtil.error("Unexpected runtime failure in PacketSpawnerSanitizer(BLOCK_ENTITY_DATA)", ex);
            throw ex;
        }
    }

    private void sanitizeChunkData(PacketSendEvent event) {
        try {
            WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
            TileEntity[] tileEntities = wrapper.getColumn().getTileEntities();
            if (tileEntities.length == 0) {
                return;
            }

            List<TileEntity> filtered = new ArrayList<>(tileEntities.length);
            boolean removedSpawner = false;
            int removedCount = 0;
            for (TileEntity tileEntity : tileEntities) {
                if (tileEntity.getType() == MOB_SPAWNER_TYPE_ID) {
                    removedSpawner = true;
                    removedCount++;
                    continue;
                }
                filtered.add(tileEntity);
            }

            if (!removedSpawner) {
                return;
            }

            Column column = wrapper.getColumn();
            Column sanitizedColumn = new Column(
                    column.getX(),
                    column.getZ(),
                    column.isFullChunk(),
                    column.getChunks(),
                    filtered.toArray(new TileEntity[0]),
                    column.getHeightmaps()
            );
            wrapper.setColumn(sanitizedColumn);
            event.markForReEncode(true);
            if (DEBUG_COORD_LOGGING) {
                debugLog("stripped " + removedCount + " spawner tile entities from CHUNK_DATA chunk="
                        + column.getX() + "," + column.getZ());
            }
        } catch (RuntimeException ex) {
            if (PacketDecodeUtils.isPacketDecodeDesync(ex)) {
                PacketDecodeUtils.logSuppressedDecode("PacketSpawnerSanitizer(CHUNK_DATA)", event.getPacketType(), ex);
                return;
            }
            LogUtil.error("Unexpected runtime failure in PacketSpawnerSanitizer(CHUNK_DATA)", ex);
            throw ex;
        }
    }

    private void debugLog(String message) {
        long now = System.currentTimeMillis();
        long previous = LAST_DEBUG_LOG_AT_MS.get();
        if (now - previous < DEBUG_LOG_INTERVAL_MS) {
            return;
        }
        if (!LAST_DEBUG_LOG_AT_MS.compareAndSet(previous, now)) {
            return;
        }
        LogUtil.warn("[SpawnerSanitizer] " + message);
    }
}
