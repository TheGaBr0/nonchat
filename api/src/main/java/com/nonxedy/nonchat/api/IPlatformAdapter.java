package com.nonxedy.nonchat.api;

import org.bukkit.plugin.Plugin;

public interface IPlatformAdapter {
    String getSupportedVersion();

    void registerChatListener(Plugin plugin, IMessageHandler handler);

    default boolean supports(String bukkitVersion) {
        if (bukkitVersion == null) return false;

        // "1.21.11-R0.1-SNAPSHOT" → "1.21.11"
        String serverVer = bukkitVersion.split("-")[0];
        String supported  = getSupportedVersion();

        return serverVer.equals(supported)
            || serverVer.startsWith(supported + ".");
    }

    default void cleanup() {
    }
}
