package ac.grim.grimac.platform.fabric.mc1205.convert;

import ac.grim.grimac.platform.fabric.utils.convert.FabricItemStackConversion;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.mojang.serialization.JsonOps;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.ComponentSerialization;

public class Fabric1205ConversionUtil implements IFabricConversionUtil {
    public ItemStack fromFabricItemStack(net.minecraft.world.item.ItemStack fabricStack) {
        return FabricItemStackConversion.peItemStackFromNative(fabricStack);
    }

    /**
     * Codec parse uses JsonOps only; registry-heavy component features may be limited until full
     * registry-backed parse is wired for this path.
     */
    public net.minecraft.network.chat.Component toNativeText(Component component) {
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, GsonComponentSerializer.gson().serializeToTree(component))
                    .getOrThrow();
        } catch (RuntimeException e) {
            LogUtil.error(
                    "Failed to parse Adventure Component to native (invalid JSON / codec): "
                            + String.valueOf(component),
                    e);
            return net.minecraft.network.chat.Component.literal("");
        }
    }
}
