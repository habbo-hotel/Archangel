package com.eu.habbo.roleplay.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserRoleplayStatsChangeComposer extends MessageComposer {
    private final Habbo habbo;

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.userRoleplayStatsChangeComposer);
        this.response.appendInt(this.habbo.getHabboInfo().getId());
        this.response.appendString(this.habbo.getHabboInfo().getUsername());
        this.response.appendString(this.habbo.getHabboInfo().getLook());
        this.response.appendInt(this.habbo.getHabboInfo().getCredits());
        this.response.appendInt(0); // TODO: Bank
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getHealthNow());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getHealthMax());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getEnergyNow());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getEnergyMax());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getHungerNow());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getHungerMax());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getCorporation().getId());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getCorporationPosition().getId());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getGang().getId());
        this.response.appendInt(this.habbo.getHabboRoleplayStats().getGangPosition().getId());
        return this.response;
    }
}
