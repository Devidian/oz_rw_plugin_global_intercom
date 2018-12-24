/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import com.mongodb.ConnectionString;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.SECONDS;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChatEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author Maik Laschober
 */
public class GlobalIntercom extends Plugin implements Listener {

    // Settings
    static int logLevel = 0;
    static boolean restartOnUpdate = true;
    static boolean overrideDefault = true;
    static ConnectionString mongoConnectionString;
    static String defaultChannel = "global";
    static boolean sendMOTD = false;
    static String motd = "This Server uses [#F00000]Global Intercom[#FFFFFF] Plugin. Type [#997d4a]/gi info[#FFFFFF] for more info";

    static String colorOther = "[#3881f7]";
    static String colorSelf = "[#37f7da]";
    static String colorLocal = "[#FFFFFF]";
    // END Settings

    static String colorError = "[#FF0000]";
    static String colorWarning = "[#808000]";
    static String colorOkay = "[#00FF00]";
    static String colorText = "[#EEEEEE]";

    static int chatVersion = 1;
    static boolean mongoStatus = false;
    static MongoClient mongoClient;
    static MongoDatabase mongoDB;
    static MongoCollection<Document> chatCollection;
    static ChangeStreamPublisher<Document> giPublisher;
    static Subscriber<ChangeStreamDocument<Document>> giSubscriber;

    @Override
    public void onEnable() {
        registerEventListener(this);
        this.initSettings();
        this.initMongoDB();
    }

    @Override
    public void onDisable() {

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
                player.sendTextMessage(colorError + "GlobalIntercom:>[#FFFFFF] Invalid call " + command + "\nUsage: /gi [join|leave] [channelname]\nchannelname must be at least 3 but max 10 letters");
                return;
            }
            switch (option) {
                case "join":
                    player.setAttribute("gi." + channel, true);
                    player.sendTextMessage("[#00FF00]GlobalIntercom:>[#FFFFFF] You have joined " + channel);
                    break;
                case "leave":
                    player.setAttribute("gi." + channel, false);
                    player.sendTextMessage("[#808000]GlobalIntercom:>[#FFFFFF] You have left " + channel);
                    break;
                case "help":
                case "info":
                    player.sendTextMessage(colorOkay + "GlobalIntercom:> USAGE\n"
                            + colorText + "Join or leave channel[#997d4a]/gi [join|leave] [channelname] \n"
                            + colorText + "Write <HelloWorld> to default channel: [#997d4a]#HelloWorld\n"
                            + colorText + "Write <HelloWorld> to other channel: [#997d4a]##other HelloWorld\n"
                            + colorText + "Reset default channel to local: [#997d4a]#%text\n"
                            + colorText + "Change chat-override: [#997d4a]/gi override [true|false]\n"
                            + colorText + "Other commands: [#997d4a]/gi [help|info|status]");
                    break;
                case "status":
                    String lastCH = "lokal";
                    if (player.hasAttribute("gilastch")) {
                        lastCH = (String) player.getAttribute("gilastch");
                    }
                    String dbStatus = colorError + "not connected";
                    if (mongoStatus) {
                        dbStatus = colorOkay + "connected";
                    }

                    player.sendTextMessage(colorOkay + "GlobalIntercom:> STATUS\n[#FFFFFF]Current default channel: [#997d4a]" + lastCH + "\n[#FFFFFF]Database status: " + dbStatus);
                    break;
                case "override":
                    if (cmd.length > 2) {
                        boolean setOverrideTo = cmd[2].toLowerCase().contentEquals("true");
                        player.setAttribute("giOverride", setOverrideTo);
                        if (setOverrideTo) {
                            player.sendTextMessage(colorOkay + "GlobalIntercom:> " + colorText + "chat-override is now " + colorOkay + "on");
                        } else {
                            player.sendTextMessage(colorOkay + "GlobalIntercom:> " + colorText + "chat-override is now " + colorError + "off");
                        }
                    } else if (player.hasAttribute("giOverride")) {
                        if ((boolean) player.getAttribute("giOverride")) {
                            player.sendTextMessage(colorOkay + "GlobalIntercom:> " + colorText + "chat-override is now " + colorOkay + "on");
                        } else {
                            player.sendTextMessage(colorOkay + "GlobalIntercom:> " + colorText + "chat-override is now " + colorError + "off");
                        }
                    } else {
                        player.sendTextMessage(colorOkay + "GlobalIntercom:> " + colorText + "chat-override is not set turn on or off with /gi override [true|false] ");
                    }
                    break;
                default:
                    player.sendTextMessage(colorError + "GlobalIntercom:> unknown option " + option);
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
                player.sendTextMessage(colorError + "GlobalIntercom:>[#FFFFFF] channel length can't exceed 10 (" + channel + ")");
                return;
            } else if (channel.length() < 3) {
                player.sendTextMessage(colorError + "GlobalIntercom:>[#FFFFFF] channel length must at least be 3 (" + channel + ")");
                return;
            } else if (!player.hasAttribute("gi." + channel) || !(boolean) player.getAttribute("gi." + channel)) {
                player.sendTextMessage(colorError + "GlobalIntercom:>[#FFFFFF] you are not in this channel (" + channel + ")\nTo join type /gi join " + channel);
                return;
            }
            if (override) {
                player.setAttribute("gilastch", channel);
            }
            chatMessage = msgParts[1];
        } else if (noColorText.startsWith("#")) {
            channel = defaultChannel;
            if (player.hasAttribute("gi." + channel) && !(boolean) player.getAttribute("gi." + channel)) {
                player.sendTextMessage(colorError + "GlobalIntercom:>[#FFFFFF] you are not in this channel (" + channel + ")\nTo join type /gi join " + channel);
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

        Document doc = new Document("createdOn", new Date())
                .append("chatVersion", chatVersion)
                .append("chatContent", chatMessage.trim())
                .append("chatChannel", channel)
                .append("playerName", player.getName())
                .append("playerUID", uid)
                .append("sourceName", server.getName())
                .append("sourceIP", server.getIP())
                .append("sourceVersion", server.getVersion());
        try {
            chatCollection.insertOne(doc).subscribe(new Subscriber<Success>() {
                @Override
                public void onSubscribe(final Subscription s) {
                    s.request(1);  // <--- Data requested and the insertion will now occur
                }

                @Override
                public void onNext(final Success success) {
                    System.out.println("Inserted");
                }

                @Override
                public void onError(final Throwable t) {
                    System.out.println("Failed");
                }

                @Override
                public void onComplete() {
                    System.out.println("Completed");
                }
            });
            event.setCancelled(true);
        } catch (Exception ex) {
            log(ex.getMessage(), 999);
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
     * https://github.com/mongodb/mongo-java-driver-reactivestreams/blob/master/examples/documentation/src/ChangeStreamSamples.java
     */
    private void initMongoDB() {
        mongoClient = MongoClients.create(mongoConnectionString);
        mongoDB = mongoClient.getDatabase("risingWorld");
        chatCollection = mongoDB.getCollection("chatIntercom");
        initIntercomWatcher();
    }

    /**
     *
     */
    private void initIntercomWatcher() {

        giPublisher = chatCollection.watch();
        giSubscriber = new Subscriber<ChangeStreamDocument<Document>>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(Integer.MAX_VALUE);
                mongoStatus = true;
            }

            @Override
            public void onNext(final ChangeStreamDocument<Document> csdoc) {
                try {
                    Document doc = csdoc.getFullDocument();
                    int version = doc.getInteger("chatVersion", 0);
                    if (chatVersion != version) {
                        return;
                    }
                    String playerName = doc.getString("playerName");
                    String channel = doc.getString("chatChannel");
                    Long playerUID = doc.getLong("playerUID");
//                    String serverName = doc.getString("sourceName");
                    String chatMessage = doc.getString("chatContent");

                    getServer().getAllPlayers().forEach((player) -> {
                        String chKey = "gi." + channel;
                        String color = colorOther;
                        if (player.getUID() == playerUID) {
                            color = colorSelf;
                        }
                        boolean isInChannel = player.hasAttribute(chKey) && (boolean) player.getAttribute(chKey);
                        boolean isGlobalDefault = !player.hasAttribute(chKey) && channel.contentEquals(defaultChannel);
                        if (isInChannel || isGlobalDefault) {
                            player.sendTextMessage(color + "[" + channel.toUpperCase() + "] " + playerName + ": " + colorText + chatMessage);
                        }
                    });
                } catch (Exception ex) {
                    log(ex.getMessage(), 999);
                }
            }

            @Override
            public void onError(final Throwable t) {
                log("Failed", 999);
                mongoStatus = false;
            }

            @Override
            public void onComplete() {
                log("Completed", 0);
                mongoStatus = false;
            }
        };
        giPublisher.subscribe(giSubscriber);
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
            mongoConnectionString = new ConnectionString(settings.getProperty("mongoURL"));
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
            this.log("OmegaZirkel GlobalIntercom Plugin is enabled", 10);

        } catch (IOException ex) {
            this.log("IOException on initSettings: " + ex.getMessage(), 100);
//            e.printStackTrace();
        } catch (NumberFormatException ex) {
            this.log("NumberFormatException on initSettings: " + ex.getMessage(), 100);
        } catch (Exception ex) {
            this.log("Exception on initSettings: " + ex.getMessage(), 100);
        }
    }

    /**
     *
     * @param text
     * @param level
     */
    private void log(String text, int level) {
        if (level >= logLevel) {
            System.out.println("[OZGI] " + text);
        }
    }
}
