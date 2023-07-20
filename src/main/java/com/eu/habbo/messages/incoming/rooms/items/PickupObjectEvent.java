package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.habbohotel.items.interactions.InteractionPostIt;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;
import com.eu.habbo.messages.incoming.MessageHandler;

public class PickupObjectEvent extends MessageHandler {
    @Override
    public void handle() {
        int category = this.packet.readInt(); //10 = floorItem and 20 = wallItem
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getRoomUnit().getRoom();

        if (room == null)
            return;

        RoomItem item = room.getHabboItem(itemId);

        if (item == null)
            return;

        if (item instanceof InteractionPostIt)
            return;

        if (item.getUserId() == this.client.getHabbo().getHabboInfo().getId()) {
            room.getRoomItemManager().pickUpItem(item, this.client.getHabbo());
        } else {
            if (room.hasRights(this.client.getHabbo())) {
                if (this.client.getHabbo().hasRight(Permission.ACC_ANYROOMOWNER)) {
                    item.setUserId(this.client.getHabbo().getHabboInfo().getId());
                } else {
                    if (this.client.getHabbo().getHabboInfo().getId() != room.getRoomInfo().getOwnerInfo().getId()) {
                        if (item.getUserId() == room.getRoomInfo().getOwnerInfo().getId()) {
                            return;
                        }
                    }
                }

                room.ejectUserItem(item);
            }
        }
    }
}
