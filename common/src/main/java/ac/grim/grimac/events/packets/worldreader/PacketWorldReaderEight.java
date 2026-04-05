package ac.grim.grimac.events.packets.worldreader;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public class PacketWorldReaderEight extends BasePacketWorldReader {
    @Override
    public void handleMapChunkBulk(final GrimPlayer player, final PacketSendEvent event) {
        throw unsupported();
    }

    @Override
    public void handleMapChunk(final GrimPlayer player, final PacketSendEvent event) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("1.8 chunk packet reader is not supported in this 26.1-only fork");
    }
}
