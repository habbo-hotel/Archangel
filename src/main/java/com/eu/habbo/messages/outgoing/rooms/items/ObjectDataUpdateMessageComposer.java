package com.eu.habbo.messages.outgoing.rooms.items;

import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectDataUpdateMessageComposer extends MessageComposer {
    private final RoomItem item;

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.objectDataUpdateMessageComposer);
        this.response.appendString(this.item.getId() + "");
        this.item.serializeExtradata(this.response);
        return this.response;
    }
}