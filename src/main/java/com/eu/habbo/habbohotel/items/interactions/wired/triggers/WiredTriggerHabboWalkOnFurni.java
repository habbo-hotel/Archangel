package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredTriggerHabboWalkOnFurni extends InteractionWiredTrigger {
    public static final WiredTriggerType type = WiredTriggerType.WALKS_ON_FURNI;

    private THashSet<HabboItem> items;

    public WiredTriggerHabboWalkOnFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerHabboWalkOnFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (stuff.length >= 1) {
            if (stuff[0] instanceof HabboItem) {
                return this.items.contains(stuff[0]);
            }
        }
        return false;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public boolean saveData() {
        this.items.clear();

        int count = this.getWiredSettings().getItems().length;

        for (int i = 0; i < count; i++) {
            this.items.add(Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(this.getWiredSettings().getItems()[i]));
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredSettings(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            for (Integer id: data.itemIds) {
                HabboItem item = room.getHabboItem(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        } else {
            if (wiredData.split(":").length >= 3) {
//                super.setDelay(Integer.parseInt(wiredData.split(":")[0])); TODO this trigger has delay???

                if (!wiredData.split(":")[2].equals("\t")) {
                    for (String s : wiredData.split(":")[2].split(";")) {
                        if (s.isEmpty())
                            continue;

                        try {
                            HabboItem item = room.getHabboItem(Integer.parseInt(s));

                            if (item != null)
                                this.items.add(item);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        List<Integer> itemIds;

        public JsonData(List<Integer> itemIds) {
            this.itemIds = itemIds;
        }
    }
}
