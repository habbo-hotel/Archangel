package com.eu.habbo.plugin.events.furniture;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;

public class FurniturePickedUpEvent extends FurnitureUserEvent {

    public FurniturePickedUpEvent(RoomItem furniture, Habbo habbo) {
        super(furniture, habbo);
    }
}
