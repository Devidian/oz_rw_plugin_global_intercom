package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerJoinChannelMessage extends PlayerMessage {
    public String channel;
    public String password;

    PlayerJoinChannelMessage(Player player) {
        super(player);
    }
}