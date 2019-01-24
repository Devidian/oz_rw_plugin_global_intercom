package de.omegazirkel.risingworld;

import net.risingworld.api.objects.Player;

public class PlayerRegisterMessage extends PlayerMessage {
    public boolean register;

    PlayerRegisterMessage(Player player) {
        super(player);
        this.register = true;
    }
}