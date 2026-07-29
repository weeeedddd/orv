# Development tools

Standalone single-file Java programs. They are not part of the mod's build
and are not shipped in the jar. Run them from the repository root with
Java 21's single-file source mode.

## `GenerateGuildAtlas.java`

Generates the guild GUI texture atlas.

```sh
java tools/GenerateGuildAtlas.java
```

Writes `src/main/resources/assets/orv/textures/gui/guild_panel.png`
(256x256). Output is deterministic, so rerunning it reproduces the same
bytes and the checked-in PNG stays stable.

The atlas holds tileable dark oak planking, nine-slice regions for the
copper/gold frame, the parchment card, the gold plaque, the carved tabs,
the list rows and the sidebar, plus three 16x16 icons. The region layout is
documented in the file header and mirrored by
`client/gui/GuildAtlas.java` — change one and you must change the other.

## `GenerateStatusAtlas.java`

Generates the status HUD texture atlas.

```sh
java tools/GenerateStatusAtlas.java
```

Writes `src/main/resources/assets/orv/textures/gui/status_hud.png`
(128x128), also deterministically. It holds the glowing filigree frame,
the gear-and-eye alchemy seal, the coin, energy-vortex and sword-rune
icons, the lit and unlit star sigils, the dokkaebi horn motif and the
scenario-path labyrinth. The region layout is mirrored by
`client/gui/StatusAtlas.java`.

## `PreviewGuildScreen.java`

Renders offline previews of the guild screen layout, so the panel can be
checked without launching Minecraft.

```sh
java -Djava.awt.headless=true tools/PreviewGuildScreen.java
```

It replays the same nine-slice maths and the same layout constants as
`GuildScreen` against the generated atlas, and draws text at a fixed 6px
advance — close enough to Minecraft's default font to expose overflow and
centring mistakes. It is an approximation, not a renderer: exact glyph
shapes and hover states will differ in game.

Layout constants are duplicated from `GuildScreen` and `GuildTheme`. If
those change, update the mirrored blocks at the top of the file.

## `PreviewStatusHud.java`

Renders offline previews of the status HUD over a stand-in dusk scene,
with the vanilla hotbar and hearts for context.

```sh
java -Djava.awt.headless=true tools/PreviewStatusHud.java
```

Same caveats as `PreviewGuildScreen`: approximated font metrics, mirrored
constants from `ORVOverlayHud` and `StatusAtlas`.
