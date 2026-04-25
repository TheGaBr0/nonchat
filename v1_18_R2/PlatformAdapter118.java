package com.nonxedy.nonchat.adapter.v1_18_R2;

import com.nonxedy.nonchat.api.IPlatformAdapter;
import org.bukkit.entity.Player;

public class PlatformAdapter118 implements IPlatformAdapter {

    @Override
    public String getSupportedVersion() {
        return "1.18";
    }

    @Override
    public void registerChatListener(Plugin plugin, ChatManager manager, ChatService service) {
        plugin.getServer().getPluginManager().registerEvents(
            new ChatListener118(manager, service), plugin
        );
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public boolean supportsTextDisplay() { return false; }
}