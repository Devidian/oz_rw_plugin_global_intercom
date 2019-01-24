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
import de.omegazirkel.risingworld.tools.I18n;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class GlobalIntercom extends Plugin implements Listener, MessageHandler {

	static final String pluginVersion = "0.8.0";
	static final String pluginName = "GlobalIntercom";

	static final de.omegazirkel.risingworld.tools.Logger log = new de.omegazirkel.risingworld.tools.Logger("[OZ.GI]");
	private static I18n t = null;

	static final String colorError = "[#FF0000]";
	static final String colorWarning = "[#808000]";
	static final String colorOkay = "[#00FF00]";
	static final String colorText = "[#EEEEEE]";
	static final String colorCommand = "[#997d4a]";

	// Settings
	static int logLevel = 0;
	static boolean restartOnUpdate = true;
	static boolean joinDefault = false;
	static URI webSocketURI;
	static String defaultChannel = "global";
	static boolean sendMOTD = false;
	static String motd = colorText + "This Server uses " + colorOkay + "Global Intercom" + colorText + " Plugin. Type "
			+ colorCommand + "/gi info" + colorText + " for more info";

	static String colorOther = "[#3881f7]";
	static String colorSelf = "[#37f7da]";
	static String colorLocal = "[#FFFFFF]";
	// END Settings

	// WebSocket
	static WSClientEndpoint ws;

	static final Map<String, GlobalIntercomPlayer> playerMap = new HashMap<String, GlobalIntercomPlayer>();

	@Override
	public void onEnable() {
		t = t != null ? t : new I18n(this);
		registerEventListener(this);
		this.initSettings();
		this.initWebSocketClient();
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
		String lang = player.getLanguage();

		try {
			if (ws.isConnected) {
				String msg = gson.toJson(wsmsg);
				ws.sendMessage(msg);
			} else {
				player.sendTextMessage(colorError + pluginName + ":> " + colorText + t.get("MSG_WS_OFFLINE", lang));
			}
		} catch (Exception e) {
			player.sendTextMessage(colorError + pluginName + ":>" + colorText + " " + e.getMessage());
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
		Player player = event.getPlayer();
		PlayerOfflineMessage msg = new PlayerOfflineMessage(player);
		WSMessage<PlayerOfflineMessage> wsmsg = new WSMessage<>("playerOffline", msg);

		this.transmitMessageWS(player, wsmsg);
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String command = event.getCommand();
		String lang = event.getPlayer().getLanguage();
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");

		String[] cmd = command.split(" ");

		if (cmd[0].equals("/gi")) {
			String option = cmd[1];
			String channel = defaultChannel;
			if (cmd.length > 2) {
				channel = cmd[2].toLowerCase();
			}
			if (option.isEmpty() || channel.isEmpty() || channel.length() < 3 || channel.length() > 10) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText + " Invalid call " + command
						+ "\nUsage: " + colorCommand + "/gi [join|leave|create] [channelname]\n" + colorText
						+ "channelname must be at least 3 but max 10 letters");
				return;
			}
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
					player.sendTextMessage(colorError + pluginName + ":>" + colorText
							+ t.get("MSG_CMD_ERR_ARGUMENTS", lang)
									.replace("PH_COMMAND", colorError + command + colorText)
									.replace("PH_COMMAND_HELP", colorCommand + "/gi save [true|false]\n" + colorText));
				}
			case "create": {
				PlayerCreateChannelMessage msg = new PlayerCreateChannelMessage(player);
				if (cmd.length > 3) {
					msg.password = cmd[3];
				}
				WSMessage<PlayerCreateChannelMessage> wsmsg = new WSMessage<>("playerCreateChannel", msg);
				this.transmitMessageWS(player, wsmsg);
			}
				break;
			case "join": {

				PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
				msg.channel = channel;
				if (cmd.length > 3) {
					msg.password = cmd[3];
				}
				WSMessage<PlayerJoinChannelMessage> wsmsg = new WSMessage<>("playerJoinChannel", msg);
				this.transmitMessageWS(player, wsmsg);
			}
				break;
			case "leave": {

				PlayerLeaveChannelMessage msg = new PlayerLeaveChannelMessage(player);
				msg.channel = channel;
				WSMessage<PlayerLeaveChannelMessage> wsmsg = new WSMessage<>("playerLeaveChannel", msg);
				this.transmitMessageWS(player, wsmsg);

			}
				break;
			case "help":
			case "info":
				player.sendTextMessage(colorOkay + pluginName + ":> USAGE\n" + colorText + "Join or leave channel "
						+ colorCommand + "/gi [join|leave] [channelname] \n" + colorText
						+ "Write <HelloWorld> to default channel: " + colorCommand + "#HelloWorld\n" + colorText
						+ "Write <HelloWorld> to other channel: " + colorCommand + "##other HelloWorld\n" + colorText
						+ "Reset default channel to local: " + colorCommand + "#%text\n" + colorText
						+ "Change chat-override: " + colorCommand + "/gi override [true|false]\n" + colorText
						+ "Show (this) help: " + colorCommand + "/gi [help|info]\n" + colorText + "Show Plugin status: "
						+ colorCommand + "/gi status");
				break;
			case "status":
				String lastCH = "lokal";
				if (player.hasAttribute("gilastch")) {
					lastCH = (String) player.getAttribute("gilastch");
				}

				String wsStatus = colorError + t.get("STATE_DISCONNECTED", lang);
				if (ws.isConnected) {
					wsStatus = colorOkay + t.get("STATE_CONNECTED", lang);
				}

				String saveStatus = colorError + t.get("STATUS_SAVE_INACTIVE", lang);
				if (giPlayer.saveSettings) {
					saveStatus = colorOkay + t.get("STATUS_SAVE_ACTIVE", lang);
				}

				String overrideStatus = "";
				if (giPlayer.override) {
					overrideStatus = colorOkay + t.get("STATE_ON", lang);
				} else {
					overrideStatus = colorError + t.get("STATE_OFF", lang);
				}

				player.sendTextMessage(colorOkay + pluginName + ":> STATUS\n" + colorText + "Plugin version: "
						+ colorOkay + pluginVersion + "\n" + colorText + "WebSocket: " + wsStatus + "\n" + colorText
						+ t.get("STATUS_DEFAULT_CH", lang) + ": " + colorCommand + lastCH + "\n" + colorText
						+ t.get("STATUS_SAVE", lang) + ": " + saveStatus + "\n" + colorText
						+ t.get("MSG_INFO_OVERRIDE_STATE", lang).replace("PH_STATE", overrideStatus) + "\n"
						+ colorText);
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
					String message = colorOkay + pluginName + ":> " + colorText + t.get("MSG_CMD_OVERRIDE_NOTSET", lang)
							.replace("PH_CMD", colorCommand + "/gi override [true|false] " + colorText);
					player.sendTextMessage(message);
				}
				break;
			default:
				player.sendTextMessage(colorError + pluginName + ":> " + colorText
						+ t.get("MSG_ERR_CMD_UNKNOWN_OPTION", lang).replace("PH_OPTION", option));
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
		String noColorText = message.replaceFirst("(\\[#[a-fA-F]+\\])", "");
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		boolean override = giPlayer.override;

		// its a GI message if it starts with # or gilastch is set with override true
		// AND chat doesnt start with #%
		return (noColorText.startsWith("#") || (player.hasAttribute("gilastch") && override))
				&& !noColorText.startsWith("#%");
	}

	@EventMethod
	public void onPlayerChat(PlayerChatEvent event) {

		Player player = event.getPlayer();
		Server server = getServer();
		String message = event.getChatMessage();
		String chatMessage;
		String channel;
		String lang = event.getPlayer().getLanguage();
		String noColorText = message.replaceFirst("(\\[#[a-fA-F]+\\])", "");
		GlobalIntercomPlayer giPlayer = playerMap.get(player.getUID() + "");
		if (giPlayer == null) {
			if (noColorText.startsWith("#")) {
				player.sendTextMessage(colorError + pluginName + ":> " + colorText + t.get("MSG_ERR_GI_INIT", lang));
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
				player.sendTextMessage(
						colorOkay + pluginName + ":>" + colorText + t.get("MSG_INFO_CH_DEFAULT_RESET", lang));
				event.setCancelled(true); // No text, don't proceed
			}
			return;
		} else if (noColorText.startsWith("#")) {
			if (noColorText.startsWith("##")) {
				// this is a message into a special channel
				String[] msgParts = noColorText.substring(2).split(" ", 2);
				channel = msgParts[0].toLowerCase();
				chatMessage = msgParts[1];
			} else {
				channel = defaultChannel;
				chatMessage = noColorText.substring(1);
			}
			if (channel.length() > 20) {
				player.sendTextMessage(colorError + pluginName + ":> " + colorText
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (channel.length() < 3) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText
						+ t.get("MSG_ERR_CH_LENGTH", lang).replace("PH_CHANNEL", channel));
				event.setCancelled(true); // do not post to local chat
				return;
			} else if (!giPlayer.isInChannel(channel)) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText
						+ t.get("MSG_ERR_CH_NOMEMBER", lang).replace("PH_CHANNEL", channel) + "\n"
						+ t.get("MSG_INFO_CH_JOIN", lang).replace("PH_CMD_JOIN",
								colorCommand + "/gi join " + channel + colorText));
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

		event.setCancelled(true);
		ChatMessage cmsg = new ChatMessage(player, server, chatMessage, channel);
		WSMessage<ChatMessage> wsbcm = new WSMessage<>("broadcastMessage", cmsg);

		GsonBuilder gsb = new GsonBuilder();
		Gson gson = gsb.create();

		try {
			if (ws.isConnected) {
				String msg = gson.toJson(wsbcm);
				// log.out("sending..."+msg,0);
				ws.sendMessage(msg);
			} else {
				player.sendTextMessage(colorError + pluginName + ":> " + colorText + t.get("MSG_WS_OFFLINE", lang));
			}
		} catch (Exception e) {
			player.sendTextMessage(colorError + pluginName + ":>" + colorText + " " + e.getMessage());
			this.initWebSocketClient();
		}
	}

	/**
	 *
	 * @param event
	 */
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if (sendMOTD) {
			Player player = event.getPlayer();
			player.sendTextMessage(motd);
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
			logLevel = Integer.parseInt(settings.getProperty("logLevel"));
			webSocketURI = new URI(settings.getProperty("webSocketURI"));
			defaultChannel = settings.getProperty("defaultChannel");
			joinDefault = settings.getProperty("joinDefault").contentEquals("true");
			colorOther = settings.getProperty("colorOther");
			colorSelf = settings.getProperty("colorSelf");
			colorLocal = settings.getProperty("colorLocal");

			// motd settings
			sendMOTD = settings.getProperty("sendMOTD").contentEquals("true");
			motd = settings.getProperty("motd");

			// restart settings
			restartOnUpdate = settings.getProperty("restartOnUpdate").contentEquals("true");
			log.out("GlobalIntercom Plugin is enabled", 10);

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
	 * @version 0.8.0
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
						+ colorText + cmsg.chatContent);
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
			String lang = player.getLanguage();

			if (wsm.event.contentEquals("directContactMessage")) {
				// Not yet implemented
			} else if (wsm.event.contentEquals("registerPlayer")) {
				player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + t.get("MSG_REGISTERED", lang));
			} else if (wsm.event.contentEquals("unregisterPlayer")) {
				player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + t.get("MSG_UNREGISTERED", lang));
			} else if (wsm.event.contentEquals("playerOnline")) {
				if (!giPlayer.saveSettings && joinDefault && !giPlayer.isInChannel(defaultChannel)) {
					PlayerJoinChannelMessage msg = new PlayerJoinChannelMessage(player);
					msg.channel = defaultChannel;
					this.transmitMessageWS(player, new WSMessage<>("playerJoinChannel", msg));
					// event.getPlayer().setAttribute("gi." + defaultChannel, true);
					// String lang = event.getPlayer().getLanguage();
				}
			} else if (wsm.event.contentEquals("playerOffline")) {
				// Currently nothing to do here
			} else if (wsm.event.contentEquals("playerOverrideChange")) {
				{
					boolean newVal = wsmsg.subject.contentEquals("true");
					String msg = colorOkay + pluginName + ":> " + colorText + t.get("MSG_CMD_OVERRIDE_STATE", lang);

					if (newVal) {
						msg = msg.replace("PH_STATE", colorOkay + t.get("STATE_ON", lang) + colorText);
					} else {
						msg = msg.replace("PH_STATE", colorError + t.get("STATE_OFF", lang) + colorText);
					}

					player.sendTextMessage(msg);
				}
			} else if (wsm.event.contentEquals("playerJoinChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(colorOkay + pluginName + ":> " + colorText
						+ t.get("MSG_JOIN", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerLeaveChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(colorWarning + pluginName + ":> " + colorText
						+ t.get("MSG_LEAVE", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerCreateChannel")) {
				String chName = wsmsg.subject;
				player.sendTextMessage(colorOkay + pluginName + ":> " + colorText
						+ t.get("MSG_CREATE", lang).replace("PH_CHANNEL", chName));
			} else if (wsm.event.contentEquals("playerResponseError")) {
				String code = wsmsg.errorCode;
				String baseMessage = colorError + pluginName + ":> " + colorText + t.get(code, lang);
				switch (code) {
				case "RELAY_CHANNEL_NOTMEMBER":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					baseMessage.replace("PH_CMD",
							colorCommand + "/gi join " + wsmsg.subject + " [password]" + colorText);
					break;
				case "RELAY_UNREGISTER_CHOWNER":
					// no placeholder
					break;
				case "RELAY_JOIN_NOACCESS":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					break;
				case "RELAY_CHANNEL_UNKNOWN":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					baseMessage.replace("PH_CMD",
							colorCommand + "/gi create " + wsmsg.subject + " [password]" + colorText);
					break;
				case "RELAY_LEAVE_OWNER":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					baseMessage.replace("PH_CMD", colorCommand + "/gi close " + wsmsg.subject + colorText);
					break;
				default:
					break;
				}
				player.sendTextMessage(baseMessage);
			} else if (wsm.event.contentEquals("playerResponseSuccess")) {
				String code = wsmsg.successCode;
				String baseMessage = colorOkay + pluginName + ":> " + colorText + t.get(code, lang);
				switch (code) {
				case "RELAY_SUCCESS_REGISTER":
					// no placeholder
					break;
				case "RELAY_SUCCESS_UNREGISTER":
					// no placeholder
					break;
				case "RELAY_JOIN_SUCCESS":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					break;
				case "RELAY_LEAVE_SUCCESS":
					baseMessage.replace("PH_CHANNEL", colorWarning + wsmsg.subject + colorText);
					break;

				default:
					break;
				}
				player.sendTextMessage(baseMessage);
			} else if (wsm.event.contentEquals("playerResponseInfo")) {
				String code = wsmsg.infoCode;
				String baseMessage = colorText + pluginName + ":> " + colorText + t.get(code, lang);
				// switch (code) {

				// default:
				// 	break;
				// }
				player.sendTextMessage(baseMessage);
			} else {
				log.out("Unknown message type <" + wsm.event + ">");
			}
		}
	}
}
