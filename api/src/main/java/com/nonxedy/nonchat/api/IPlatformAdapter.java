package com.nonxedy.nonchat.api;

import org.bukkit.plugin.Plugin;

public interface IPlatformAdapter {
    String getSupportedVersion();

    void registerChatListener(Plugin plugin, IMessageHandler handler);

    default boolean supports(String bukkitVersion) {
        return bukkitVersion != null && bukkitVersion.startsWith(getSupportedVersion());
    }

    default void cleanup() {
    }
}
