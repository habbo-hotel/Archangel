package com.eu.habbo.roleplay.messages.outgoing.bank;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.roleplay.users.HabboBankAccount;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BankAccountInfoComposer extends MessageComposer {
    private final HabboBankAccount habboBankAccount;

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.invoiceReceivedComposer);
        this.response.appendInt(this.habboBankAccount.getId());
        this.response.appendInt(this.habboBankAccount.getCorpID());
        this.response.appendInt(this.habboBankAccount.getUserID());
        this.response.appendInt(this.habboBankAccount.getCreditBalance());
        this.response.appendInt(this.habboBankAccount.getDebitBalance());
        this.response.appendInt(this.habboBankAccount.getCreatedAt());
        this.response.appendInt(this.habboBankAccount.getUpdatedAt());
        return this.response;
    }
}