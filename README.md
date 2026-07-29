# ORV – NeoForge 1.21.1 Mod

Die Klassen unter `src/main/java` wurden gegen NeoForge 1.21.1
(`21.1.235`, Java 21) kompiliert.

## Bauen und testen

Voraussetzung ist ein installiertes JDK 21. Eine separate
Gradle-Installation ist nicht nötig, da der Gradle-Wrapper enthalten ist.

Unter Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Unter Linux oder macOS:

```bash
./gradlew test
./gradlew build
./gradlew runClient
```

Der erste Aufruf lädt die benötigten Gradle-, Minecraft- und
NeoForge-Abhängigkeiten herunter. Das fertige Mod-JAR liegt danach unter
`build/libs/orv-1.0.0.jar`. Im Test-Client öffnet `G` die Guild-Oberfläche.

In IntelliJ kann der Projektordner direkt als Gradle-Projekt geöffnet
werden. Als Gradle-JVM muss Java 21 ausgewählt sein.

## Coin-System

Coins liegen im persistenten `orv:coin_data`-Attachment. Der konkrete
`CoinData`-Typ implementiert `ICoinData`; alle serverseitigen Änderungen
laufen über `CoinService` und lösen danach eine Client-Synchronisierung aus.
Negative Werte und `long`-Überläufe werden abgewiesen.

Administratoren ab Permission-Level 2 können folgende Befehle verwenden:

```text
/orv coins add <targets> <amount>
/orv coins remove <targets> <amount>
/orv coins set <targets> <amount>
```

`<targets>` unterstützt normale Minecraft-Spielerselektoren wie `@s`,
`@p` oder `@a`. Die NBT-Persistenz und Transaktionsregeln werden über
JUnit-Tests abgedeckt.

## System-Synchronisierung

Der Server sendet `SyncSystemDataPayload` unter der Paket-ID
`orv:system_sync`. Der Snapshot enthält Spieler-UUID, Coins, Energy,
Energy-Maximum, Kanal-Kennung und den Konstellationsnamen. Auf dem Client
veröffentlicht der Handler die Daten nach `enqueueWork` atomar in einem
thread-sicheren Cache.

Ein vollständiger Snapshot wird bei Login, Respawn und Dimensionswechsel
gesendet. Coin- und Energy-Änderungen lösen ebenfalls eine Synchronisierung
aus. Beim Logout wird der Client-Cache geleert.

## Sponsor- und Stigma-System

Sponsor-Daten liegen persistent im Attachment `orv:player_sponsor`. Es
speichert den Konstellationsnamen, aktive Stigmas, Probability und
Cooldown-Endzeiten und wird beim Tod kopiert. Änderungen erfolgen
serverseitig über `SponsorService`.

Stigmas werden aus der Datapack-Registry `orv:stigma` geladen. Eigene
Definitionen liegen unter:

```text
data/<datapack-namespace>/orv/stigma/<stigma-id>.json
```

Beispiel:

```json
{
  "name": "Recovery",
  "mana_cost": 20,
  "cooldown_ticks": 100,
  "effect_logic": "orv:heal"
}
```

`effect_logic` verweist auf serverseitig registrierte Logik. Zusätzliche
Implementierungen können über `ModStigmaEffects.register(...)` angebunden
werden. Der mitgelieferte Stigma `orv:recovery` verwendet `orv:heal`.

Die produktive API wird so aufgerufen:

```java
SponsorService.selectConstellation(player, "Demon-like Judge of Fire");
SponsorService.activateStigma(
        player,
        ResourceLocation.fromNamespaceAndPath("orv", "recovery")
);
ModAttachments.setEnergy(player, 100L);

StigmaExecutionResult result = ServerStigmaEngine.execute(
        player,
        ResourceLocation.fromNamespaceAndPath("orv", "recovery")
);
```

Die Engine prüft Definition, Freischaltung, EffectLogic, Cooldown und Mana.
Nur nach erfolgreicher Effekt-Ausführung werden Energy und Cooldown
gespeichert und zum HUD synchronisiert.

Annahmen:

- Mod-ID: `orv`
- Basis-Package: `com.weeeedddd.orv`

## Core-Systeme

1. **Data Attachments registrieren**
   Persistente `CoinData`- und `PlayerData`-Attachments per `Codec`
   registrieren und `copyOnDeath()` aktivieren.
2. **Serverseitige Coin-API implementieren**
   Atomare Get-/Set-/Add-/Remove-Methoden bereitstellen; Änderungen
   ausschließlich über `CoinService` durchführen.
3. **System-Daten zum Client synchronisieren**
   Clientbound `CustomPacketPayload` registrieren und bei Login, Respawn,
   Dimensionswechsel sowie jeder autoritativen API-Änderung senden.
4. **Persistenz und Sync testen**
   GameTests/manuelle Tests für Reconnect, Tod, Nether-/End-Wechsel und
   mehrere Coin-Updates dokumentieren.

## Verwendung

Den Projektordner als Gradle-Projekt öffnen. Die Mod-ID `orv` muss mit
`META-INF/neoforge.mods.toml` übereinstimmen.

Auf NeoForge 1.21.1 synchronisieren Data Attachments nicht automatisch.
Darum gehört der enthaltene Clientbound Payload zur Lösung. Für
ItemStack-Daten werden später Data Components verwendet; Coins und
Player-Werte gehören in Data Attachments.

## Status-HUD

`ORVOverlayHud` rendert die ORV-Systemleiste als Topbar: ein Filigranrahmen
mit ornamentalen Endkonsolen, links das Zahnrad-Augen-Siegel, rechts der
facettierte Edelstein, dahinter ein schwach durchscheinendes
„Scenario Path"-Labyrinth. Darunter hängt die Statusplatte mit dem
Kanalmeister-Kopf.

Die Leiste zeigt fünf Spalten: Kanal, Coins, Strength, Energy (als
`wert / max`) und Constellation. Jede Spalte hat ihr eigenes Sigil; das
Sternzeichen leuchtet erst, wenn eine Konstellation gesetzt ist, sonst
steht dort `[Searching Star Stream]`.

**Die Anzeigewerte sind derzeit fest verdrahtet** und entsprechen 1:1 dem
freigegebenen Entwurf (`MOCK_*`-Konstanten in `ORVOverlayHud`). Die Leiste
zeigt also bei jedem Spieler dieselben Zahlen, unabhängig vom echten
Spielstand.

Die Server-Anbindung liegt vollständig bereit: Coins, Energy, Energy-Maximum,
Kanal-Kennung und Konstellation kommen über `SyncSystemDataPayload` im
`SystemDataClientCache` an. `maxEnergy` und `channelId` liegen im
`orv:player_data`-Attachment (beide Codec-Felder optional, damit ältere
Welten weiter laden) und werden serverseitig über
`ModAttachments.setMaxEnergy(...)` bzw. `setChannelId(...)` gesetzt. Um die
Leiste auf Live-Daten umzustellen, genügt es, die `MOCK_*`-Konstanten in
`buildSegments()` durch Snapshot-Zugriffe zu ersetzen.

Passt der Inhalt nicht in die Leiste, werden zuerst die Spaltenabstände
gestaucht, danach die Konstellation und zuletzt der Kanal mit Ellipse
gekürzt — die Zahlenwerte bleiben immer vollständig lesbar.

Der Atlas liegt in `assets/orv/textures/gui/status_hud.png` und wird von
`tools/GenerateStatusAtlas.java` erzeugt; `tools/PreviewStatusHud.java`
rendert die Leiste offline zur Kontrolle.

## Guild UI & Networking

Das Paket enthält außerdem den per `G` geöffneten `GuildScreen` mit
Member-, Invite- und Guild-Quest-Tab. Die Payloads synchronisieren
Online-Spieler und Guild-Snapshots; Invite- und Rollenaktionen werden
serverseitig autorisiert.

`GuildService` verwendet für diesen Meilenstein bewusst In-Memory-Daten.
Der Einstiegspunkt `createGuild(...)` ist für ein späteres Command- oder
Menü-Feature vorgesehen. Dauerhafte Guild-Daten, Invite-Annahme und
SavedData-Anbindung sind getrennte Folgeschritte.

### Guild-Oberfläche

Der `GuildScreen` ist in Dunkeleiche mit Kupfer- und Goldrahmen gehalten.
Panel, Tabs, Listenzeilen, Pergamentkarte, Goldplakette und Sidebar werden
als Nine-Slice-Regionen aus einem einzelnen Atlas gezeichnet
(`assets/orv/textures/gui/guild_panel.png`); Text und Layout entstehen
prozedural, damit die Beschriftung in jeder GUI-Skalierung scharf bleibt.

Der Atlas ist generiert, nicht handgemalt — `tools/GenerateGuildAtlas.java`
erzeugt ihn reproduzierbar. `tools/PreviewGuildScreen.java` rendert das
Layout offline als PNG, um es ohne Client-Start prüfen zu können. Details
siehe `tools/README.md`.

Die Sidebar-Einträge *Create Guild* und *Guild Settings* sind bewusst
inaktiv: dafür existiert serverseitig noch keine Payload. *Browse Invites*
wechselt auf den Invite-Tab.
