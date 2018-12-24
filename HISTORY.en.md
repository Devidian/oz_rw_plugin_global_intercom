### Version 0.5.0:
- Fixed: in local chat the first to letters were cut off accidently
- Changed: chat-override is now off (false) by default, turn it on in your `settings.properties` if you like
- New command: you can force chat-override to be on or off by yourself as player. Just type `/gi override [true|false]` to override server settings (this is currently stored per session, you have to set it on every new login to mp or sp game)

### Version 0.4.1:
- Change/Fix: whitespaces before and after chat message will now be removed

### Version 0.4.0:
- New feature: gi remembers your last channel, just press enter and write! Type `#%text` to return to yourl local server chat. (this is stored per session and returns to default for each login)
- New feature: Chat colors can be adjusted in `settings.properties`
- New command: `/gi status` shows your current chat channel and the database connection status.
- Changed: Only channel and player name are colored, text is white (default)

### Version 0.3.1:
- Fixed: reinstalled database to fix access, changed default port to 47017

### Version 0.3.0:
- New command: `/gi info` shows usage
- New: you can now configure motd in the `settings.properties` file
- New: you can now change the default channel for your server in the `settings.properties` file (default: `global`)

### Version 0.2.0:
- New feature/command: Custom-Chat-Channels `/gi join|leave channelName` (Currently stored per session yo have to re-enter on each login)
- New feature/command: global chat can be turned off by user with `/gi leave global`

### Version 0.1.0:
- initial plugin, basic features