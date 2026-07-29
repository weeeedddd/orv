package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Tells the client to open one of the mod's screens.
 *
 * <p>Screens can only be opened client-side, so a command has to ask for it
 * over the wire. This is also the seam a voice layer plugs into: anything
 * that can run a chat command — an external speech-to-text tool, a macro
 * binding — can open these windows without touching rendering code.
 */
public record OpenScreenPayload(Target target) implements CustomPacketPayload {

    public enum Target {
        /** The character sheet. */
        STATUS,
        /** The guild console. */
        GUILD
    }

    public static final Type<OpenScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(OrvMod.MOD_ID, "open_screen")
    );

    public static final StreamCodec<FriendlyByteBuf, OpenScreenPayload>
            STREAM_CODEC = new StreamCodec<>() {
                @Override
                public OpenScreenPayload decode(FriendlyByteBuf buffer) {
                    return new OpenScreenPayload(
                            buffer.readEnum(Target.class)
                    );
                }

                @Override
                public void encode(
                        FriendlyByteBuf buffer,
                        OpenScreenPayload payload
                ) {
                    buffer.writeEnum(payload.target());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
