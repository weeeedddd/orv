package com.weeeedddd.orv.client.gui;

import com.weeeedddd.orv.client.gui.widget.GuildTabButton;
import com.weeeedddd.orv.client.gui.widget.SidebarButton;
import com.weeeedddd.orv.client.gui.widget.WoodButton;
import com.weeeedddd.orv.client.guild.GuildClientState;
import com.weeeedddd.orv.guild.GuildCreationCheck;
import com.weeeedddd.orv.guild.GuildRole;
import com.weeeedddd.orv.guild.GuildRoleAction;
import com.weeeedddd.orv.guild.GuildSnapshot;
import com.weeeedddd.orv.network.GuildPayloads.GuildRoleActionPayload;
import com.weeeedddd.orv.network.GuildPayloads.RequestGuildDataPayload;
import com.weeeedddd.orv.network.CreateGuildPayload;
import com.weeeedddd.orv.network.GuildPayloads.SendGuildInvitePayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class GuildScreen extends Screen {

    private static final int PANEL_MAX_WIDTH = 720;
    private static final int PANEL_HEIGHT = 344;
    private static final int PAGE_SIZE = 7;
    private static final int ROW_HEIGHT = 26;
    private static final int MARGIN = 14;
    private static final int TAB_HEIGHT = 24;
    private static final int MOTE_COUNT = 16;
    /** Height of the little stand drawn beneath the empty-state placard. */
    private static final int STAND_HEIGHT = 9;

    private static final int SIDEBAR_WIDTH = 40;
    private static final int SIDEBAR_ICON = 28;
    private static final int SIDEBAR_GAP = 8;
    private static final int SIDEBAR_PAD = 8;
    private static final int SIDEBAR_ENTRIES = 3;
    private static final int SIDEBAR_HEIGHT = SIDEBAR_PAD * 2
            + SIDEBAR_ENTRIES * SIDEBAR_ICON
            + (SIDEBAR_ENTRIES - 1) * SIDEBAR_GAP;

    private GuildTab activeTab = GuildTab.MEMBERS;
    private GuildSnapshot snapshot = GuildClientState.snapshot();
    private EditBox inviteNote;
    private String noteDraft = "";
    private long observedRevision = -1L;
    private int memberPage;
    private int invitePage;
    private boolean requestSent;

    public GuildScreen() {
        super(Component.literal("Guild Console"));
    }

    @Override
    protected void init() {
        snapshot = GuildClientState.snapshot();
        observedRevision = GuildClientState.revision();

        Layout layout = layout();
        addTabs(layout);
        addSidebar(layout);

        switch (activeTab) {
            case MEMBERS -> initMemberActions(layout);
            case INVITES -> initInviteActions(layout);
            case QUESTS -> {
                // Placeholder tab intentionally has no widgets yet.
            }
        }

        if (!requestSent) {
            PacketDistributor.sendToServer(new RequestGuildDataPayload());
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
        renderWorldBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, GuildTheme.BACKDROP);

        Layout layout = layout();
        renderPanel(graphics, layout);

        switch (activeTab) {
            case MEMBERS -> renderMembers(graphics, layout);
            case INVITES -> renderInvites(graphics, layout);
            case QUESTS -> renderQuests(graphics, layout);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        renderMotes(graphics, layout);
    }

    /**
     * Renders Minecraft's world blur before any guild content is drawn.
     *
     * <p>{@link Screen#render} invokes {@link #renderBackground} immediately
     * before rendering widgets. Calling it unchanged after the custom guild
     * layers would blur those layers as part of the current framebuffer.
     */
    protected void renderWorldBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * The background pass already ran at the beginning of {@link #render}.
     * This override prevents {@link Screen#render} from applying it again
     * after the custom guild layers have been drawn.
     */
    @Override
    public void renderBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // Intentionally empty.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------------------------------------------------------
    // Chrome
    // ---------------------------------------------------------------

    private void renderPanel(GuiGraphics graphics, Layout layout) {
        GuildAtlas.tileWood(
                graphics,
                layout.left(),
                layout.top(),
                layout.panelWidth(),
                PANEL_HEIGHT
        );
        GuildAtlas.nineSlice(
                graphics,
                GuildAtlas.FRAME,
                layout.left(),
                layout.top(),
                layout.panelWidth(),
                PANEL_HEIGHT
        );

        renderPlaque(graphics, layout);

        GuildAtlas.nineSlice(
                graphics,
                GuildAtlas.SIDEBAR,
                layout.sidebarX(),
                layout.contentTop() - 4,
                SIDEBAR_WIDTH,
                SIDEBAR_HEIGHT
        );
    }

    /** Polished gold plaque carrying the viewer's rank and guild. */
    private void renderPlaque(GuiGraphics graphics, Layout layout) {
        int plaqueWidth = Math.min(340, layout.panelWidth() - 2 * MARGIN);
        int plaqueX = layout.left() + MARGIN;
        int plaqueY = layout.top() + 12;
        int plaqueHeight = 38;

        GuildAtlas.nineSlice(
                graphics,
                GuildAtlas.PLAQUE,
                plaqueX,
                plaqueY,
                plaqueWidth,
                plaqueHeight
        );

        int textLimit = plaqueWidth - 20;
        String rankLine = "[ Guild Console | Rank: "
                + snapshot.viewerRole().displayName().toUpperCase() + " ]";
        String guildLine = "[ GUILD SYSTEM | CURRENT GUILD: "
                + currentGuildLabel() + " ]";

        graphics.drawString(
                font,
                GuildTheme.fit(font, rankLine, textLimit),
                plaqueX + 10,
                plaqueY + 8,
                GuildTheme.PLAQUE_TEXT,
                false
        );
        graphics.drawString(
                font,
                GuildTheme.fit(font, guildLine, textLimit),
                plaqueX + 10,
                plaqueY + 21,
                GuildTheme.PLAQUE_TEXT_DIM,
                false
        );
    }

    private String currentGuildLabel() {
        if (observedRevision <= 0L) {
            return "Loading…";
        }
        if (snapshot.viewerRole() == GuildRole.NONE) {
            return "None";
        }
        String name = snapshot.guildName();
        return name == null || name.isBlank() ? "None" : name;
    }

    /** Faint blue motes drifting near the panel corners. */
    private void renderMotes(GuiGraphics graphics, Layout layout) {
        long millis = Util.getMillis();

        for (int index = 0; index < MOTE_COUNT; index++) {
            int corner = index % 4;
            double speed = 0.09 + (index % 5) * 0.017;
            double phase = ((millis / 1000.0) * speed + index * 0.37) % 1.0;

            // Motes fade in and out over their travel, so they never pop.
            int alpha = (int) (Math.sin(phase * Math.PI) * 110.0);
            if (alpha <= 6) {
                continue;
            }

            double spread = Math.abs(Math.sin(index * 12.9898)) * 30.0
                    + Math.sin(millis / 1400.0 + index) * 5.0;
            double travel = phase * 46.0;
            boolean leftSide = corner == 0 || corner == 2;
            boolean topSide = corner < 2;

            int x = leftSide
                    ? layout.left() + 8 + (int) spread
                    : layout.left() + layout.panelWidth() - 8 - (int) spread;
            int y = topSide
                    ? layout.top() + 10 + (int) (travel * 0.55)
                    : layout.top() + PANEL_HEIGHT - 10 - (int) travel;

            int size = 1 + (index % 2);
            graphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    (alpha << 24) | (GuildTheme.GLOW & 0x00FFFFFF)
            );
        }
    }

    // ---------------------------------------------------------------
    // Tabs and sidebar
    // ---------------------------------------------------------------

    private void addTabs(Layout layout) {
        GuildTab[] tabs = GuildTab.values();
        int available = layout.contentWidth();
        int gap = 4;
        int tabWidth = (available - gap * (tabs.length - 1)) / tabs.length;

        for (int index = 0; index < tabs.length; index++) {
            GuildTab tab = tabs[index];
            addRenderableWidget(new GuildTabButton(
                    layout.contentLeft() + index * (tabWidth + gap),
                    layout.tabTop(),
                    tabWidth,
                    TAB_HEIGHT,
                    Component.literal(tab.label),
                    activeTab == tab,
                    () -> switchTab(tab)
            ));
        }
    }

    private void addSidebar(Layout layout) {
        int size = SIDEBAR_ICON;
        int x = layout.sidebarX() + (SIDEBAR_WIDTH - size) / 2;
        int y = layout.contentTop() - 4 + SIDEBAR_PAD;
        int step = size + SIDEBAR_GAP;

        // The server decides eligibility and ships the verdict in the
        // snapshot; the button only mirrors it.
        GuildCreationCheck creation = snapshot.creation();
        SidebarButton create = new SidebarButton(
                x,
                y,
                size,
                Component.literal("Create Guild"),
                GuildAtlas.ICON_QUILL_U,
                this::requestGuildCreation
        );
        create.active = creation.allowed();
        create.setTooltip(Tooltip.create(Component.literal(
                "Create Guild — " + creation.describe()
        )));
        addRenderableWidget(create);

        addRenderableWidget(new SidebarButton(
                x,
                y + step,
                size,
                Component.literal("Browse Invites"),
                GuildAtlas.ICON_LETTER_U,
                () -> switchTab(GuildTab.INVITES)
        ));

        SidebarButton settings = new SidebarButton(
                x,
                y + step * 2,
                size,
                Component.literal("Guild Settings"),
                GuildAtlas.ICON_KEY_U,
                () -> {
                }
        );
        settings.active = false;
        settings.setTooltip(Tooltip.create(
                Component.literal("Guild Settings — not available yet")
        ));
        addRenderableWidget(settings);
    }

    // ---------------------------------------------------------------
    // Members tab
    // ---------------------------------------------------------------

    private void renderMembers(GuiGraphics graphics, Layout layout) {
        renderColumnHeaders(graphics, layout, "MEMBER NAME", "GUILD RANK");

        List<GuildSnapshot.Member> visible = visibleMembers();
        if (visible.isEmpty()) {
            renderEmptyState(
                    graphics,
                    layout,
                    snapshot.viewerRole() == GuildRole.NONE
                            ? "Guild Member List: Currently unassigned to a guild."
                            : "Guild Member List: No members on record."
            );
            return;
        }

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.Member member = visible.get(index);
            int rowY = layout.listTop() + index * ROW_HEIGHT;
            renderRow(graphics, layout, index, rowY);

            graphics.fill(
                    layout.contentLeft() + 10,
                    rowY + 9,
                    layout.contentLeft() + 15,
                    rowY + 14,
                    member.online() ? GuildTheme.ONLINE : GuildTheme.OFFLINE
            );
            graphics.drawString(
                    font,
                    GuildTheme.fit(font, member.playerName(), layout.rankX()
                            - layout.contentLeft() - 30),
                    layout.contentLeft() + 22,
                    rowY + 7,
                    GuildTheme.TEXT,
                    false
            );
            graphics.drawString(
                    font,
                    member.role().displayName(),
                    layout.rankX(),
                    rowY + 7,
                    roleColor(member.role()),
                    false
            );
        }

        renderPageLabel(
                graphics,
                layout,
                memberPage,
                pageCount(snapshot.members().size())
        );
    }

    private void initMemberActions(Layout layout) {
        List<GuildSnapshot.Member> visible = visibleMembers();

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.Member member = visible.get(index);
            if (member.playerId().equals(snapshot.viewerId())) {
                continue;
            }

            List<ActionButton> actions = allowedActions(member);
            int buttonWidth = 52;
            int gap = 3;
            int totalWidth = actions.size() * buttonWidth
                    + Math.max(0, actions.size() - 1) * gap;
            int actionX = layout.contentRight() - 6 - totalWidth;
            int actionY = layout.listTop() + index * ROW_HEIGHT + 2;

            for (ActionButton action : actions) {
                addRenderableWidget(new WoodButton(
                        actionX,
                        actionY,
                        buttonWidth,
                        ROW_HEIGHT - 8,
                        Component.literal(action.label()),
                        action.action() == GuildRoleAction.KICK
                                ? GuildTheme.DANGER
                                : GuildTheme.GOLD_INLAY,
                        () -> sendRoleAction(member, action.action())
                ));
                actionX += buttonWidth + gap;
            }
        }

        addPageButtons(
                layout,
                memberPage,
                pageCount(snapshot.members().size()),
                page -> {
                    memberPage = page;
                    rebuildWidgets();
                }
        );
    }

    // ---------------------------------------------------------------
    // Invite tab
    // ---------------------------------------------------------------

    private void renderInvites(GuiGraphics graphics, Layout layout) {
        boolean canInvite = snapshot.viewerRole().canInvite();

        graphics.drawString(
                font,
                "OPTIONAL INVITE NOTE",
                layout.contentLeft() + 2,
                layout.contentTop() + 2,
                GuildTheme.COPPER_ETCH,
                false
        );
        graphics.drawString(
                font,
                canInvite
                        ? "ONLINE PLAYERS"
                        : "INVITES REQUIRE LEADER OR VICE-LEADER",
                layout.contentLeft() + 2,
                layout.contentTop() + 34,
                canInvite ? GuildTheme.COPPER_ETCH : GuildTheme.DANGER,
                false
        );

        List<GuildSnapshot.OnlinePlayer> visible = visibleOnlinePlayers();
        if (visible.isEmpty()) {
            renderEmptyState(
                    graphics,
                    layout,
                    "Online Roster: No other players are connected."
            );
            return;
        }

        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.OnlinePlayer player = visible.get(index);
            int rowY = layout.listTop() + index * ROW_HEIGHT;
            renderRow(graphics, layout, index, rowY);

            graphics.fill(
                    layout.contentLeft() + 10,
                    rowY + 9,
                    layout.contentLeft() + 15,
                    rowY + 14,
                    GuildTheme.ONLINE
            );
            graphics.drawString(
                    font,
                    GuildTheme.fit(font, player.playerName(), 140),
                    layout.contentLeft() + 22,
                    rowY + 7,
                    GuildTheme.TEXT,
                    false
            );

            String availability = player.available() ? "AVAILABLE" : "IN A GUILD";
            graphics.drawString(
                    font,
                    availability,
                    layout.rankX(),
                    rowY + 7,
                    player.available() ? GuildTheme.ONLINE : GuildTheme.MUTED,
                    false
            );
        }

        renderPageLabel(
                graphics,
                layout,
                invitePage,
                pageCount(snapshot.onlinePlayers().size())
        );
    }

    private void initInviteActions(Layout layout) {
        inviteNote = new EditBox(
                font,
                layout.contentLeft() + 2,
                layout.contentTop() + 14,
                layout.contentWidth() - 4,
                18,
                Component.literal("Optional guild invite note")
        );
        inviteNote.setMaxLength(160);
        inviteNote.setHint(Component.literal(
                "Optional message for the invited player"
        ));
        inviteNote.setValue(noteDraft);
        inviteNote.setResponder(value -> noteDraft = value);
        addRenderableWidget(inviteNote);

        List<GuildSnapshot.OnlinePlayer> visible = visibleOnlinePlayers();
        for (int index = 0; index < visible.size(); index++) {
            GuildSnapshot.OnlinePlayer player = visible.get(index);
            if (!snapshot.viewerRole().canInvite() || !player.available()) {
                continue;
            }

            addRenderableWidget(new WoodButton(
                    layout.contentRight() - 64,
                    layout.listTop() + index * ROW_HEIGHT + 2,
                    58,
                    ROW_HEIGHT - 8,
                    Component.literal("Invite"),
                    GuildTheme.GOLD_INLAY,
                    () -> sendInvite(player)
            ));
        }

        addPageButtons(
                layout,
                invitePage,
                pageCount(snapshot.onlinePlayers().size()),
                page -> {
                    invitePage = page;
                    rebuildWidgets();
                }
        );
    }

    // ---------------------------------------------------------------
    // Quests tab
    // ---------------------------------------------------------------

    private void renderQuests(GuiGraphics graphics, Layout layout) {
        int cardWidth = Math.min(320, layout.contentWidth() - 20);
        int cardHeight = 108;
        int cardX = layout.contentLeft()
                + (layout.contentWidth() - cardWidth) / 2;
        int cardY = layout.contentTop()
                + (layout.contentHeight() - cardHeight) / 2;

        GuildAtlas.nineSlice(
                graphics,
                GuildAtlas.PARCHMENT,
                cardX,
                cardY,
                cardWidth,
                cardHeight
        );

        int centerX = cardX + cardWidth / 2;
        graphics.drawCenteredString(
                font,
                "☆ Guild Quest Archive ☆",
                centerX,
                cardY + 26,
                GuildTheme.INK
        );
        graphics.drawCenteredString(
                font,
                "Scenario contracts will be indexed here.",
                centerX,
                cardY + 48,
                GuildTheme.INK_DIM
        );
        graphics.drawCenteredString(
                font,
                "Status: module reserved",
                centerX,
                cardY + 70,
                GuildTheme.INK_DIM
        );
    }

    // ---------------------------------------------------------------
    // Shared content pieces
    // ---------------------------------------------------------------

    private void renderColumnHeaders(
            GuiGraphics graphics,
            Layout layout,
            String left,
            String right
    ) {
        int headerY = layout.contentTop() + 8;
        graphics.drawString(
                font,
                left,
                layout.contentLeft() + 2,
                headerY,
                GuildTheme.COPPER_ETCH,
                false
        );
        graphics.drawString(
                font,
                right,
                layout.rankX(),
                headerY,
                GuildTheme.COPPER_ETCH,
                false
        );
        // Thin copper rule under the headers.
        graphics.fill(
                layout.contentLeft(),
                headerY + 12,
                layout.contentRight(),
                headerY + 13,
                GuildTheme.COPPER_DIM
        );
    }

    /**
     * Draws the parchment placard used whenever a list has nothing to show,
     * with an explanatory line in gold beneath it.
     */
    private void renderEmptyState(
            GuiGraphics graphics,
            Layout layout,
            String caption
    ) {
        int cardWidth = Math.min(230, layout.contentWidth() - 40);
        int cardHeight = 62;
        // Card, stand and caption are centred as one block.
        int blockHeight = cardHeight + STAND_HEIGHT + 24;
        int cardX = layout.contentLeft()
                + (layout.contentWidth() - cardWidth) / 2;
        int cardY = layout.contentTop()
                + (layout.contentHeight() - blockHeight) / 2;
        int centerX = cardX + cardWidth / 2;

        // Small stand under the placard.
        graphics.fill(
                centerX - 18,
                cardY + cardHeight,
                centerX + 18,
                cardY + cardHeight + 4,
                0xFF5C3D23
        );
        graphics.fill(
                centerX - 30,
                cardY + cardHeight + 4,
                centerX + 30,
                cardY + cardHeight + 7,
                0xFF3B2616
        );
        graphics.fill(
                centerX - 30,
                cardY + cardHeight + 7,
                centerX + 30,
                cardY + cardHeight + 9,
                0xFF241608
        );

        GuildAtlas.nineSlice(
                graphics,
                GuildAtlas.PARCHMENT,
                cardX,
                cardY,
                cardWidth,
                cardHeight
        );
        graphics.drawCenteredString(
                font,
                "No entries found.",
                centerX,
                cardY + cardHeight / 2 - 4,
                GuildTheme.INK
        );

        graphics.drawCenteredString(
                font,
                GuildTheme.fit(font, caption, layout.contentWidth() - 8),
                layout.contentLeft() + layout.contentWidth() / 2,
                cardY + cardHeight + STAND_HEIGHT + 15,
                GuildTheme.GOLD
        );
    }

    private void renderRow(
            GuiGraphics graphics,
            Layout layout,
            int index,
            int rowY
    ) {
        GuildAtlas.nineSlice(
                graphics,
                index % 2 == 0 ? GuildAtlas.ROW_EVEN : GuildAtlas.ROW_ODD,
                layout.contentLeft(),
                rowY,
                layout.contentWidth(),
                ROW_HEIGHT - 4
        );
    }

    private void renderPageLabel(
            GuiGraphics graphics,
            Layout layout,
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
                layout.contentRight() - 70 - font.width(label),
                layout.contentBottom() - 15,
                GuildTheme.MUTED,
                false
        );
    }

    private void addPageButtons(
            Layout layout,
            int page,
            int pages,
            IntConsumer onPage
    ) {
        if (pages <= 1) {
            return;
        }

        int buttonY = layout.contentBottom() - 20;
        WoodButton previous = new WoodButton(
                layout.contentRight() - 64,
                buttonY,
                28,
                18,
                Component.literal("<"),
                GuildTheme.GOLD_INLAY,
                () -> onPage.accept(Math.max(0, page - 1))
        );
        previous.active = page > 0;
        addRenderableWidget(previous);

        WoodButton next = new WoodButton(
                layout.contentRight() - 32,
                buttonY,
                28,
                18,
                Component.literal(">"),
                GuildTheme.GOLD_INLAY,
                () -> onPage.accept(Math.min(pages - 1, page + 1))
        );
        next.active = page < pages - 1;
        addRenderableWidget(next);
    }

    // ---------------------------------------------------------------
    // Behaviour
    // ---------------------------------------------------------------

    private List<ActionButton> allowedActions(GuildSnapshot.Member target) {
        GuildRole viewerRole = snapshot.viewerRole();
        List<ActionButton> actions = new ArrayList<>();

        if (viewerRole.canPromote(target.role())) {
            actions.add(new ActionButton("Promote", GuildRoleAction.PROMOTE));
        }
        if (viewerRole.canDemote(target.role())) {
            actions.add(new ActionButton("Demote", GuildRoleAction.DEMOTE));
        }
        if (viewerRole.canTransferLeadership(target.role())) {
            actions.add(new ActionButton(
                    "Transfer",
                    GuildRoleAction.TRANSFER_LEADERSHIP
            ));
        }
        if (viewerRole.canKick(target.role())) {
            actions.add(new ActionButton("Kick", GuildRoleAction.KICK));
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

    /**
     * Asks the server to found a guild. The name is taken from the invite
     * note field when the invite tab is open, otherwise a default is used;
     * the server validates it either way.
     */
    private void requestGuildCreation() {
        String name = noteDraft == null || noteDraft.isBlank()
                ? minecraft.player.getGameProfile().getName() + "'s Guild"
                : noteDraft;
        PacketDistributor.sendToServer(new CreateGuildPayload(
                name,
                "star"
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
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        rebuildWidgets();
    }

    private List<GuildSnapshot.Member> visibleMembers() {
        int from = Math.min(memberPage * PAGE_SIZE, snapshot.members().size());
        int to = Math.min(from + PAGE_SIZE, snapshot.members().size());
        return snapshot.members().subList(from, to);
    }

    private List<GuildSnapshot.OnlinePlayer> visibleOnlinePlayers() {
        int from = Math.min(
                invitePage * PAGE_SIZE,
                snapshot.onlinePlayers().size()
        );
        int to = Math.min(from + PAGE_SIZE, snapshot.onlinePlayers().size());
        return snapshot.onlinePlayers().subList(from, to);
    }

    private void clampPages() {
        memberPage = Math.min(
                memberPage,
                Math.max(0, pageCount(snapshot.members().size()) - 1)
        );
        invitePage = Math.min(
                invitePage,
                Math.max(0, pageCount(snapshot.onlinePlayers().size()) - 1)
        );
    }

    private int pageCount(int entryCount) {
        return Math.max(1, (entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int roleColor(GuildRole role) {
        return switch (role) {
            case LEADER -> GuildTheme.GOLD_INLAY;
            case VICE_LEADER -> GuildTheme.COPPER_ETCH;
            case MEMBER -> GuildTheme.TEXT;
            case NONE -> GuildTheme.MUTED;
        };
    }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------

    private Layout layout() {
        int panelWidth = Math.min(
                PANEL_MAX_WIDTH,
                Math.max(400, width - 24)
        );
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - PANEL_HEIGHT) / 2);
        int sidebarX = left + panelWidth - MARGIN - SIDEBAR_WIDTH;

        return new Layout(
                left,
                top,
                panelWidth,
                left + MARGIN,
                sidebarX - 8,
                top + 62,
                top + 92,
                top + PANEL_HEIGHT - MARGIN,
                sidebarX
        );
    }

    /** Resolved pixel geometry for the current screen size. */
    private record Layout(
            int left,
            int top,
            int panelWidth,
            int contentLeft,
            int contentRight,
            int tabTop,
            int contentTop,
            int contentBottom,
            int sidebarX
    ) {
        int contentWidth() {
            return contentRight - contentLeft;
        }

        int contentHeight() {
            return contentBottom - contentTop;
        }

        int listTop() {
            return contentTop + 24;
        }

        /** Left edge of the rank / availability column. */
        int rankX() {
            return contentLeft + Math.min(260, contentWidth() / 2);
        }
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

    private record ActionButton(String label, GuildRoleAction action) {
    }
}
