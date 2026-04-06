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

public class PacketSpawnerSanitizer extends PacketListenerAbstract {
    // 26.1-only fork: use the server's latest mapping id path by design.
    // We intentionally do not support legacy/multi-version client id remapping here.
    private static final ClientVersion SERVER_CLIENT_VERSION =
            PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    private static final int MOB_SPAWNER_TYPE_ID = BlockEntityTypes.MOB_SPAWNER.getId(SERVER_CLIENT_VERSION);

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
            for (TileEntity tileEntity : tileEntities) {
                if (tileEntity.getType() == MOB_SPAWNER_TYPE_ID) {
                    removedSpawner = true;
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
        } catch (RuntimeException ex) {
            if (PacketDecodeUtils.isPacketDecodeDesync(ex)) {
                PacketDecodeUtils.logSuppressedDecode("PacketSpawnerSanitizer(CHUNK_DATA)", event.getPacketType(), ex);
                return;
            }
            LogUtil.error("Unexpected runtime failure in PacketSpawnerSanitizer(CHUNK_DATA)", ex);
            throw ex;
        }
    }
}
