package com.eu.habbo.habbohotel.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class NewUserGift implements ISerialize {
    @Getter
    private final int id;
    @Getter
    private final Type type;
    @Getter
    private final String imageUrl;
    @Getter
    private Map<String, String> items = new HashMap<>();

    public NewUserGift(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.type = Type.valueOf(set.getString("type").toUpperCase());
        this.imageUrl = set.getString("image");
        this.items.put(this.type == Type.ROOM ? "" : set.getString("value"), this.type == Type.ROOM ? set.getString("value") : "");
    }

    public NewUserGift(int id, Type type, String imageUrl, Map<String, String> items) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.type = type;
        this.items = items;
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendString(this.imageUrl);
        message.appendInt(this.items.size());
        for (Map.Entry<String, String> entry : this.items.entrySet()) {
            message.appendString(entry.getKey()); //Item Name
            message.appendString(entry.getValue()); //Extra Info
        }
    }

    public void give(Habbo habbo) {
        if (this.type == Type.ITEM) {
            for (Map.Entry<String, String> set : this.items.entrySet()) {
                Item item = Emulator.getGameEnvironment().getItemManager().getItem(set.getKey());

                if (item != null) {
                    RoomItem createdItem = Emulator.getGameEnvironment().getItemManager().createItem(habbo.getHabboInfo().getId(), item, 0, 0, "");

                    if (createdItem != null) {
                        habbo.addFurniture(createdItem);
                    }
                }
            }
        } else if (this.type == Type.ROOM) {
            //TODO Give room
        }
    }

    public enum Type {
        ITEM,
        ROOM
    }
}