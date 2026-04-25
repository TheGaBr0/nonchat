package com.nonxedy.nonchat.api;

import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.nonxedy.nonchat.core.ChatManager;
import com.nonxedy.nonchat.service.ChatService;
import net.kyori.adventure.text.Component;

/**
 * Platform version adapter — every version of MC realize this contract.
 * Registering by ServiceLoader (META-INF/services/…).
 */
public interface IPlatformAdapter {

    /** Version of MC that supports adapter (prefix of Bukkit version) */
    String getSupportedVersion();

    /** Register right chat listener for this version */
    void registerChatListener(
        Plugin plugin,
        ChatManager manager,
        ChatService service
    );

    /** Creates TextDisplay bubble. Empty Optional on 1.16–1.19.3 */
    Optional<Object> createChatBubble(Player player, Component message);

    /** Delete bubble entity */
    void removeChatBubble(Object entity);

    /** Gets ping of player (in ms) */
    int getPing(Player player);

    /** Checks, does adapter supports TextDisplay */
    default boolean supportsTextDisplay() { return false; }
}