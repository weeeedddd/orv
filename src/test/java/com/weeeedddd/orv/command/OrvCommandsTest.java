package com.weeeedddd.orv.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrvCommandsTest {
    @Test
    void registersTheAdministrativeCoinCommandTree() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();

        OrvCommands.register(dispatcher);

        CommandNode<CommandSourceStack> orv =
                requiredChild(dispatcher.getRoot(), "orv");
        CommandNode<CommandSourceStack> coins =
                requiredChild(orv, "coins");

        assertTransactionPath(coins, "add");
        assertTransactionPath(coins, "remove");
        assertTransactionPath(coins, "set");
    }

    @Test
    void restrictsCoinCommandsToPermissionLevelTwo() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        OrvCommands.register(dispatcher);
        CommandNode<CommandSourceStack> orv =
                requiredChild(dispatcher.getRoot(), "orv");
        CommandNode<CommandSourceStack> coins = requiredChild(orv, "coins");
        CommandSourceStack regularPlayer =
                mock(CommandSourceStack.class);
        CommandSourceStack administrator =
                mock(CommandSourceStack.class);
        when(regularPlayer.hasPermission(2)).thenReturn(false);
        when(administrator.hasPermission(2)).thenReturn(true);

        assertFalse(coins.canUse(regularPlayer));
        assertTrue(coins.canUse(administrator));
        verify(regularPlayer).hasPermission(2);
        verify(administrator).hasPermission(2);
    }

    @Test
    void letsAnyPlayerOpenTheirOwnWindows() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        OrvCommands.register(dispatcher);
        CommandNode<CommandSourceStack> orv =
                requiredChild(dispatcher.getRoot(), "orv");
        CommandSourceStack regularPlayer =
                mock(CommandSourceStack.class);
        when(regularPlayer.hasPermission(2)).thenReturn(false);

        // The root and the window commands must stay open, otherwise a
        // non-operator could not reach their own status or guild screen.
        assertTrue(orv.canUse(regularPlayer));
        assertTrue(requiredChild(orv, "status").canUse(regularPlayer));
        assertTrue(requiredChild(orv, "window").canUse(regularPlayer));
        assertTrue(requiredChild(orv, "guild").canUse(regularPlayer));
    }

    private static void assertTransactionPath(
            CommandNode<CommandSourceStack> coins,
            String operation
    ) {
        CommandNode<CommandSourceStack> operationNode =
                requiredChild(coins, operation);
        CommandNode<CommandSourceStack> targets =
                requiredChild(operationNode, "targets");
        requiredChild(targets, "amount");
    }

    private static CommandNode<CommandSourceStack> requiredChild(
            CommandNode<CommandSourceStack> parent,
            String name
    ) {
        CommandNode<CommandSourceStack> child = parent.getChild(name);
        assertNotNull(child, () -> "Missing command node: " + name);
        return child;
    }
}
