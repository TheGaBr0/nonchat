package com.nonxedy.nonchat.api;

import org.bukkit.plugin.Plugin;

public interface IPlatformAdapter {
    String getSupportedVersion();

    void registerChatListener(Plugin plugin, IMessageHandler handler);

    default boolean supports(String bukkitVersion) {
        if (bukkitVersion == null) {
            return false;
        }

        String normalizedVersion = bukkitVersion.split("-")[0];
        return normalizedVersion.startsWith(getSupportedVersion());
    }

    default void cleanup() {
    }
}
