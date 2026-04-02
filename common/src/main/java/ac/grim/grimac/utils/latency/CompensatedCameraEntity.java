package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import ac.grim.grimac.utils.anticheat.PacketCapabilityGuard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class CompensatedCameraEntity extends Check implements PacketCheck {
    private final ArrayDeque<PacketEntity> entities = new ArrayDeque<>(1);

    public CompensatedCameraEntity(GrimPlayer player) {
        super(player);
        entities.add(player.compensatedEntities.self);
    }

    private static boolean isCameraPacket(PacketTypeCommon packetType) {
        if (packetType == null) return false;
        if (packetType == PacketType.Play.Server.CAMERA) return true;
        String name = packetType.getName();
        return "CAMERA".equals(name) || "SET_CAMERA".equals(name);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (!isCameraPacket(packetType)) return;
        if (!PacketCapabilityGuard.isSafe(packetType)) return;
        int camera;
        try {
            camera = new WrapperPlayServerCamera(event).getCameraId();
        } catch (Exception e) {
            PacketCapabilityGuard.logParseFailure(packetType, e);
            return;
        }
        player.sendTransaction();

        player.addRealTimeTaskNow(() -> {
            PacketEntity entity = player.compensatedEntities.getEntity(camera);
            if (entity != null) {
                entities.add(entity);
            }
        });

        player.addRealTimeTaskNext(() -> {
            while (entities.size() > 1) {
                entities.poll();
            }

            if (entities.isEmpty()) {
                entities.add(player.compensatedEntities.self);
            }
        });
    }

    public boolean isSelf() {
        PacketEntity self = player.compensatedEntities.self;
        for (PacketEntity entity : entities) {
            if (entity != self) {
                return false;
            }
        }

        return true;
    }

    public List<PacketEntity> getPossibilities() {
        return new ArrayList<>(entities);
    }
}
