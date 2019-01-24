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