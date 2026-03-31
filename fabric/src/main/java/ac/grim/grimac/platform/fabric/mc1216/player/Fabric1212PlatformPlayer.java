package ac.grim.grimac.platform.fabric.mc1216.player;

import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.api.world.PlatformWorld;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import ac.grim.grimac.platform.fabric.utils.thread.FabricFutureUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.math.Location;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

public class Fabric1212PlatformPlayer extends Fabric1202PlatformPlayer {
    public Fabric1212PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public Sender getSender() {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory().wrap(fabricPlayer.createCommandSourceStack());
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        PlatformWorld world = location.getWorld();
        if (world == null || !(world instanceof ServerLevel targetLevel)) {
            return CompletableFuture.completedFuture(false);
        }
        return FabricFutureUtil.supplySync(() -> {
            try {
                // MC 1.21.2+: last boolean is the overload that matches server teleport API; differs from
                // Fabric1161PlatformPlayer (false) due to signature/behavior on older branches.
                fabricPlayer.teleportTo(
                        targetLevel,
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        EnumSet.noneOf(Relative.class),
                        location.getYaw(),
                        location.getPitch(),
                        true
                );
                if (fabricPlayer.level() != targetLevel) {
                    return false;
                }
                double epsilon = 1e-3;
                return Math.abs(fabricPlayer.getX() - location.getX()) <= epsilon
                        && Math.abs(fabricPlayer.getY() - location.getY()) <= epsilon
                        && Math.abs(fabricPlayer.getZ() - location.getZ()) <= epsilon
                        && Math.abs(fabricPlayer.getYRot() - location.getYaw()) <= 0.01f
                        && Math.abs(fabricPlayer.getXRot() - location.getPitch()) <= 0.01f;
            } catch (Exception e) {
                LogUtil.warn("teleportAsync failed for " + fabricPlayer.getScoreboardName(), e);
                return false;
            }
        });
    }
}
