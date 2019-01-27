## [Unreleased]

## [0.8.2] - 2019-01-27
### Changed
- the plugin-shared lib `tools.jar` must now be placed into `/plugins/shared/lib/tools.jar`
- ru.properties now translated by `Galochka`

## [0.8.1] - 2019-01-25
### Added
- translation file for french (fr) translated by google from en
- translation file for russian (ru) translated by  (initialized by google translation from en)
- `/gi status` shows your current locale (en,de,...) and system locale
- `/gi status` shows the used locale (en,de,...)

### Changed
- motd is now false by default
- now `player.getSystemLanguage()` is used instead of `player.getLanguage()`
- updated version of tools.jar (0.2.0) IMPORTANT: if you use my DiscordPlugin too, be sure both have 0.2.0! remove all lower versions!

## [0.8.0] - 2019-01-24
### Added
- Multiple PlayerMessage classes to handle extended communication with RelayServer
- i18n files for translation (de/en)
- translated almost every texts
- you can create your own channels with passwords if you like (must save your settings on the server therefore)
- now using `de.omegazirkel.risingworld.tools` package for logging 
- new Command `/gi save true|false`
- new Command `/gi create channelname [password]`
- new Command `/gi close channelname`

### Changed
- replaced all player.attributes except current default channel with GiPlayer object that holds these settings
- you can now only join channels that were created before
- `/gi status` and `/gi help` were adjusted
- `/gi status` shows now a list of channels you are currently in

### Removed
- server settings `overrideDefault` was removed, only player settings count and will be false by default

## [0.7.2] - 2019-01-15
### Added
- public method to check if an ChatEvent is a GI Chat Message

## [0.7.1] - 2019-01-15
### Fixed
- null pointer exception caused in onDisable()

## [0.7.0] - 2019-01-08
### Added
- new settings `joinDefault=false`. If you set this to `true`, players will join default channel on connect (~ like before < 0.7.0). This allows servers to deactivate default channels for players that don't like to chat and might get annoyed when they have to leave the channel everytime.

### Fixed
- if you try posting into a GI channel that you have not joined yet, you will get a notice but your text does not proceed to local chat anymore.
- if you just want to change your chatOverride channel back to local you can now do this with just typing `#%`. There will be no empty message now. 

## [0.6.1] - 2019-01-05
### Changed
- `/gi info` slightly changed output
- project is now a maven project

### Added
- `/gi status` shows now plugin version

## [0.6.0]
- changed: replaced MongoDB libs with WebSocket, no direct database access from now on.

## [0.5.0]
- fixed: in local chat the first to letters were cut off accidently
- changed: chat-override is now off (false) by default, turn it on in your `settings.properties` if you like
- new command: you can force chat-override to be on or off by yourself as player. Just type `/gi override [true|false]` to override server settings (this is currently stored per session, you have to set it on every new login to mp or sp game)

## [0.4.1]
- Change/Fix: whitespaces before and after chat message will now be removed

## [0.4.0]
- new feature: gi remembers your last channel, just press enter and write! Type `#%text` to return to yourl local server chat. (this is stored per session and returns to default for each login)
- new feature: Chat colors can be adjusted in `settings.properties`
- new command: `/gi status` shows your current chat channel and the database connection status.
- changed: Only channel and player name are colored, text is white (default)

## [0.3.1]
- fixed: reinstalled database to fix access, changed default port to 47017

## [0.3.0]
- new command: `/gi info` shows usage
- new: you can now configure motd in the `settings.properties` file
- new: you can now change the default channel for your server in the `settings.properties` file (default: `global`)

## [0.2.0]
- new feature/command: Custom-Chat-Channels `/gi join|leave channelName` (Currently stored per session yo have to re-enter on each login)
- new feature/command: global chat can be turned off by user with `/gi leave global`

## [0.1.0]
- initial plugin, basic features