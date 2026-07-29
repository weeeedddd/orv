package com.weeeedddd.orv.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.weeeedddd.orv.character.StrengthService;
import com.weeeedddd.orv.guild.GuildCreationCheck;
import com.weeeedddd.orv.guild.GuildService;
import com.weeeedddd.orv.guild.GuildStorage;
import com.weeeedddd.orv.network.ModNetworking;
import com.weeeedddd.orv.network.OpenScreenPayload.Target;
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
                        // The root is open so players can reach their own
                        // windows; only administrative mutation subtrees
                        // need operator rights.
                        .then(Commands.literal("coins")
                                .requires(source -> source.hasPermission(
                                        ADMIN_PERMISSION_LEVEL
                                ))
                                .then(transaction("add", Operation.ADD))
                                .then(transaction(
                                        "remove",
                                        Operation.REMOVE
                                ))
                                .then(transaction("set", Operation.SET))
                        )
                        .then(Commands.literal("level")
                                .requires(source -> source.hasPermission(
                                        ADMIN_PERMISSION_LEVEL
                                ))
                                .then(levelOperation(
                                        "add",
                                        LevelOperation.ADD
                                ))
                                .then(levelOperation(
                                        "set",
                                        LevelOperation.SET
                                ))
                        )
                        .then(openScreen("status", Target.STATUS))
                        // Alias, so a spoken "open window" maps to a command.
                        .then(openScreen("window", Target.STATUS))
                        .then(Commands.literal("guild")
                                .executes(context -> open(
                                        context.getSource(),
                                        Target.GUILD
                                ))
                                .then(Commands.literal("create")
                                        .then(Commands.argument(
                                                        "name",
                                                        StringArgumentType
                                                                .greedyString()
                                                )
                                                .executes(
                                                        OrvCommands::createGuild
                                                )
                                        )
                                )
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> openScreen(
            String name,
            Target target
    ) {
        return Commands.literal(name)
                .executes(context -> open(context.getSource(), target));
    }

    private static int open(CommandSourceStack source, Target target) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(
                    "This command must be run by a player."
            ));
            return 0;
        }
        ModNetworking.openScreen(player, target);
        return Command.SINGLE_SUCCESS;
    }

    private static int createGuild(
            CommandContext<CommandSourceStack> context
    ) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal(
                    "This command must be run by a player."
            ));
            return 0;
        }

        String name = StringArgumentType.getString(context, "name");
        GuildCreationCheck check = GuildService.createGuild(
                player,
                name,
                GuildStorage.DEFAULT_EMBLEM
        );
        return check.allowed() ? Command.SINGLE_SUCCESS : 0;
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

    private static LiteralArgumentBuilder<CommandSourceStack> levelOperation(
            String name,
            LevelOperation operation
    ) {
        return Commands.literal(name)
                .then(Commands.argument(
                                "amount",
                                IntegerArgumentType.integer(0)
                        )
                        .executes(context -> executeLevelOperation(
                                context.getSource(),
                                IntegerArgumentType.getInteger(
                                        context,
                                        "amount"
                                ),
                                operation
                        ))
                );
    }

    private static int executeLevelOperation(
            CommandSourceStack source,
            int amount,
            LevelOperation operation
    ) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(
                    "This command must be run by a player."
            ));
            return 0;
        }

        try {
            int newLevel = switch (operation) {
                case ADD -> StrengthService.addLevels(player, amount);
                case SET -> StrengthService.setLevel(player, amount);
            };
            source.sendSuccess(
                    () -> Component.translatable(
                            operation.translationKey,
                            amount,
                            newLevel
                    ),
                    true
            );
            return Command.SINGLE_SUCCESS;
        } catch (ArithmeticException exception) {
            source.sendFailure(Component.translatable(
                    "commands.orv.level.overflow"
            ));
            return 0;
        }
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

    private enum LevelOperation {
        ADD("commands.orv.level.add.success"),
        SET("commands.orv.level.set.success");

        private final String translationKey;

        LevelOperation(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
