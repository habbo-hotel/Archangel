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
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class WiredTriggerBotReachedFurni extends InteractionWiredTrigger {
    

    public final static WiredTriggerType type = WiredTriggerType.WALKS_ON_FURNI;

    private final THashSet<HabboItem> items;
    private String botName = "";

    public WiredTriggerBotReachedFurni(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerBotReachedFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public boolean saveData() {
        this.botName = this.getWiredSettings().getStringParam();

        this.items.clear();

        int count = this.getWiredSettings().getItems().length;

        for (int i = 0; i < count; i++) {
            this.items.add(Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(this.getWiredSettings().getItems()[i]));
        }

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (stuff.length >= 1) {
            if (stuff[0] instanceof HabboItem) {
                return this.items.contains(stuff[0]) && room.getBots(this.botName).stream().anyMatch(bot -> bot.getRoomUnit() == roomUnit);
            }
        }
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.botName,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredSettings(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.botName = data.botName;
            for (Integer id: data.itemIds) {
                HabboItem item = room.getHabboItem(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        } else {
            String[] data = wiredData.split(":");

            if (data.length == 1) {
                this.botName = data[0];
            } else if (data.length == 2) {
                this.botName = data[0];

                String[] items = data[1].split(";");

                for (String id : items) {
                    try {
                        HabboItem item = room.getHabboItem(Integer.parseInt(id));

                        if (item != null)
                            this.items.add(item);
                    } catch (Exception e) {
                        log.error("Caught exception", e);
                    }
                }
            }
        }
    }

    static class JsonData {
        String botName;
        List<Integer> itemIds;

        public JsonData(String botName, List<Integer> itemIds) {
            this.botName = botName;
            this.itemIds = itemIds;
        }
    }
}
