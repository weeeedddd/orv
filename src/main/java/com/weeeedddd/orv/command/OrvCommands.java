package com.weeeedddd.orv.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.weeeedddd.orv.economy.CoinService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class OrvCommands {
    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private OrvCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("orv")
                        .requires(source ->
                                source.hasPermission(ADMIN_PERMISSION_LEVEL)
                        )
                        .then(Commands.literal("coins")
                                .then(transaction("add", Operation.ADD))
                                .then(transaction(
                                        "remove",
                                        Operation.REMOVE
                                ))
                                .then(transaction("set", Operation.SET))
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> transaction(
            String name,
            Operation operation
    ) {
        return Commands.literal(name)
                .then(Commands.argument(
                                "targets",
                                EntityArgument.players()
                        )
                        .then(Commands.argument(
                                        "amount",
                                        LongArgumentType.longArg(0L)
                                )
                                .executes(context -> executeTransaction(
                                        context.getSource(),
                                        EntityArgument.getPlayers(
                                                context,
                                                "targets"
                                        ),
                                        LongArgumentType.getLong(
                                                context,
                                                "amount"
                                        ),
                                        operation
                                ))
                        )
                );
    }

    private static int executeTransaction(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            long amount,
            Operation operation
    ) {
        int successes = 0;

        for (ServerPlayer target : targets) {
            try {
                if (apply(operation, target, amount)) {
                    successes++;
                } else {
                    source.sendFailure(Component.translatable(
                            "commands.orv.coins.insufficient",
                            target.getDisplayName(),
                            amount,
                            CoinService.getCoins(target)
                    ));
                }
            } catch (ArithmeticException exception) {
                source.sendFailure(Component.translatable(
                        "commands.orv.coins.overflow",
                        target.getDisplayName()
                ));
            }
        }

        if (successes > 0) {
            int successfulTargets = successes;
            source.sendSuccess(
                    () -> Component.translatable(
                            operation.translationKey,
                            amount,
                            successfulTargets
                    ),
                    true
            );
        }

        return successes;
    }

    private static boolean apply(
            Operation operation,
            ServerPlayer target,
            long amount
    ) {
        return switch (operation) {
            case ADD -> {
                CoinService.addCoins(target, amount);
                yield true;
            }
            case REMOVE -> CoinService.removeCoins(target, amount);
            case SET -> {
                CoinService.setCoins(target, amount);
                yield true;
            }
        };
    }

    private enum Operation {
        ADD("commands.orv.coins.add.success"),
        REMOVE("commands.orv.coins.remove.success"),
        SET("commands.orv.coins.set.success");

        private final String translationKey;

        Operation(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
