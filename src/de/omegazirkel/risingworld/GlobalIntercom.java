/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.omegazirkel.risingworld.WSClientEndpoint.MessageHandler;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
// import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
// import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
// import javax.imageio.ImageWriteParam;
// import javax.imageio.ImageWriter;

import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;

/**
 *
 * @author Maik Laschober
 */
public class GlobalIntercom extends Plugin implements Listener, MessageHandler, FileChangeListener {

	static final String pluginVersion = "0.10.2";
	static final String pluginName = "GlobalIntercom";
	static final String pluginCMD = "gi";

	static final de.omegazirkel.risingworld.tools.Logger log = new de.omegazirkel.risingworld.tools.Logger("[OZ.GI]");
	static final Colors c = Colors.getInstance();
	private static I18n t = null;

	// Settings
	static int logLevel = 0;
	static boolean restartOnUpdate = true;
	static boolean sendPluginWelcome = false;
	static boolean joinDefault = false;
	static URI webSocketURI;
	static String defaultChannel = "global";

	static String colorOther = "[#3881f7]";
	static String colorSelf = "[#37f7da]";
	static String colorLocal = "[#FFFFFF]";

	static boolean allowScreenshots = true;
	static int maxScreenWidth = 1920;
	// END Settings

	static boolean flagRestart = false;

	// WebSocket
	static WSClientEndpoint ws;

	static final Map<String, GlobalIntercomPlayer> playerMap = new HashMap<String, GlobalIntercomPlayer>();

	@Override
	public void onEnable() {
		t = t != null ? t : new I18n(this);
		registerEventListener(this);
		this.initSettings();
		this.initWebSocketClient();
		log.out(pluginName + " Plugin is enabled", 10);
	}

	/**
	 * todo: check if we can move gsb and gson to elsewhere and dont recreate them
	 * everytime
	 *
	 * this Method converts a WSMessage to JSON and sends it through WS
	 */
	private void transmitMessageWS(Player player, WSMessage<?> wsmsg) {
		GsonBuilder gsb = new GsonBuilder();
		Gson gson = gsb.create();
		String lang = player.getSystemLanguage();

		try {
			if (ws.isConnected) {
				String msg = gson.toJson(wsmsg);
				ws.sendMessage(msg);
			} else {
				player.sendTextMessage(c.error + pluginName + ":> " + c.text + t.get("MSG_WS_OFFLINE", lang));
			}
		} catch (Exception e) {
			player.sendTextMessage(c.error + pluginName + ":>" + c.text + " " + e.getMessage());
			this.initWebSocketClient();
		}
	}

	@Override
	public void onDisable() {
		try {
			// TODO: unload everything!
			if (ws != null) {
				ws.session.close();
				ws = null;
			}
		} catch (IOException ex) {
			Logger.getLogger(GlobalIntercom.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		Player player = event.getPlayer();
		PlayerOnlineMessage msg = new PlayerOnlineMessage(player);
		WSMessage<PlayerOnlineMessage> wsmsg = new WSMessage<>("playerOnline", msg);

		this.transmitMessageWS(player, wsmsg);
	}

	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		Server server = getServer();
		Player player = event.getPlayer();
		PlayerOfflineMessage msg = new PlayerOfflineMessage(player);
		WSMessage<PlayerOfflineMessage> wsmsg = new WSMessage<>("playerOffline", msg);

		this.transmitMessageWS(player, wsmsg);

		if (flagRestart) {
			int playersLeft = server.getPlayerCount() - 1;
			if (playersLeft == 0) {
				log.out("Last player left the server, shutdown now due to flagRestart is set", 100); // INFO LEVEL
				server.shutdown();
			} else if (playersLeft > 1) {
				this.broadcastMessage("BC_PLAYER_REMAIN", playersLeft);
			}
		}
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String command = event.getCommand();
		String lang = event.getPlayer().getSystemLanguage();
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");

		String[] cmd = command.split(" ");

		if (cmd[0].equals("/" + pluginCMD)) {
			// Invalid number of arguments (0)
			if (cmd.length < 2) {
				player.sendTextMessage(c.error + pluginName + ":>" + c.text
						+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
								.replace("PH_COMMAND_HELP", c.command + "/" + pluginCMD + " help\n" + c.text));
				return;
			}
			String option = cmd[1];
			// String channel = defaultChannel;
			switch (option) {
			case "save":
				if (cmd.length > 2) {
					if (cmd[2].toLowerCase().contentEquals("true")) {
						WSMessage<PlayerRegisterMessage> wsmsg = new WSMessage<>("registerPlayer",
								new PlayerRegisterMessage(player));
						this.transmitMessageWS(player, wsmsg);
					} else {
						WSMessage<PlayerUnregisterMessage> wsmsg = new WSMessage<>("unregisterPlayer",
								new PlayerUnregisterMessage(player));
						this.transmitMessageWS(player, wsmsg);
					}
				} else {
					player.sendTextMessage(c.error + pluginName + ":>" + c.text
							+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
									.replace("PH_COMMAND_HELP",
											c.command + "/" + pluginCMD + " save true|false\n" + c.text));
				}
				break;
			case "create": {
				if (cmd.length > 2) {
					PlayerCreateChannelMessage msg = new PlayerCreateChannelMessage(player);
					msg.channel = cmd[2].toLowerCase();
					if (cmd.length > 3) {
						msg.password = cmd[3];
					}
					WSMessage<PlayerCreateChannelMessage> wsmsg = new WSMessage<>("playerCreateChannel", msg);
					this.transmitMessageWS(player, wsmsg);
				} else {
					player.sendTextMessage(c.error + pluginName + ":>" + c.text
							+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
									.replace("PH_COMMAND_HELP",
											c.command + "/" + pluginCMD + " create channelname [password]\n" + c.text));
				}
			}
				break;
			case "close": {
				if (cmd.length > 2) {
					PlayerCloseChannelMessage msg = new PlayerCloseChannelMessage(player);
					msg.channel = cmd[2].toLowerCase();
					WSMessage<PlayerCloseChannelMessage> wsmsg = new WSMessage<>("playerCloseChannel", msg);
					this.transmitMessageWS(player, wsmsg);
				} else {
					player.sendTextMessage(c.error + pluginName + ":>" + c.text
							+ t.get("MSG_CMD_ERR_ARGUMENTS", lang).replace("PH_CMD", c.error + command + c.text)
									.replace("PH_COMMAND_HELP",
											c.command + "/" + pluginCMD + " close channelname\n" + c.text));
				}
			}
				break;
			case "join": {
				PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);

				if (cmd.length > 2) {
					msg.channel = cmd[2].toLowerCase();
				} else {
					msg.channel = defaultChannel;
				}
				if (cmd.length > 3) {
					msg.password = cmd[3];
				}
				WSMessage<PlayerJoinChannelMessage> wsmsg = new WSMessage<>("playerJoinChannel", msg);
				this.transmitMessageWS(player, wsmsg);
			}
				break;
			case "leave": {

				PlayerLeaveChannelMessage msg = new PlayerLeaveChannelMessage(player);
				if (cmd.length > 2) {
					msg.channel = cmd[2].toLowerCase();
				} else {
					msg.channel = defaultChannel;
				}
				WSMessage<PlayerLeaveChannelMessage> wsmsg = new WSMessage<>("playerLeaveChannel", msg);
				this.transmitMessageWS(player, wsmsg);

			}
				break;
			case "info":
				String infoMessage = t.get("CMD_INFO", lang);
				player.sendTextMessage(c.okay + pluginName + ":> " + infoMessage);
				break;
			case "help":
				String helpMessage = t.get("CMD_HELP", lang)
						.replace("PH_CMD_JOIN", c.command + "/" + pluginCMD + " join channelname [password]" + c.text)
						.replace("PH_CMD_LEAVE", c.command + "/" + pluginCMD + " leave channelname" + c.text)
						.replace("PH_CMD_CHAT_DEFAULT", c.command + "#HelloWorld" + c.text)
						.replace("PH_CMD_CHAT_OTHER", c.command + "##other HelloWorld" + c.text)
						.replace("PH_CMD_CHAT_LOCAL", c.command + "#%local HelloWorld" + c.text)
						.replace("PH_CMD_OVERRIDE", c.command + "/" + pluginCMD + " override true|false" + c.text)
						.replace("PH_CMD_HELP", c.command + "/" + pluginCMD + " help" + c.text)
						.replace("PH_CMD_INFO", c.command + "/" + pluginCMD + " info" + c.text)
						.replace("PH_CMD_STATUS", c.command + "/" + pluginCMD + " status" + c.text)
						.replace("PH_CMD_CREATE",
								c.command + "/" + pluginCMD + " create channelname [password]" + c.text)
						.replace("PH_CMD_CLOSE", c.command + "/" + pluginCMD + " close channelname" + c.text)
						.replace("PH_CMD_SAVE", c.command + "/" + pluginCMD + " save true|false" + c.text);
				player.sendTextMessage(c.okay + pluginName + ":> " + helpMessage);
				break;
			case "status":
				String lastCH = "lokal";
				if (player.hasAttribute("gilastch")) {
					lastCH = (String) player.getAttribute("gilastch");
				}

				String wsStatus = c.error + t.get("STATE_DISCONNECTED", lang);
				if (ws.isConnected) {
					wsStatus = c.okay + t.get("STATE_CONNECTED", lang);
				}

				String saveStatus = c.error + t.get("STATE_INACTIVE", lang);
				if (giPlayer.saveSettings) {
					saveStatus = c.okay + t.get("STATE_ACTIVE", lang);
				}

				String overrideStatus = "";
				if (giPlayer.override) {
					overrideStatus = c.okay + t.get("STATE_ON", lang);
				} else {
					overrideStatus = c.error + t.get("STATE_OFF", lang);
				}

				String statusMessage = t.get("CMD_STATUS", lang).replace("PH_VERSION", c.okay + pluginVersion + c.text)
						.replace("PH_LANGUAGE",
								colorSelf + player.getLanguage() + " / " + player.getSystemLanguage() + c.text)
						.replace("PH_USEDLANG", colorOther + t.getLanguageUsed(lang) + c.text)
						.replace("PH_LANG_AVAILABLE", c.okay + t.getLanguageAvailable() + c.text)
						.replace("PH_STATE_WS", wsStatus + c.text).replace("PH_STATE_CH", c.command + lastCH + c.text)
						.replace("PH_STATE_SAVE", saveStatus + c.text).replace("PH_STATE_OR", overrideStatus + c.text)
						.replace("PH_CHLIST", c.command + giPlayer.getChannelList() + c.text);

				player.sendTextMessage(c.okay + pluginName + ":> " + statusMessage);
				break;
			case "override":
				if (cmd.length > 2) {
					{
						PlayerOverrideChangeMessage msg = new PlayerOverrideChangeMessage(player);
						msg.override = cmd[2].toLowerCase().contentEquals("true");
						WSMessage<PlayerOverrideChangeMessage> wsmsg = new WSMessage<>("playerOverrideChange", msg);
						this.transmitMessageWS(player, wsmsg);
					}
				} else {
					String message = c.okay + pluginName + ":> " + c.text + t.get("MSG_CMD_OVERRIDE_NOTSET", lang)
							.replace("PH_CMD", c.command + "/" + pluginCMD + " override [true|false] " + c.text);
					player.sendTextMessage(message);
				}
				break;
			default:
				player.sendTextMessage(c.error + pluginName + ":> " + c.text
						+ t.get("MSG_CMD_ERR_UNKNOWN_OPTION", lang).replace("PH_OPTION", option));
				break;
			}
		}
	}

	/**
	 *
	 * @param event
	 * @return
	 */
	public boolean isGIMessage(PlayerChatEvent event) {
		Player player = event.getPlayer();
		String message = event.getChatMessage();
		String noColorText = message.replaceFirst("\\[[#0-9a-fA-F\\[\\]]+\\]#", "#");
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		boolean override = giPlayer.override;
		boolean isValidLastChannel = override && player.hasAttribute("gilastch")
				&& giPlayer.isInChannel((String) player.getAttribute("gilastch"));

		// its a GI message if it starts with # or gilastch is set with override true
		// AND chat doesnt start with #%
		return (noColorText.startsWith("#") || (isValidLastChannel && !noColorText.startsWith("#%")));
	}

	@EventMethod
	public void onPlayerChat(PlayerChatEvent event) {

		Player player = event.getPlayer();
		Server server = getServer();
		String message = event.getChatMessage();
		String chatMessage;
		String channel;
		String lang = event.getPlayer().getSystemLanguage();
		// log.out("message: "+message,0);
		String noColorText = message.replaceFirst("\\[[#0-9a-fA-F\\[\\]]+\\]#", "#");
		// log.out("noColorText: "+noColorText,0);
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		if (giPlayer == null) {
			if (noColorText.startsWith("#")) {
				player.sendTextMessage(c.error + pluginName + ":> " + c.text + t.get("MSG_ERR_GI_INIT", lang));
				event.setCancelled(true);
			}
			return;
		}
		boolean override = giPlayer.override;

		// long uid = player.getUID();

		if (noColorText.startsWith("#%")) {
			// reset to local chat
			player.deleteAttribute("gilastch");
			if (noColorText.substring(2).length() > 0) {
				event.setChatMessage(colorLocal + noColorText.substring(2));
			} else {
				player.sendTextMessage(c.okay + pluginName + ":>" + c.text + t.get("MSG_INFO_CH_DEFAULT_RESET", lang));
				event.setCancelled(true); // No text, don't proceed
			}
			return;
		} else if (noColorText.startsWith("#")) {
			if (noColorText.startsWith("##")) {
				// this is a message into a special channel
				String[] msgParts = noColorText.substring(2).split(" ", 2);
				channel = msgParts[0].toLowerCase();
				if (msgParts.length > 1) {
					chatMessage = msgParts[1];
				} else {
					chatMessage = "";
				}
			} else {
				channel = defaultChannel;
				chatMessage = noColorText.substring(1);
			}
			if (channel.length() > 20) {
				player.sendTextMessage(c.error + pluginName + ":> " + c.text
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (channel.length() < 3) {
				player.sendTextMessage(c.error + pluginName + ":>" + c.text
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (!giPlayer.isInChannel(channel)) {
				player.sendTextMessage(c.error + pluginName + ":>" + c.text
						+ t.get("MSG_ERR_CH_NOMEMBER", lang).replace("PH_CHANNEL", channel) + "\n"
						+ t.get("MSG_INFO_CH_JOIN", lang).replace("PH_CMD_JOIN",
								c.command + "/" + pluginCMD + " join " + channel + c.text));
				event.setCancelled(true); // do not post to local chat
				return;
			}

			// Override default text channel
			if (override) {
				player.setAttribute("gilastch", channel);
			}

		} else if (player.hasAttribute("gilastch") && override) {
			channel = (String) player.getAttribute("gilastch");
			chatMessage = noColorText;
		} else {
			event.setChatMessage(colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		if (!giPlayer.isInChannel(channel)) {
			// The player is not in that channel, return to local
			player.deleteAttribute("gilastch");
			event.setChatMessage(colorLocal + noColorText);
			return; // no Global Intercom Chat message
		}

		event.setCancelled(true);
		ChatMessage cmsg = new ChatMessage(player, server, chatMessage, channel);

		if (chatMessage.contains("+screen")) {
			if (allowScreenshots == true) {
				int playerResolutionX = player.getScreenResolutionX();
				float sizeFactor = 1.0f;
				if (playerResolutionX > maxScreenWidth) {
					sizeFactor = maxScreenWidth * 1f / playerResolutionX * 1f;
				}

				player.createScreenshot(sizeFactor, (BufferedImage bimg) -> {
					final ByteArrayOutputStream os = new ByteArrayOutputStream();
					try {
						ImageIO.write(bimg, "jpg", os);
						cmsg.attachment = Base64.getEncoder().encodeToString(os.toByteArray());
					} catch (Exception e) {
						// throw new UncheckedIOException(ioe);
						log.out("Exception on createScreenshot-> " + e.toString());
						// e.printStackTrace();
					}
					cmsg.chatContent = chatMessage.replace("+screen", "[screenshot.jpg]");
					WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
					transmitMessageWS(player, wsbcm);
				});
			} else {
				cmsg.chatContent = chatMessage.replace("+screen", "[noimage.jpg]");
				player.sendTextMessage(t.get("MSG_SCREEN_NOTALLOWED", lang));
				WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
				transmitMessageWS(player, wsbcm);
			}
		} else {
			WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);
			transmitMessageWS(player, wsbcm);
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if (sendPluginWelcome) {
			Player player = event.getPlayer();
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("MSG_PLUGIN_WELCOME", lang));
		}
	}

	/**
	 *
	 */
	private void initWebSocketClient() {
		// test
		try {
			ws = WSClientEndpoint.getInstance(webSocketURI);
			ws.setMessageHandler(this);
		} catch (Exception e) {
			log.out(e.getMessage(), 999);
			e.printStackTrace();
		}
	}

	/**
	 *
	 */
	private void initSettings() {
		Properties settings = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(getPath() + "/settings.properties");
			settings.load(new InputStreamReader(in, "UTF8"));
			in.close();
			// fill global values
			logLevel = Integer.parseInt(settings.getProperty("logLevel", "1"));
			webSocketURI = new URI(settings.getProperty("webSocketURI", "wss://rw.gi.omega-zirkel.de/ws"));
			defaultChannel = settings.getProperty("defaultChannel", "global");
			joinDefault = settings.getProperty("joinDefault", "true").contentEquals("true");
			colorOther = settings.getProperty("colorOther", "[#3881f7]");
			colorSelf = settings.getProperty("colorSelf", "[#37f7da]");
			colorLocal = settings.getProperty("colorLocal", "[#FFFFFF]");

			sendPluginWelcome = settings.getProperty("sendPluginWelcome", "true").contentEquals("true");

			allowScreenshots = settings.getProperty("allowScreenshots", "true").contentEquals("true");
			maxScreenWidth = Integer.parseInt(settings.getProperty("maxScreenWidth", "1920"));

			// restart settings
			restartOnUpdate = settings.getProperty("restartOnUpdate", "false").contentEquals("true");
			log.out(pluginName + " Plugin settings loaded", 10);
		} catch (IOException ex) {
			log.out("IOException on initSettings: " + ex.getMessage(), 100);
			// e.printStackTrace();
		} catch (NumberFormatException ex) {
			log.out("NumberFormatException on initSettings: " + ex.getMessage(), 100);
		} catch (URISyntaxException ex) {
			log.out("Exception on initSettings: " + ex.getMessage(), 100);
		}
	}

	/**
	 *
	 * @version 0.8.1
	 * @param cmsg
	 */
	private void broadcastMessage(ChatMessage cmsg) {
		getServer().getAllPlayers().forEach((player) -> {
			if (!playerMap.containsKey(player.getUID() + "")) {
				return; // Player not initialized with GI
			}
			GlobalIntercomPlayer gip = playerMap.get(player.getUID() + "");
			if (gip.isInChannel(cmsg.chatChannel)) {
				String color = colorOther;
				if ((player.getUID() + "").contentEquals(cmsg.playerUID)) {
					color = colorSelf;
				}
				player.sendTextMessage(color + "[" + cmsg.chatChannel.toUpperCase() + "] " + cmsg.playerName + ": "
						+ c.text + cmsg.chatContent);
			}
		});
	}

	@Override
	public void handleMessage(String message) {
		// log.out(message, 0);
		WSMessage wsm = new Gson().fromJson(message, WSMessage.class);
		if (wsm.event.contentEquals("broadcastMessage")) {
			Type type = new TypeToken<WSMessage<ChatMessage>>() {
			}.getType();
			// System.out.println(type);
			WSMessage<ChatMessage> wscmsg = new Gson().fromJson(message, type);
			// System.out.println(wscmsg.toString());
			ChatMessage cmsg = wscmsg.payload;
			log.out("New BC Message <" + cmsg.chatContent + "> from " + cmsg.playerName, 0);
			this.broadcastMessage(cmsg);
		} else {
			Type type = new TypeToken<WSMessage<GlobalIntercomPlayer>>() {
			}.getType();
			WSMessage<GlobalIntercomPlayer> wsmsg = new Gson().fromJson(message, type);
			GlobalIntercomPlayer giPlayer = wsmsg.payload;
			playerMap.put(giPlayer._id, giPlayer);
			Player player = getServer().getPlayer(Long.parseLong(giPlayer._id));
			String lang = player.getSystemLanguage();

			if (wsm.event.contentEquals("directContactMessage")) {
				// Not yet implemented
			}
			// else if (wsm.event.contentEquals("registerPlayer")) {
			// player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
			// t.get("MSG_REGISTERED", lang));
			// } else if (wsm.event.contentEquals("unregisterPlayer")) {
			// player.sendTextMessage(c.okay + pluginName + ":> " + c.text +
			// t.get("MSG_UNREGISTERED", lang));
			// }
			else if (wsm.event.contentEquals("playerOnline")) {
				if (!giPlayer.saveSettings && joinDefault && !giPlayer.isInChannel(defaultChannel)) {
					PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
					msg.channel = defaultChannel;
					this.transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
					// event.getPlayer().setAttribute("gi." + defaultChannel, true);
					// String lang = event.getPlayer().getSystemLanguage();
				}
			} else if (wsm.event.contentEquals("playerOffline")) {
				// Currently nothing to do here
			} else if (wsm.event.contentEquals("playerOverrideChange")) {
				{
					boolean newVal = wsmsg.subject.contentEquals("true");
					String msg = c.okay + pluginName + ":> " + c.text + t.get("MSG_CMD_OVERRIDE_STATE", lang);

					if (newVal) {
						msg = msg.replace("PH_STATE", c.okay + t.get("STATE_ON", lang) + c.text);
					} else {
						msg = msg.replace("PH_STATE", c.error + t.get("STATE_OFF", lang) + c.text);
					}

					player.sendTextMessage(msg);
				}
			} else if (wsm.event.contentEquals("playerJoinChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(
						c.okay + pluginName + ":> " + c.text + t.get("MSG_JOIN", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerLeaveChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(c.warning + pluginName + ":> " + c.text
						+ t.get("MSG_LEAVE", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerCreateChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(
						c.okay + pluginName + ":> " + c.text + t.get("MSG_CREATE", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerResponseError")) {
				String code = wsmsg.errorCode;
				String baseMessage = c.error + pluginName + ":> " + c.text + t.get(code, lang);
				switch (code) {
				case "RELAY_CHANNEL_NOTMEMBER":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					baseMessage = baseMessage.replace("PH_CMD",
							c.command + "/" + pluginCMD + " join " + wsmsg.subject + " [password]" + c.text);
					break;
				case "RELAY_UNREGISTER_CHOWNER":
					// no placeholder
					break;
				case "RELAY_JOIN_NOACCESS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CHANNEL_UNKNOWN":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					baseMessage = baseMessage.replace("PH_CMD",
							c.command + "/" + pluginCMD + " create " + wsmsg.subject + " [password]" + c.text);
					break;
				case "RELAY_LEAVE_OWNER":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					baseMessage = baseMessage.replace("PH_CMD",
							c.command + "/" + pluginCMD + " close " + wsmsg.subject + c.text);
					break;
				case "RELAY_CREATE_NOTREGISTERED":
					// no replacements
					break;
				case "RELAY_CREATE_NOGLOBAL":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CREATE_LENGTH":
					break;
				case "RELAY_CREATE_EXISTS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					baseMessage = baseMessage.replace("PH_CMD",
							c.command + "/" + pluginCMD + " join " + wsmsg.subject + c.text);
					break;
				case "RELAY_CH_CLOSE_NOTEXISTS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CH_CLOSE_NOTOWNER":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CH_CLOSED":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				default:
					break;
				}
				player.sendTextMessage(baseMessage);
			} else if (wsm.event.contentEquals("playerResponseSuccess")) {
				String code = wsmsg.successCode;
				String baseMessage = c.okay + pluginName + ":> " + c.text + t.get(code, lang);
				switch (code) {
				case "RELAY_SUCCESS_REGISTER":
					// no placeholder
					break;
				case "RELAY_SUCCESS_UNREGISTER":
					// no placeholder
					break;
				case "RELAY_JOIN_SUCCESS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_LEAVE_SUCCESS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CREATE_SUCCESS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				case "RELAY_CH_CLOSE_SUCCESS":
					baseMessage = baseMessage.replace("PH_CHANNEL", c.warning + wsmsg.subject + c.text);
					break;
				default:
					break;
				}
				player.sendTextMessage(baseMessage);
			} else if (wsm.event.contentEquals("playerResponseInfo")) {
				String code = wsmsg.infoCode;
				String baseMessage = c.text + pluginName + ":> " + c.text + t.get(code, lang);
				// switch (code) {

				// default:
				// break;
				// }
				player.sendTextMessage(baseMessage);
			} else {
				log.out("Unknown message type <" + wsm.event + ">");
			}
		}
	}

	// All stuff for plugin updates

	/**
	 *
	 * @param i18nIndex
	 * @param playerCount
	 */
	private void broadcastMessage(String i18nIndex, int playerCount) {
		getServer().getAllPlayers().forEach((player) -> {
			try {
				String lang = player.getSystemLanguage();
				player.sendTextMessage(c.warning + pluginName + ":> " + c.text
						+ t.get(i18nIndex, lang).replace("PH_PLAYERS", playerCount + ""));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onFileChangeEvent(Path file) {
		if (file.toString().endsWith("jar")) {
			if (restartOnUpdate) {
				Server server = getServer();

				if (server.getPlayerCount() > 0) {
					flagRestart = true;
					this.broadcastMessage("BC_UPDATE_FLAG", server.getPlayerCount());
				} else {
					log.out("onFileCreateEvent: <" + file + "> changed, restarting now (no players online)", 100);
				}

			} else {
				log.out("onFileCreateEvent: <" + file + "> changed but restartOnUpdate is false", 0);
			}
		} else {
			log.out("onFileCreateEvent: <" + file + ">", 0);
		}
	}

	@Override
	public void onFileCreateEvent(Path file) {
		if (file.toString().endsWith("settings.properties")) {
			this.initSettings();
		} else {
			log.out(file.toString() + " was changed", 0);
		}
	}
}
