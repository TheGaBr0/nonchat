package com.nonxedy.nonchat.api;

import com.nonxedy.nonchat.util.core.broadcast.BroadcastMessage;
import org.bukkit.entity.Player;

public interface IMessageHandler {
    void handleChat(Player player, String message);
    void handlePrivateMessage(Player sender, Player receiver, String message);
    void handleBroadcast(BroadcastMessage message);
    void handleStaffChat(Player sender, String message);
}
