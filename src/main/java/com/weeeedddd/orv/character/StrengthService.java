package com.weeeedddd.orv.character;

import com.weeeedddd.orv.data.ModAttachments;
import com.weeeedddd.orv.network.ModNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative mutations for the ORV Strength level.
 */
public final class StrengthService {

    private StrengthService() {
    }

    public static int setLevel(ServerPlayer player, int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }

        ModAttachments.setStrengthLevel(player, level);
        CharacterService.sync(player);
        ModNetworking.sendGuildSnapshot(player);
        return level;
    }

    public static int addLevels(ServerPlayer player, int amount) {
        int nextLevel = calculateAddedLevel(
                ModAttachments.getStrengthLevel(player),
                amount
        );
        return setLevel(player, nextLevel);
    }

    static int calculateAddedLevel(int currentLevel, int amount) {
        if (currentLevel < 0) {
            throw new IllegalArgumentException(
                    "currentLevel must not be negative"
            );
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        return Math.addExact(currentLevel, amount);
    }
}
