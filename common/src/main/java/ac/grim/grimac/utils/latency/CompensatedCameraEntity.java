package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CompensatedCameraEntity extends Check implements PacketCheck {
    private final ArrayDeque<PacketEntity> entities = new ArrayDeque<>(1);
    private static final long CAMERA_LOG_THROTTLE_MS = 10_000L;
    private static final AtomicLong LAST_CAMERA_LOG_MS = new AtomicLong(0);

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

    private static boolean shouldLogCameraEvent() {
        long now = System.currentTimeMillis();
        long last = LAST_CAMERA_LOG_MS.get();
        return now - last > CAMERA_LOG_THROTTLE_MS && LAST_CAMERA_LOG_MS.compareAndSet(last, now);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (!isCameraPacket(packetType)) return;
        int camera;
        try {
            camera = new WrapperPlayServerCamera(event).getCameraId();
        } catch (Exception e) {
            return;
        }
        if (shouldLogCameraEvent()) {
            LogUtil.info("[camera-check] observed packet=" + packetType.getName() + " cameraId=" + camera);
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
