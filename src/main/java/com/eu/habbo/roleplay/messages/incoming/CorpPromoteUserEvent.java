package com.eu.habbo.roleplay.messages.incoming;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.roleplay.commands.corporation.CorpPromoteCommand;

public class CorpPromoteUserEvent extends MessageHandler {
    @Override
    public void handle() {
        String targetedUsername = this.packet.readString();

        if (targetedUsername == null) {
            return;
        }

        new CorpPromoteCommand().handle(this.client, new String[] {null, targetedUsername});
    }
}