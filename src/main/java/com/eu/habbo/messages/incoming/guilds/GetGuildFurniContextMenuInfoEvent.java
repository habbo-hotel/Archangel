package com.eu.habbo.messages.incoming.guilds;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.guilds.GuildFurniContextMenuInfoMessageComposer;

public class GetGuildFurniContextMenuInfoEvent extends MessageHandler {
    @Override
    public void handle() {
        int itemId = this.packet.readInt();
        int guildId = this.packet.readInt();

        if (this.client.getHabbo().getRoomUnit().getRoom() != null) {
            RoomItem item = this.client.getHabbo().getRoomUnit().getRoom().getHabboItem(itemId);
            Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

            if (item != null && guild != null)
                this.client.sendResponse(new GuildFurniContextMenuInfoMessageComposer(this.client.getHabbo(), guild, item));
        }
    }
}
