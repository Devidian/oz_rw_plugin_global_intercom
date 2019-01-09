### Version 0.7.1:
- fixed: null pointer exception onDisable()

### Version 0.7.0:
- new: new settings `joinDefault=false`. If you set this to `true`, players will join default channel on connect (~ like before < 0.7.0). This allows servers to deactivate default channels for players that don't like to chat and might get annoyed when they have to leave the channel everytime.
- fixed: if you try posting into a GI channel that you have not joined yet, you will get a notice but your text does not proceed to local chat anymore.
- fixed: if you just want to change your chatOverride channel back to local you can now do this with just typing `#%`. There will be no empty message now. 

### Version 0.6.1:
- changed: `/gi info` slightly changed output
- added: `/gi status` shows now plugin version
- misc: project is now a maven project

### Version 0.6.0:
- changed: replaced MongoDB libs with WebSocket, no direct database access from now on.

### Version 0.5.0:
- fixed: in local chat the first to letters were cut off accidently
- changed: chat-override is now off (false) by default, turn it on in your `settings.properties` if you like
- new command: you can force chat-override to be on or off by yourself as player. Just type `/gi override [true|false]` to override server settings (this is currently stored per session, you have to set it on every new login to mp or sp game)

### Version 0.4.1:
- Change/Fix: whitespaces before and after chat message will now be removed

### Version 0.4.0:
- new feature: gi remembers your last channel, just press enter and write! Type `#%text` to return to yourl local server chat. (this is stored per session and returns to default for each login)
- new feature: Chat colors can be adjusted in `settings.properties`
- new command: `/gi status` shows your current chat channel and the database connection status.
- changed: Only channel and player name are colored, text is white (default)

### Version 0.3.1:
- fixed: reinstalled database to fix access, changed default port to 47017

### Version 0.3.0:
- new command: `/gi info` shows usage
- new: you can now configure motd in the `settings.properties` file
- new: you can now change the default channel for your server in the `settings.properties` file (default: `global`)

### Version 0.2.0:
- new feature/command: Custom-Chat-Channels `/gi join|leave channelName` (Currently stored per session yo have to re-enter on each login)
- new feature/command: global chat can be turned off by user with `/gi leave global`

### Version 0.1.0:
- initial plugin, basic features