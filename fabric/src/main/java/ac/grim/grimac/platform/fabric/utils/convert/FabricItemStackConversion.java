package ac.grim.grimac.platform.fabric.utils.convert;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared native ItemStack → PacketEvents item conversion (STREAM_CODEC + PacketWrapper).
 */
public final class FabricItemStackConversion {
    private static final long CONVERSION_WARN_THROTTLE_MS = 10_000L;
    private static final AtomicLong LAST_CONVERSION_WARN_MS = new AtomicLong(0L);

    private FabricItemStackConversion() {}

    public static ItemStack peItemStackFromNative(net.minecraft.world.item.ItemStack fabricStack) {
        if (fabricStack == null || fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            RegistryAccess registryManager = GrimACFabricLoaderPlugin.FABRIC_SERVER.registryAccess();
            RegistryFriendlyByteBuf registryByteBuf = new RegistryFriendlyByteBuf(buffer, registryManager);
            net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(registryByteBuf, fabricStack);
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer);
            return wrapper.readItemStack();
        } catch (Exception e) {
            ItemStack fallback = toSimpleFallback(fabricStack);
            if (!fallback.isEmpty()) {
                logFailureOnce("Failed to fully decode ItemStack; using simple fallback type/count for "
                        + BuiltInRegistries.ITEM.getKey(fabricStack.getItem())
                        + " x" + fabricStack.getCount()
                        + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
                return fallback;
            }
            logFailureOnce("Failed to decode ItemStack and fallback mapping for "
                    + BuiltInRegistries.ITEM.getKey(fabricStack.getItem())
                    + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            return ItemStack.EMPTY;
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private static ItemStack toSimpleFallback(net.minecraft.world.item.ItemStack fabricStack) {
        if (fabricStack == null || fabricStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String itemKey = String.valueOf(BuiltInRegistries.ITEM.getKey(fabricStack.getItem()));
        ItemType itemType = ItemTypes.getByName(itemKey);
        if (itemType == null) {
            return ItemStack.EMPTY;
        }
        return ItemStack.builder()
                .type(itemType)
                .amount(Math.max(1, fabricStack.getCount()))
                .build();
    }

    private static void logFailureOnce(String message) {
        long now = System.currentTimeMillis();
        long previous = LAST_CONVERSION_WARN_MS.get();
        if (now - previous < CONVERSION_WARN_THROTTLE_MS) {
            return;
        }
        if (!LAST_CONVERSION_WARN_MS.compareAndSet(previous, now)) {
            return;
        }
        LogUtil.warn(message);
    }
}
