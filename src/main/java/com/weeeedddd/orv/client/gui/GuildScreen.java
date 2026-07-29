package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.client.guild.GuildClientState;
import com.weeeedddd.orv.guild.GuildRole;
import com.weeeedddd.orv.guild.GuildRoleAction;
import com.weeeedddd.orv.guild.GuildSnapshot;
import com.weeeedddd.orv.network.GuildPayloads.GuildRoleActionPayload;
import com.weeeedddd.orv.network.GuildPayloads.RequestGuildDataPayload;
import com.weeeedddd.orv.network.GuildPayloads.SendGuildInvitePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class GuildScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 700;
    private static final int PANEL_HEIGHT = 360;
    private static final int PAGE_SIZE = 7;
    private static final int ROW_HEIGHT = 28;

    private static final int BACKGROUND = 0xF0080D18;
    private static final int PANEL = 0xEE0B1220;
    private static final int PANEL_ALT = 0xCC101C2D;
    private static final int ACCENT = 0xFF00E5FF;
    private static final int TEXT = 0xFFE7F7FF;
    private static final int MUTED = 0xFF7890A4;
    private static final int ONLINE = 0xFF55FF88;
    private static final int OFFLINE = 0xFF53606C;
    private static final int DANGER = 0xFFFF5E6C;
    private static final int GOLD = 0xFFFFD166;

    private GuildTab activeTab = GuildTab.MEMBERS;
    private GuildSnapshot snapshot = GuildClientState.snapshot();
    private EditBox inviteNote;
    private String noteDraft = "";
    private long observedRevision = -1L;
    private int memberPage;
    private int invitePage;
    private boolean requestSent;

    public GuildScreen() {
        super(Component.literal("Nebula System"));
    }

    @Override
    protected void init() {
        snapshot = GuildClientState.snapshot();
        observedRevision = GuildClientState.revision();

        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int tabWidth = (panelWidth - 24) / GuildTab.values().length;

        for (int index = 0; index < GuildTab.values().length; index++) {
            GuildTab tab = GuildTab.values()[index];
            Button button = Button.builder(
                            Component.literal(tab.label),
                            ignored -> switchTab(tab)
                    )
                    .bounds(
                            left + 8 + index * tabWidth,
                            top + 48,
                            tabWidth - 4,
                            22
                    )
                    .build();
            button.active = activeTab != tab;
            addRenderableWidget(button);
        }

        switch (activeTab) {
            case MEMBERS -> initMemberActions(left, top, panelWidth);
            case INVITES -> initInviteActions(left, top, panelWidth);
            case QUESTS -> {
                // Placeholder tab intentionally has no widgets yet.
            }
        }

        if (!requestSent) {
            PacketDistributor.sendToServer(
                    new RequestGuildDataPayload()
            );
            requestSent = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        long currentRevision = GuildClientState.revision();
        if (currentRevision != observedRevision) {
            snapshot = GuildClientState.snapshot();
            observedRevision = currentRevision;
            clampPages();
            rebuildWidgets();
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(0, 0, width, height, BACKGROUND);

        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();

        renderPanel(graphics, left, top, panelWidth);

        switch (activeTab) {
            case MEMBERS -> renderMembers(
                    graphics,
                    left,
                    top,
                    panelWidth
            );
            case INVITES -> renderOnlinePlayers(
                    graphics,
                    left,
                    top,
                    panelWidth
            );
            case QUESTS -> renderQuestPlaceholder(
                    graphics,
                    left,
                    top,
                    panelWidth
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderPanel(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth
    ) {
        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + PANEL_HEIGHT,
                PANEL
        );
        graphics.renderOutline(
                left,
                top,
                panelWidth,
                PANEL_HEIGHT,
                ACCENT
        );

        graphics.fill(
                left + 1,
                top + 1,
                left + 5,
                top + 42,
                ACCENT
        );
        graphics.drawString(
                font,
                "NEBULA SYSTEM // GUILD",
                left + 14,
                top + 12,
                ACCENT,
                false
        );
        graphics.drawString(
                font,
                snapshot.guildName(),
                left + 14,
                top + 28,
                TEXT,
                false
        );

        String authority = "AUTHORITY: "
                + snapshot.viewerRole().displayName().toUpperCase();
        graphics.drawString(
                font,
                authority,
                left + panelWidth - font.width(authority) - 12,
                top + 20,
                roleColor(snapshot.viewerRole()),
                false
        );
    }

    private void renderMembers(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth
    ) {
        int listTop = top + 92;
        graphics.drawString(
                font,
                "MEMBER",
                left + 18,
                listTop,
                MUTED,
                false
        );
        graphics.drawString(
                font,
                "ROLE",
                left + Math.min(220, panelWidth / 2),
                listTop,
                MUTED,
                false
        );

        List<GuildSnapshot.Member> visible = visibleMembers();
        if (visible.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    snapshot.viewerRole() == GuildRole.NONE
                            ? "No guild membership found."
                            : "No members available.",
                    left + panelWidth / 2,
                    listTop + 56,
                    MUTED
            );
            return;
        }

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.Member member = visible.get(index);
            int rowY = listTop + 18 + index * ROW_HEIGHT;

            graphics.fill(
                    left + 10,
                    rowY,
                    left + panelWidth - 10,
                    rowY + ROW_HEIGHT - 3,
                    index % 2 == 0 ? PANEL_ALT : 0xAA0D1726
            );
            graphics.fill(
                    left + 18,
                    rowY + 9,
                    left + 24,
                    rowY + 15,
                    member.online() ? ONLINE : OFFLINE
            );
            graphics.drawString(
                    font,
                    member.playerName(),
                    left + 31,
                    rowY + 8,
                    TEXT,
                    false
            );
            graphics.drawString(
                    font,
                    member.role().displayName(),
                    left + Math.min(220, panelWidth / 2),
                    rowY + 8,
                    roleColor(member.role()),
                    false
            );
        }

        renderPageLabel(
                graphics,
                left,
                top,
                panelWidth,
                memberPage,
                pageCount(snapshot.members().size())
        );
    }

    private void renderOnlinePlayers(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth
    ) {
        int listTop = top + 120;
        graphics.drawString(
                font,
                "OPTIONAL INVITE NOTE",
                left + 16,
                top + 84,
                MUTED,
                false
        );
        graphics.drawString(
                font,
                snapshot.viewerRole().canInvite()
                        ? "ONLINE PLAYERS"
                        : "INVITES REQUIRE LEADER OR VICE-LEADER",
                left + 16,
                listTop - 14,
                snapshot.viewerRole().canInvite() ? MUTED : DANGER,
                false
        );

        List<GuildSnapshot.OnlinePlayer> visible =
                visibleOnlinePlayers();
        if (visible.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    "No other players are online.",
                    left + panelWidth / 2,
                    listTop + 52,
                    MUTED
            );
            return;
        }

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.OnlinePlayer player = visible.get(index);
            int rowY = listTop + index * ROW_HEIGHT;

            graphics.fill(
                    left + 10,
                    rowY,
                    left + panelWidth - 10,
                    rowY + ROW_HEIGHT - 3,
                    index % 2 == 0 ? PANEL_ALT : 0xAA0D1726
            );
            graphics.fill(
                    left + 18,
                    rowY + 9,
                    left + 24,
                    rowY + 15,
                    ONLINE
            );
            graphics.drawString(
                    font,
                    player.playerName(),
                    left + 31,
                    rowY + 8,
                    TEXT,
                    false
            );

            String availability = player.available()
                    ? "AVAILABLE"
                    : "UNAVAILABLE";
            graphics.drawString(
                    font,
                    availability,
                    left + panelWidth
                            - font.width(availability)
                            - 86,
                    rowY + 8,
                    player.available() ? ONLINE : OFFLINE,
                    false
            );
        }

        renderPageLabel(
                graphics,
                left,
                top,
                panelWidth,
                invitePage,
                pageCount(snapshot.onlinePlayers().size())
        );
    }

    private void renderQuestPlaceholder(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth
    ) {
        int boxLeft = left + 24;
        int boxTop = top + 102;
        int boxWidth = panelWidth - 48;

        graphics.fill(
                boxLeft,
                boxTop,
                boxLeft + boxWidth,
                boxTop + 170,
                0x77101C2D
        );
        graphics.renderOutline(
                boxLeft,
                boxTop,
                boxWidth,
                170,
                MUTED
        );
        graphics.drawCenteredString(
                font,
                "\u2606 GUILD QUEST ARCHIVE \u2606",
                left + panelWidth / 2,
                boxTop + 54,
                ACCENT
        );
        graphics.drawCenteredString(
                font,
                "Scenario contracts will be indexed here.",
                left + panelWidth / 2,
                boxTop + 82,
                TEXT
        );
        graphics.drawCenteredString(
                font,
                "STATUS: MODULE RESERVED",
                left + panelWidth / 2,
                boxTop + 108,
                MUTED
        );
    }

    private void initMemberActions(
            int left,
            int top,
            int panelWidth
    ) {
        List<GuildSnapshot.Member> visible = visibleMembers();
        int listTop = top + 110;

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.Member member = visible.get(index);
            if (member.playerId().equals(snapshot.viewerId())) {
                continue;
            }

            List<ActionButton> actions = allowedActions(member);
            int buttonWidth = 50;
            int gap = 3;
            int totalWidth = actions.size() * buttonWidth
                    + Math.max(0, actions.size() - 1) * gap;
            int actionX = left + panelWidth - 15 - totalWidth;
            int actionY = listTop + index * ROW_HEIGHT + 3;

            for (ActionButton action : actions) {
                addRenderableWidget(Button.builder(
                                Component.literal(action.label),
                                ignored -> sendRoleAction(
                                        member,
                                        action.action
                                )
                        )
                        .bounds(
                                actionX,
                                actionY,
                                buttonWidth,
                                20
                        )
                        .build());
                actionX += buttonWidth + gap;
            }
        }

        addMemberPageButtons(left, top, panelWidth);
    }

    private void initInviteActions(
            int left,
            int top,
            int panelWidth
    ) {
        inviteNote = new EditBox(
                font,
                left + 16,
                top + 94,
                Math.max(120, panelWidth - 32),
                20,
                Component.literal("Optional guild invite note")
        );
        inviteNote.setMaxLength(160);
        inviteNote.setHint(Component.literal(
                "Optional message for the invited player"
        ));
        inviteNote.setValue(noteDraft);
        inviteNote.setResponder(value -> noteDraft = value);
        addRenderableWidget(inviteNote);

        List<GuildSnapshot.OnlinePlayer> visible =
                visibleOnlinePlayers();
        int listTop = top + 120;

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.OnlinePlayer player = visible.get(index);
            if (!snapshot.viewerRole().canInvite()
                    || !player.available()) {
                continue;
            }

            int rowY = listTop + index * ROW_HEIGHT + 3;
            addRenderableWidget(Button.builder(
                            Component.literal("Invite"),
                            ignored -> sendInvite(player)
                    )
                    .bounds(
                            left + panelWidth - 76,
                            rowY,
                            62,
                            20
                    )
                    .build());
        }

        addInvitePageButtons(left, top, panelWidth);
    }

    private List<ActionButton> allowedActions(
            GuildSnapshot.Member target
    ) {
        GuildRole viewerRole = snapshot.viewerRole();
        List<ActionButton> actions = new ArrayList<>();

        if (viewerRole.canPromote(target.role())) {
            actions.add(new ActionButton(
                    "Promote",
                    GuildRoleAction.PROMOTE
            ));
        }
        if (viewerRole.canDemote(target.role())) {
            actions.add(new ActionButton(
                    "Demote",
                    GuildRoleAction.DEMOTE
            ));
        }
        if (viewerRole.canTransferLeadership(target.role())) {
            actions.add(new ActionButton(
                    "Transfer",
                    GuildRoleAction.TRANSFER_LEADERSHIP
            ));
        }
        if (viewerRole.canKick(target.role())) {
            actions.add(new ActionButton(
                    "Kick",
                    GuildRoleAction.KICK
            ));
        }

        return actions;
    }

    private void sendRoleAction(
            GuildSnapshot.Member target,
            GuildRoleAction action
    ) {
        PacketDistributor.sendToServer(new GuildRoleActionPayload(
                target.playerId(),
                action
        ));
    }

    private void sendInvite(GuildSnapshot.OnlinePlayer target) {
        PacketDistributor.sendToServer(new SendGuildInvitePayload(
                target.playerId(),
                target.playerName(),
                noteDraft
        ));
    }

    private void switchTab(GuildTab tab) {
        activeTab = tab;
        rebuildWidgets();
    }

    private void addMemberPageButtons(
            int left,
            int top,
            int panelWidth
    ) {
        int pages = pageCount(snapshot.members().size());
        if (pages <= 1) {
            return;
        }

        addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        ignored -> {
                            memberPage = Math.max(0, memberPage - 1);
                            rebuildWidgets();
                        }
                )
                .bounds(
                        left + panelWidth - 76,
                        top + PANEL_HEIGHT - 29,
                        28,
                        20
                )
                .build());

        Button next = Button.builder(
                        Component.literal(">"),
                        ignored -> {
                            memberPage = Math.min(
                                    pages - 1,
                                    memberPage + 1
                            );
                            rebuildWidgets();
                        }
                )
                .bounds(
                        left + panelWidth - 43,
                        top + PANEL_HEIGHT - 29,
                        28,
                        20
                )
                .build();
        next.active = memberPage < pages - 1;
        addRenderableWidget(next);
    }

    private void addInvitePageButtons(
            int left,
            int top,
            int panelWidth
    ) {
        int pages = pageCount(snapshot.onlinePlayers().size());
        if (pages <= 1) {
            return;
        }

        addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        ignored -> {
                            invitePage = Math.max(0, invitePage - 1);
                            rebuildWidgets();
                        }
                )
                .bounds(
                        left + panelWidth - 76,
                        top + PANEL_HEIGHT - 29,
                        28,
                        20
                )
                .build());

        Button next = Button.builder(
                        Component.literal(">"),
                        ignored -> {
                            invitePage = Math.min(
                                    pages - 1,
                                    invitePage + 1
                            );
                            rebuildWidgets();
                        }
                )
                .bounds(
                        left + panelWidth - 43,
                        top + PANEL_HEIGHT - 29,
                        28,
                        20
                )
                .build();
        next.active = invitePage < pages - 1;
        addRenderableWidget(next);
    }

    private void renderPageLabel(
            GuiGraphics graphics,
            int left,
            int top,
            int panelWidth,
            int page,
            int pageCount
    ) {
        if (pageCount <= 1) {
            return;
        }

        String label = "PAGE " + (page + 1) + " / " + pageCount;
        graphics.drawString(
                font,
                label,
                left + panelWidth - font.width(label) - 88,
                top + PANEL_HEIGHT - 23,
                MUTED,
                false
        );
    }

    private List<GuildSnapshot.Member> visibleMembers() {
        int from = Math.min(
                memberPage * PAGE_SIZE,
                snapshot.members().size()
        );
        int to = Math.min(
                from + PAGE_SIZE,
                snapshot.members().size()
        );
        return snapshot.members().subList(from, to);
    }

    private List<GuildSnapshot.OnlinePlayer> visibleOnlinePlayers() {
        int from = Math.min(
                invitePage * PAGE_SIZE,
                snapshot.onlinePlayers().size()
        );
        int to = Math.min(
                from + PAGE_SIZE,
                snapshot.onlinePlayers().size()
        );
        return snapshot.onlinePlayers().subList(from, to);
    }

    private void clampPages() {
        memberPage = Math.min(
                memberPage,
                Math.max(0, pageCount(snapshot.members().size()) - 1)
        );
        invitePage = Math.min(
                invitePage,
                Math.max(
                        0,
                        pageCount(snapshot.onlinePlayers().size()) - 1
                )
        );
    }

    private int pageCount(int entryCount) {
        return Math.max(1, (entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int panelWidth() {
        return Math.min(PANEL_MAX_WIDTH, Math.max(360, width - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (height - PANEL_HEIGHT) / 2);
    }

    private int roleColor(GuildRole role) {
        return switch (role) {
            case LEADER -> GOLD;
            case VICE_LEADER -> ACCENT;
            case MEMBER -> TEXT;
            case NONE -> MUTED;
        };
    }

    private enum GuildTab {
        MEMBERS("MEMBERS"),
        INVITES("INVITE"),
        QUESTS("GUILD QUESTS");

        private final String label;

        GuildTab(String label) {
            this.label = label;
        }
    }

    private record ActionButton(
            String label,
            GuildRoleAction action
    ) {
    }
}
