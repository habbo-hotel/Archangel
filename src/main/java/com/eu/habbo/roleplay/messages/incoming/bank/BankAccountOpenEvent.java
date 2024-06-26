package com.eu.habbo.roleplay.messages.incoming.bank;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.roleplay.commands.bank.BankAccountOpenCommand;

public class BankAccountOpenEvent extends MessageHandler {
    @Override
    public void handle() {
        String corpID = String.valueOf(this.packet.readInt());
        String username = this.packet.readString();;

        if (corpID == null || username == null) {
            return;
        }

        new BankAccountOpenCommand().handle(this.client, new String[] {null, corpID, username});
    }
}