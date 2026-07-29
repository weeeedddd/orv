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
`build/libs/orv-1.0.0.jar`. Im Test-Client öffnet `G` die Guild-Oberfläche
und `K` das Charakterblatt.

In IntelliJ kann der Projektordner direkt als Gradle-Projekt geöffnet
werden. Als Gradle-JVM muss Java 21 ausgewählt sein.

## Shader im Test-Client

`runClient` lädt automatisch die stabilen NeoForge-1.21.1-Versionen von
Iris (`1.8.12`) und Sodium (`0.6.13`). Die Abhängigkeiten gelten nur für
den Client-Run: Sie werden weder in die ORV-JAR eingebettet noch einem
Dedicated-Server hinzugefügt.

Shaderpacks als unveränderte `.zip`-Dateien kommen nach:

```text
run/client/shaderpacks/
```

Danach können sie in Minecraft unter
`Options -> Video Settings -> Shader Packs` ausgewählt werden. Für eine
normale Minecraft-Installation außerhalb der Entwicklungsumgebung müssen
Iris und Sodium separat in denselben NeoForge-1.21.1-Modordner installiert
werden.

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
/orv level add <amount>
/orv level set <level>
```

`<targets>` unterstützt normale Minecraft-Spielerselektoren wie `@s`,
`@p` oder `@a`. Die Level-Befehle verändern das eigene ORV-Strength-Level,
das auch für die Guild-Gründung geprüft wird. Die NBT-Persistenz und
Transaktionsregeln werden über JUnit-Tests abgedeckt.

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

`ORVOverlayHud` rendert die ORV-Systemleiste als Topbar: ein rund 8px
starker Bronzerahmen mit Fase und heller Innenlinie, an beiden Enden
ornamentale Konsolen mit Voluten, die die Leiste oben und unten überragen —
links das Zahnrad-Augen-Siegel, rechts der facettierte Edelstein. Dahinter
liegt ein schwach durchscheinendes „Scenario Path"-Labyrinth, darunter hängt
die Statusplatte mit dem Kanalmeister-Kopf.

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

## Charakter-Daten

Skills und Attributes liegen im persistenten Attachment
`orv:character_profile` (`CharacterProfile`): Skills mit Name, Level und
`stolen`-Flag, Attributes mit Name und Rarity. Stigmas werden nicht dort
dupliziert, sondern beim Senden aus `orv:player_sponsor` aufgelöst.

Alle Änderungen laufen über `CharacterService` (`grantSkill`,
`removeSkill`, `grantAttribute`, `removeAttribute`, `setAge`), das danach
`SyncCharacterProfilePayload` (`orv:character_sync`) sendet. Der Client
legt den Stand in `CharacterClientCache` ab.

## Character Information

`CharacterInfoScreen` (Taste `K`) zeigt das Systemblatt einer Figur: ein
hohes, transluzentes Panel über einem herabrieselnden „Star Stream" aus
grünen Glyphenspalten. Der Rahmen besteht aus zwei leuchtenden Cyan-Linien
mit goldenen Eckwinkeln, oben wachen zwei Augensiegel, unten sitzen zwei
Zahnräder; dahinter liegt ein Konstellationsfeld.

Die Zeilen sind als `Entry`/`Value`-Records modelliert: Label in Hellblau,
Werte in Gold, Zusätze wie `(RARE)` oder `(STOLEN)` gedämpft, der
Bewertungstext in Weiß. Passt der Inhalt nicht in die Panelhöhe, lässt er
sich mit dem Mausrad scrollen (per Scissor sauber beschnitten).

Alle Zeilen stammen aus echten Serverdaten — Name und Level aus dem
Charakter-Payload, Coins/Energy/Constellation aus dem System-Snapshot,
Skills/Attributes/Stigmas aus dem Profil. Leere Listen zeigen
`[NONE RECORDED]`, statt die Überschrift verschwinden zu lassen; bis der
erste Sync eintrifft steht dort `Awaiting system sync...`.

Der Atlas liegt in `assets/orv/textures/gui/character_panel.png` und wird von
`tools/GenerateCharacterAtlas.java` erzeugt; `tools/PreviewCharacterScreen.java`
rendert den Screen offline zur Kontrolle.

## Guild UI & Networking

Das Paket enthält außerdem den per `G` geöffneten `GuildScreen` mit
Member-, Invite- und Guild-Quest-Tab. Die Payloads synchronisieren
Online-Spieler und Guild-Snapshots; Invite- und Rollenaktionen werden
serverseitig autorisiert.

### Gilden-Datenstruktur

Gilden liegen persistent in `GuildStorage`, einer `SavedData` am Overworld-
Storage. Eine `Guild` hat Id, Name, Emblem und eine Mitgliederkarte
(`UUID -> Member{lastKnownName, role}`). Ein abgeleiteter Rückwärtsindex
Spieler→Gilde hält Mitgliedschaftsabfragen bei O(1) und wird beim Laden neu
aufgebaut, statt mitgespeichert zu werden. Verwaiste Gilden ohne Mitglieder
werden entfernt.

Pending Invites bleiben bewusst flüchtig und laufen mit der Session aus.

### Gilden-Gründung

`GuildCreationCheck.evaluate(...)` prüft in fester Reihenfolge: bereits in
einer Gilde, Level, Coins, Name. Schwellen kommen aus der Server-Config
(`GuildConfig`, Default Level 10 und 15.000 Coins) und sind damit
konfigurierbar.

Das Ergebnis wird zweifach genutzt: serverseitig in
`GuildService.createGuild(...)`, das erst prüft, dann die Coins abbucht und
nur bei erfolgreicher Buchung die Gilde anlegt — und im `GuildSnapshot`, so
dass der Client den *Create Guild*-Button ausgrauen und den exakten Grund
als Tooltip anzeigen kann. Die Client-Anzeige ist reine Höflichkeit: der
Server prüft bei `CreateGuildPayload` erneut.

### Fenster öffnen

Screens lassen sich nur clientseitig öffnen, deshalb schickt der Server
`OpenScreenPayload`. Darüber laufen:

```text
/orv status     bzw.  /orv window   → Charakterblatt
/orv guild                          → Gildenkonsole
/orv guild create <name>            → Gilde gründen
```

Der `/orv`-Wurzelknoten ist offen; nur `/orv coins` verlangt weiterhin
Permission-Level 2.

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
