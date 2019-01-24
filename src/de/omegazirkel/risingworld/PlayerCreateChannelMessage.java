package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerCreateChannelMessage extends PlayerMessage {
    public String channel;
    public String password;

    PlayerCreateChannelMessage(Player player) {
        super(player);
    }
}