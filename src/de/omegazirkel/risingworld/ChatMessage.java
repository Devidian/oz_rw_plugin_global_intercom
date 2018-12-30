/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import java.util.Date;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/**
 *
 * @author Maik
 */
public class ChatMessage {

    public static int version = 1;

    // Object properties
    public Date createdOn;
    public int chatVersion;
    public String chatContent;
    public String chatChannel;
    public String playerName;
    public String playerUID;    // should be long but JavaScript cant handle this (yet)
    public String sourceName;
    public String sourceIP;
    public String sourceVersion;

    ChatMessage(Player player, Server server, String msg, String ch) {
        this.createdOn = new Date();
        // Message stuff
        this.chatVersion = version;
        this.chatContent = msg.trim();
        this.chatChannel = ch;
        // Player stuff
        this.playerName = player.getName();
        this.playerUID = ""+player.getUID();
        // Server stuff
        this.sourceName = server.getName();
        this.sourceIP = server.getIP();
        this.sourceVersion = server.getVersion();
    }

    @Override
    public String toString() {
        return "ChatMessage\n"
                + "playerName: " + this.playerName + "\n"
                + "chatContent: " + this.chatContent + "\n"
                + "chatChannel: " + this.chatChannel;
    }
}
