package com.nonxedy.nonchat.adapter.v1_17_R1;

import com.nonxedy.nonchat.api.IPlatformAdapter;
import org.bukkit.entity.Player;

public class PlatformAdapter117 implements IPlatformAdapter {

    @Override
    public String getSupportedVersion() {
        return "1.17";
    }

    @Override
    public void registerChatListener(Plugin plugin, ChatManager manager, ChatService service) {
        plugin.getServer().getPluginManager().registerEvents(
            new ChatListener117(manager, service), plugin
        );
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public boolean supportsTextDisplay() { return false; }
}