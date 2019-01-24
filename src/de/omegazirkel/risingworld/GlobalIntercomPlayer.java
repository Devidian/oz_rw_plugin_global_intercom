package de.omegazirkel.risingworld;

import java.util.ArrayList;

public class GlobalIntercomPlayer {
    public String _id; // The playerUID
    public String id64; // The playerUID - just in case _id generation might change somehow in the
                        // future
    public String name; // The players name
    public boolean saveSettings; // if set true, all settings (channels) will be saved permanent on the Relay
                                 // Server
    public ArrayList<String> channels;
    public boolean online; // Only needed on Relay Server, has currently no effect ingame
    public boolean override;


    /**
     *
     * @param chName
     * @return
     */
    public boolean isInChannel(String chName){
        return this.channels.contains(chName);
    }
}