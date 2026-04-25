package com.nonxedy.nonchat.adapter.v1_16_R3;

import com.nonxedy.nonchat.api.IPlatformAdapter;
import org.bukkit.entity.Player;

public class PlatformAdapter116 implements IPlatformAdapter {

    @Override
    public String getSupportedVersion() {
        return "1.16";
    }

    @Override
    public void registerChatListener(Plugin plugin, ChatManager manager, ChatService service) {
        plugin.getServer().getPluginManager().registerEvents(
            new ChatListener116(manager, service), plugin
        );
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public boolean supportsTextDisplay() { return false; }
}