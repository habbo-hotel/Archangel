package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RoomFloorThicknessUpdatedComposer extends MessageComposer {
    private final Room room;

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.roomFloorThicknessUpdatedComposer);
        this.response.appendBoolean(this.room.getRoomInfo().isHideWalls());
        this.response.appendInt(this.room.getRoomInfo().getFloorThickness());
        this.response.appendInt(this.room.getRoomInfo().getWallThickness());
        return this.response;
    }
}