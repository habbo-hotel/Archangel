package com.eu.habbo.roleplay.messages.incoming.gang;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.roleplay.commands.gang.GangLeaveCommand;

public class GangLeaveEvent  extends MessageHandler {
    @Override
    public void handle() {
        new GangLeaveCommand().handle(this.client, new String[] {,});
    }
}