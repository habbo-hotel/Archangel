package com.eu.habbo.roleplay.messages.outgoing.game;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TaxiFeeComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.taxFeeComposer);
        this.response.appendInt(Integer.parseInt(Emulator.getConfig().getValue("roleplay.tax.fee", "20")));
        return this.response;
    }
}
