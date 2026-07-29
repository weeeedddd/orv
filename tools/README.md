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
(256x128), also deterministically. It holds the glowing filigree frame,
the ornate end brackets, the gear-and-eye seal and the faceted gem, the
coin, energy-vortex and sword-rune icons, the lit and unlit star sigils,
the hanging plate, the channel-master head and the scenario-path
labyrinth. The region layout is mirrored by `client/gui/StatusAtlas.java`.

Nine-sliced regions must be square: `GuiBlit.nineSlice` takes a single
`size`, so a non-square source would read into the neighbouring region.

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

## `GenerateCharacterAtlas.java`

Generates the character sheet atlas.

```sh
java tools/GenerateCharacterAtlas.java
```

Writes `src/main/resources/assets/orv/textures/gui/character_panel.png`
(128x128), deterministically. It holds the twin-rule cyan frame with gold
corner brackets, the watching-eye sigil, a clockwork gear, the dokkaebi
horn-and-eye sigil and a tileable constellation field. The region layout is
mirrored by `client/gui/CharacterAtlas.java`.

## `PreviewCharacterScreen.java`

Renders an offline preview of the character sheet and reports whether the
readout overflows the panel.

```sh
java -Djava.awt.headless=true tools/PreviewCharacterScreen.java
```

Same caveats as the other previews: approximated font metrics, mirrored
constants from `CharacterInfoScreen` and `CharacterAtlas`.
