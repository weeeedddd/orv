package com.weeeedddd.orv.network;

import com.weeeedddd.orv.OrvMod;
import com.weeeedddd.orv.character.CharacterProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The full character sheet for the local player.
 *
 * <p>Stigmas arrive already resolved to display rows, so the client never
 * has to look up stigma definitions itself.
 */
public record SyncCharacterProfilePayload(
        UUID playerId,
        String playerName,
        int level,
        CharacterProfile profile,
        List<CharacterProfile.Skill> stigmas
) implements CustomPacketPayload {
    public static final int MAX_PLAYER_NAME_LENGTH = 32;

    public static final Type<SyncCharacterProfilePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    OrvMod.MOD_ID,
                    "character_sync"
            )
    );

    public SyncCharacterProfilePayload {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(profile, "profile");
        stigmas = List.copyOf(stigmas);
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        if (stigmas.size() > CharacterProfile.MAX_ENTRIES) {
            throw new IllegalArgumentException(
                    "stigmas exceed " + CharacterProfile.MAX_ENTRIES
            );
        }
    }

    public static final StreamCodec<FriendlyByteBuf, SyncCharacterProfilePayload>
            STREAM_CODEC = new StreamCodec<>() {
                @Override
                public SyncCharacterProfilePayload decode(
                        FriendlyByteBuf buffer
                ) {
                    UUID playerId = buffer.readUUID();
                    String playerName = buffer.readUtf(MAX_PLAYER_NAME_LENGTH);
                    int level = buffer.readVarInt();
                    int age = buffer.readVarInt();

                    List<CharacterProfile.Attribute> attributes =
                            buffer.readList(
                                    SyncCharacterProfilePayload::readAttribute
                            );
                    List<CharacterProfile.Skill> skills =
                            buffer.readList(
                                    SyncCharacterProfilePayload::readSkill
                            );
                    List<CharacterProfile.Skill> stigmas =
                            buffer.readList(
                                    SyncCharacterProfilePayload::readSkill
                            );

                    return new SyncCharacterProfilePayload(
                            playerId,
                            playerName,
                            level,
                            new CharacterProfile(age, attributes, skills),
                            stigmas
                    );
                }

                @Override
                public void encode(
                        FriendlyByteBuf buffer,
                        SyncCharacterProfilePayload payload
                ) {
                    buffer.writeUUID(payload.playerId());
                    buffer.writeUtf(
                            payload.playerName(),
                            MAX_PLAYER_NAME_LENGTH
                    );
                    buffer.writeVarInt(payload.level());
                    buffer.writeVarInt(payload.profile().age());
                    buffer.writeCollection(
                            payload.profile().attributes(),
                            SyncCharacterProfilePayload::writeAttribute
                    );
                    buffer.writeCollection(
                            payload.profile().skills(),
                            SyncCharacterProfilePayload::writeSkill
                    );
                    buffer.writeCollection(
                            payload.stigmas(),
                            SyncCharacterProfilePayload::writeSkill
                    );
                }
            };

    private static CharacterProfile.Skill readSkill(FriendlyByteBuf buffer) {
        return new CharacterProfile.Skill(
                buffer.readUtf(CharacterProfile.MAX_ENTRY_NAME_LENGTH),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    private static void writeSkill(
            FriendlyByteBuf buffer,
            CharacterProfile.Skill skill
    ) {
        buffer.writeUtf(skill.name(), CharacterProfile.MAX_ENTRY_NAME_LENGTH);
        buffer.writeVarInt(skill.level());
        buffer.writeBoolean(skill.stolen());
    }

    private static CharacterProfile.Attribute readAttribute(
            FriendlyByteBuf buffer
    ) {
        return new CharacterProfile.Attribute(
                buffer.readUtf(CharacterProfile.MAX_ENTRY_NAME_LENGTH),
                buffer.readEnum(CharacterProfile.Rarity.class)
        );
    }

    private static void writeAttribute(
            FriendlyByteBuf buffer,
            CharacterProfile.Attribute attribute
    ) {
        buffer.writeUtf(
                attribute.name(),
                CharacterProfile.MAX_ENTRY_NAME_LENGTH
        );
        buffer.writeEnum(attribute.rarity());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
