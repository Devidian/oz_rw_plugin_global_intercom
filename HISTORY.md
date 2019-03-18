## [Unreleased]

## [0.10.0] - 2019-03-18
### Added
- die Einstellung `restartOnUpdate` in der `settings.properties` sollte jetzt funktionieren.
### Changed
- Befehl `help|info` wurde aufgesplittet, `help` zeigt nun die Befehle, `info` eine Plugin Beschreibung (kommt noch)

## [0.9.0] - 2019-03-15
### Changed
- verwendet nun die tools.jar in Version 0.4.1
- textfarben wurden aus `GlobalIntercom` Klasse entfernt, Colors aus tools wird jetzt verwendet
- MOTD entfernt, heisst jetzt Plugin Welcome Message und kann nur noch ein und aus geschaltet werden, definition des textes ist jetzt in den Sprachdateien, damit jeder Spieler die Nachricht in seiner Sprache lesen kann (sofern übersetzt wurde)
- "Plugin geladen" Meldung wird jetzt in onEnabled gefeuert
- in initSettings wird jetzt "Einstellungen geladen" in das serverlog geschrieben
### Added
- wenn `+screen` in eine Nachricht geschrieben wird, wird ein screenshot übermittelt der in Discord angezeigt werden kann (nicht ingame)
- in den `settings.properties` kann eingestellt werden ob screenshots erlaubt sind und welche maximale Auflösung übertragen werden soll (angabe der breite)

## [0.8.2] - 2019-01-29
### Changed
- verwendet nun die tools.jar in Version 0.3.0

### Fixed
- Übersetzung von Galochka hatte noch Kyrillische Zeichen
- fehlendes `break;` bei `/gi save` Befehl
- `/gi join` und `/gi leave` konnten nicht mehr ohne Kanalnamen aufgerufen werden

### Removed
- Es wird nicht mehr auf die Events `registerPlayer` und `unregisterPlayer` reagiert

## [0.8.2] - 2019-01-27
### Changed
- Die plugin-übergreifende `tools.jar` muss nun unter `/plugins/shared/lib/tools.jar` zur Verfügung stehen
- Die russische Übersetzung ist nun von `Galochka`

## [0.8.1] - 2019-01-25
### Added
- Übersetzungsdatei für französisch hinzugefügt (fr) übersetzt durch google
- Übersetzungsdatei für russisch hinzugefügt (ru) übersetzt durch google
- `/gi status` zeigt deine aktuelle spiel und systemsprache
- `/gi status` zeigt die verwendete sprachdatei

### Changed
- motd ist nun standardmäßig aus
- Zur Ermittlung der Sprache wird nun `player.getSystemLanguage()` statt `player.getLanguage()` verwendet
- aktualisierte Version meienr tools.jar auf 0.2.0 WICHTIG: Wenn du auch mein DiscordPlugin verwendest, vergewissere dich das beide 0.2.0 im /lib ordner haben. entferne alle anderen versionen.

## [0.8.0] - 2019-01-24
### Added
- Mehrere PlayerMessage Klassen hinzugefügt um erweiterte Kommunikation mit dem RelayServer zu ermöglichen
- i18n dateien zur Übersetzung (de/en)
- so ziemlich alle Texte wurden übersetzt
- man kann nun eigene Kanäle erstellen (auch mit Passwort), wenn man seine Einstellungen auf dem Server speichern lässt.
- verwende jetzt das Paket `de.omegazirkel.risingworld.tools` zum logging

### Changed
- Alle player.attributes bis auf den aktuellen chat Kanal wurden in die Klasse GiPlayer verschoben
- man kann nun nur noch Kanälen beitreten, die vorher erstellt wurden
- `/gi status` und `/gi help` wurden angepasst
- `/gi status` zeigt nun auch die Kanäle denen man beigetreten ist
- neues Kommando: `/gi create channelname [password]`
- neues Kommando: `/gi save true|false`
- neues Kommando: `/gi close channelname`

### Removed
- Server Einstellung `overrideDefault` wurde entfernt. Spieler müssen dies nun selbst setzen, standard: aus

## [0.7.2] - 2019-01-15
### Added
- öffentliche methode zum Prüfen ob ein ChatEvent eine GI Nachricht ist

## [0.7.1] - 2019-01-15
### Fixed
- null pointer exception onDisable()

## [0.7.0] - 2019-01-08
### Added
- Neue Einstellung `joinDefault=false` wenn dieser Wert auf `true` gesetzt wird, tritt jeder Spieler beim connecten dem standard Kanal bei, damit ist es Möglich auf servern das standard-beitreten zu deaktivieren um Spieler die dieses Plugin nicht nutzen möchten nicht zu nerven ;)
### Fixed
- wenn man versucht in einen Kanal zu posten in dem man nicht selbst beigetreten ist, wird der text nicht mehr automatisch in den lokalen chat geschrieben.
- wenn man nur den standard-kanal zurück zum lokalen chat wechseln möchte, kann man nun einfach `#%` eingeben. Es erscheint kein leerer text mehr im chat.

### [0.6.1] - 2019-01-05
### Changed
- `/gi info` Ausgabe etwas angepasst
- Projekt ist jetzt ein Maven Projekt

### Added
- `/gi status` zeigt nun die installierte Plugin version an

### [0.6.0]
- Geändert: MongoDB Bibliotheken entfernt, Plugin nutzt jetzt WebSocket für den Versand und Empfang von Nachrichten

### [0.5.0]
- Behoben: die ersten 2 Zeichen im lokalen chat wurden versehentlich abgeschnitten
- Geändert: chat-override ist jetzt in der standard Konfiguration aus, zum ändern in der `settings.properties` wieder auf `true` setzen
- Neu: Spieler können die chat-override Funktion selbst aktivieren oder deaktivieren. Einfach `/gi override [true|false]` eingeben um die Server-Einstellung zu überschreiben. Dies wird im Moment leider nur für die aktuelle Sitzung gespeichert und muss bei erneutem Login wieder eingegeben werden.

### [0.4.1]
- Geändert: unnötige Leerzeichen vor und hinter der text Nachricht werden nun entfernt

### [0.4.0]
- Neu: gi merkt sich wo du zuletzt geschrieben hast, einfach enter drücken und normal schreiben. Mit `#%text` kommst du wieder in den lokalen chat deines Servers. Dies wird nach erneutem Login auf den Standardwert zurückgesetzt (lokaler chat)
- Neu: Die chat farben können in den `settings.properties` angepassst werden
- Neu: `/gi status` zeigt den aktuellen chat kanal und den status der Datenbankverbindung an.
- Geändert: Nur Kanal+Name haben jetzt eine andere Farbe, der text ist weiss

### [0.3.1]
- Behoben: Datenbank neu aufgesetzt und standard-port auf 47017 geändert.

### [0.3.0]
- Neues Kommando: `/gi info` zeigt hilfe an
- Neu: man kann jetzt die plugin-motd in der `settings.properties` Datei konfigurieren
- Neu: man kann jetzt den standard kanal des servers in der `settings.properties` Datei ändern (Standard: `global`)

### [0.2.0]
- Neu: Chat-Kanäle `/gi join|leave channelName`
- Neu: global chat kann mit `/gi leave global` deaktiviert werden

* *Anmerkung: aktive Kanäle werden zur Zeit nur pro Sitzung gespeichert und müssen bei jedem Login neu gesetzt werden (wird in Zukunft noch geändert)

### [0.1.0]
- initiales plugin, basis features