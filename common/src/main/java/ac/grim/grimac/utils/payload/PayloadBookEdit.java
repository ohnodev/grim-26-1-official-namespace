package ac.grim.grimac.utils.payload;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

public record PayloadBookEdit(ItemStack itemStack) {
    public static final PayloadCodec<PayloadBookEdit> CODEC = new PayloadCodec<>(
            wrapper -> new PayloadBookEdit(wrapper.readItemStack()),
            (wrapper, payload) -> wrapper.writeItemStack(payload.itemStack)
    );
}
