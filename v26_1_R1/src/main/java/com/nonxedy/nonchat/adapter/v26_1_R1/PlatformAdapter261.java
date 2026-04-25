package com.nonxedy.nonchat.adapter.v26_1_R1;

import com.nonxedy.nonchat.api.IMessageHandler;
import com.nonxedy.nonchat.api.ServiceAdapter;
import org.bukkit.event.Listener;

public final class PlatformAdapter261 extends ServiceAdapter {
    public PlatformAdapter261() {
        super("26.1");
    }

    @Override
    protected Listener createChatListener(IMessageHandler handler) {
        return new ChatListener261(handler);
    }
}
