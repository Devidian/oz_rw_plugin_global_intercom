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
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;

/**
 *
 * @author Maik Laschober
 */
public class GlobalIntercom extends Plugin implements Listener, MessageHandler {

	static final String pluginVersion = "0.6.1";
	static final String pluginName = "GlobalIntercom";

	static final String colorError = "[#FF0000]";
	static final String colorWarning = "[#808000]";
	static final String colorOkay = "[#00FF00]";
	static final String colorText = "[#EEEEEE]";
	static final String colorCommand = "[#997d4a]";

	// Settings
	static int logLevel = 0;
	static boolean restartOnUpdate = true;
	static boolean overrideDefault = true;
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

	@Override
	public void onEnable() {
		registerEventListener(this);
		this.initSettings();
		this.initWebSocketClient();
	}

	@Override
	public void onDisable() {
		try {
			// TODO: unload everything!
			ws.session.close();
			ws = null;
		} catch (IOException ex) {
			Logger.getLogger(GlobalIntercom.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String command = event.getCommand();

		String[] cmd = command.split(" ");

		if (cmd[0].equals("/gi")) {
			String option = cmd[1];
			String channel = defaultChannel;
			if (cmd.length > 2) {
				channel = cmd[2].toLowerCase();
			}
			if (option.isEmpty() || channel.isEmpty() || channel.length() < 3 || channel.length() > 10) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText + " Invalid call " + command
						+ "\nUsage: " + colorCommand + "/gi [join|leave] [channelname]\n" + colorText
						+ "channelname must be at least 3 but max 10 letters");
				return;
			}
			switch (option) {
			case "join":
				player.setAttribute("gi." + channel, true);
				player.sendTextMessage(colorOkay + pluginName + ":>" + colorText + " You have joined " + channel);
				break;
			case "leave":
				player.setAttribute("gi." + channel, false);
				player.sendTextMessage(colorWarning + pluginName + ":>" + colorText + " You have left " + channel);
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

				String wsStatus = colorError + "not connected";
				if (ws.isConnected) {
					wsStatus = colorOkay + "connected";
				}

				player.sendTextMessage(colorOkay + pluginName + ":> STATUS\n" + colorText + "Plugin version: "
						+ colorOkay + pluginVersion + "\n" + colorText + "WebSocket: " + wsStatus + "\n" + colorText
						+ "Current default channel: " + colorCommand + lastCH);
				break;
			case "override":
				if (cmd.length > 2) {
					boolean setOverrideTo = cmd[2].toLowerCase().contentEquals("true");
					player.setAttribute("giOverride", setOverrideTo);
					if (setOverrideTo) {
						player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + "chat-override is now "
								+ colorOkay + "on");
					} else {
						player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + "chat-override is now "
								+ colorError + "off");
					}
				} else if (player.hasAttribute("giOverride")) {
					if ((boolean) player.getAttribute("giOverride")) {
						player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + "chat-override is now "
								+ colorOkay + "on");
					} else {
						player.sendTextMessage(colorOkay + pluginName + ":> " + colorText + "chat-override is now "
								+ colorError + "off");
					}
				} else {
					player.sendTextMessage(colorOkay + pluginName + ":> " + colorText
							+ "chat-override is not set turn on or off with /gi override [true|false] ");
				}
				break;
			default:
				player.sendTextMessage(colorError + pluginName + ":> unknown option " + option);
				break;
			}
		}
	}

	@EventMethod
	public void onPlayerChat(PlayerChatEvent event) {

		Player player = event.getPlayer();
		Server server = getServer();
		String message = event.getChatMessage();
		String chatMessage;
		String channel;
		boolean override = overrideDefault;

		if (player.hasAttribute("giOverride")) {
			override = (boolean) player.getAttribute("giOverride");
		}

		String noColorText = message.replaceFirst("(\\[#[a-fA-F]+\\])", "");
		long uid = player.getUID();

		if (noColorText.startsWith("#%")) {
			// reset to local chat
			player.deleteAttribute("gilastch");
			event.setChatMessage(colorLocal + noColorText.substring(2));
			return;
		} else if (noColorText.startsWith("##")) {
			// this is a message into a special channel
			String[] msgParts = noColorText.substring(2).split(" ", 2);
			channel = msgParts[0].toLowerCase();
			if (channel.length() > 10) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText + " channel length can't exceed 10 ("
						+ channel + ")");
				return;
			} else if (channel.length() < 3) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText
						+ " channel length must at least be 3 (" + channel + ")");
				return;
			} else if (!player.hasAttribute("gi." + channel) || !(boolean) player.getAttribute("gi." + channel)) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText + " you are not in this channel ("
						+ channel + ")\nTo join type /gi join " + channel);
				return;
			}
			if (override) {
				player.setAttribute("gilastch", channel);
			}
			chatMessage = msgParts[1];
		} else if (noColorText.startsWith("#")) {
			channel = defaultChannel;
			if (player.hasAttribute("gi." + channel) && !(boolean) player.getAttribute("gi." + channel)) {
				player.sendTextMessage(colorError + pluginName + ":>" + colorText + " you are not in this channel ("
						+ channel + ")\nTo join type /gi join " + channel);
				return;
			}
			if (override) {
				player.setAttribute("gilastch", channel);
			}

			chatMessage = noColorText.substring(1);
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
				// log("sending..."+msg,0);
				ws.sendMessage(msg);
			} else {
				player.sendTextMessage(
						colorError + pluginName + ":>" + colorText + " WebSocket not connected, can't send message.");
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
			log(e.getMessage(), 999);
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
			settings.load(in);
			in.close();
			// fill global values
			logLevel = Integer.parseInt(settings.getProperty("logLevel"));
			webSocketURI = new URI(settings.getProperty("webSocketURI"));
			defaultChannel = settings.getProperty("defaultChannel");
			overrideDefault = settings.getProperty("overrideDefault").contentEquals("true");
			colorOther = settings.getProperty("colorOther");
			colorSelf = settings.getProperty("colorSelf");
			colorLocal = settings.getProperty("colorLocal");

			// motd settings
			sendMOTD = settings.getProperty("sendMOTD").contentEquals("true");
			motd = settings.getProperty("motd");

			// restart settings
			restartOnUpdate = settings.getProperty("restartOnUpdate").contentEquals("true");
			log("OmegaZirkel GlobalIntercom Plugin is enabled", 10);

		} catch (IOException ex) {
			log("IOException on initSettings: " + ex.getMessage(), 100);
			// e.printStackTrace();
		} catch (NumberFormatException ex) {
			log("NumberFormatException on initSettings: " + ex.getMessage(), 100);
		} catch (URISyntaxException ex) {
			log("Exception on initSettings: " + ex.getMessage(), 100);
		}
	}

	/**
	 *
	 * @param text
	 * @param level
	 */
	public static void log(String text, int level) {
		if (level >= logLevel) {
			System.out.println("[OZ.GI] " + text);
		}
	}

	/**
	 *
	 * @param cmsg
	 */
	private void broadcastMessage(ChatMessage cmsg) {
		getServer().getAllPlayers().forEach((player) -> {
			String chKey = "gi." + cmsg.chatChannel;
			String color = colorOther;
			// log(player.getUID() +"=="+ cmsg.playerUID,0);
			if ((player.getUID() + "").contentEquals(cmsg.playerUID)) {
				color = colorSelf;
			}
			boolean isInChannel = player.hasAttribute(chKey) && (boolean) player.getAttribute(chKey);
			boolean isGlobalDefault = !player.hasAttribute(chKey) && cmsg.chatChannel.contentEquals(defaultChannel);
			if (isInChannel || isGlobalDefault) {
				player.sendTextMessage(color + "[" + cmsg.chatChannel.toUpperCase() + "] " + cmsg.playerName + ": "
						+ colorText + cmsg.chatContent);
			}
		});
	}

	@Override
	public void handleMessage(String message) {
		// log(message, 0);
		WSMessage wsm = new Gson().fromJson(message, WSMessage.class);
		if (wsm.event.contentEquals("broadcastMessage")) {
			Type type = new TypeToken<WSMessage<ChatMessage>>() {
			}.getType();
			// System.out.println(type);
			WSMessage<ChatMessage> wscmsg = new Gson().fromJson(message, type);
			// System.out.println(wscmsg.toString());
			ChatMessage cmsg = wscmsg.payload;
			log("New BC Message <" + cmsg.chatContent + "> from " + cmsg.playerName, 0);
			this.broadcastMessage(cmsg);
		}
	}
}
