package com.nonxedy.nonchat.adapter.v1_21_R1;

import com.nonxedy.nonchat.api.IMessageHandler;
import com.nonxedy.nonchat.api.ServiceAdapter;
import org.bukkit.event.Listener;

public final class PlatformAdapter121 extends ServiceAdapter {
    public PlatformAdapter121() {
        super("1.21");
    }

    @Override
    protected Listener createChatListener(IMessageHandler handler) {
        return new ChatListener121(handler);
    }
}
