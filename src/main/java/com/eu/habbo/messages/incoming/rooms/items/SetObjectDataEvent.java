package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionCustomValues;
import com.eu.habbo.habbohotel.items.interactions.InteractionRoomAds;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import gnu.trove.map.hash.THashMap;

public class SetObjectDataEvent extends MessageHandler {
    @Override
    public void handle() {
        Room room = this.client.getHabbo().getRoomUnit().getRoom();
        if (room == null)
            return;

        if (!room.getRoomRightsManager().hasRights(this.client.getHabbo()))
            return;

        int id = this.packet.readInt();
        RoomItem item = room.getRoomItemManager().getRoomItemById(id);
        if (item == null)
            return;

        if (item instanceof InteractionRoomAds && !this.client.getHabbo().hasPermissionRight(Permission.ACC_ADS_BACKGROUND)) {
            this.client.getHabbo().alert(Emulator.getTexts().getValue("hotel.error.roomads.nopermission"));
            return;
        }
        if (item instanceof InteractionCustomValues) {
            THashMap<String, String> oldValues = new THashMap<>(((InteractionCustomValues) item).values);
            int count = this.packet.readInt();
            for (int i = 0; i < count / 2; i++) {
                String key = this.packet.readString();
                String value = this.packet.readString();

                if (!Emulator.getConfig().getBoolean("camera.use.https")) {
                    value = value.replace("https://", "http://");
                }

                ((InteractionCustomValues) item).values.put(key, value);
            }

            item.setExtraData(((InteractionCustomValues) item).toExtraData());
            item.setSqlUpdateNeeded(true);
            Emulator.getThreading().run(item);
            room.updateItem(item);
            ((InteractionCustomValues) item).onCustomValuesSaved(room, this.client, oldValues);
        }
    }
}
