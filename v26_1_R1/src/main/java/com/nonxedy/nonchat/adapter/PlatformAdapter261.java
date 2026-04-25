package com.nonxedy.nonchat.adapter.v26_1_R1;

import com.nonxedy.nonchat.api.IPlatformAdapter;
import java.util.Optional;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

public class PlatformAdapter261 implements IPlatformAdapter {

    @Override
    public String getSupportedVersion() {
        return "26.1";
    }

    @Override
    public void registerChatListener(Plugin plugin, ChatManager manager, ChatService service) {
        plugin.getServer().getPluginManager().registerEvents(
            new ChatListener261(manager, service), plugin
        );
    }

    @Override
    public Optional<Object> createChatBubble(Player player, Component message) {
        TextDisplay display = player.getWorld()
            .spawn(player.getLocation().add(0, 2.2, 0), TextDisplay.class);
        display.text(message);
        display.setBillboard(Display.Billboard.CENTER);
        return Optional.of(display);
    }

    @Override
    public void removeChatBubble(Object entity) {
        if (entity instanceof TextDisplay td) td.remove();
    }

    @Override
    public int getPing(Player player) {
        return player.getPing();
    }

    @Override
    public boolean supportsTextDisplay() { return true; }
}