package com.eu.habbo.messages.outgoing.guild;

import com.eu.habbo.roleplay.guilds.Guild;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GuildEditInfoMessageComposer extends MessageComposer {
    private final Guild guild;

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.guildEditInfoMessageComposer);
        this.response.appendInt(1);
        this.response.appendString(this.guild.getType().getType());
        this.response.appendInt(guild.getRoomId());
        this.response.appendString(guild.getRoomName());
        this.response.appendBoolean(false);
        this.response.appendBoolean(true);
        this.response.appendInt(this.guild.getId());
        this.response.appendString(this.guild.getName());
        this.response.appendString(this.guild.getDescription());
        this.response.appendInt(this.guild.getRoomId());
        this.response.appendInt(this.guild.getColorOne());
        this.response.appendInt(this.guild.getColorTwo());
        this.response.appendInt(this.guild.getState().getState());
        this.response.appendInt(this.guild.isRights() ? 0 : 1);
        this.response.appendBoolean(false);
        this.response.appendString("");
        this.response.appendInt(5);
        String badge = this.guild.getBadge();
        badge = badge.replace("b", "");
        String[] data = badge.split("s");
        int req = 5 - data.length;
        int i = 0;

        for (String s : data) {
            this.response.appendInt((s.length() >= 6 ? Integer.parseInt(s.substring(0, 3)) : Integer.parseInt(s.substring(0, 2))));
            this.response.appendInt((s.length() >= 6 ? Integer.parseInt(s.substring(3, 5)) : Integer.parseInt(s.substring(2, 4))));

            if (s.length() < 5)
                this.response.appendInt(0);
            else if (s.length() >= 6)
                this.response.appendInt(Integer.parseInt(s.substring(5, 6)));
            else
                this.response.appendInt(Integer.parseInt(s.substring(4, 5)));
        }

        while (i != req) {
            this.response.appendInt(0);
            this.response.appendInt(0);
            this.response.appendInt(0);
            i++;
        }
        this.response.appendString(this.guild.getBadge());
        this.response.appendInt(this.guild.getMemberCount());
        return this.response;
    }
}
