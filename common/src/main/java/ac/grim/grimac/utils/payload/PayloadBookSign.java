package ac.grim.grimac.utils.payload;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

public record PayloadBookSign(ItemStack itemStack) {
    public static final PayloadCodec<PayloadBookSign> CODEC = new PayloadCodec<>(
            wrapper -> new PayloadBookSign(wrapper.readItemStack()),
            (wrapper, payload) -> wrapper.writeItemStack(payload.itemStack)
    );
}
