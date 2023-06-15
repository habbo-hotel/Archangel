package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionNotHabboHasEffect extends InteractionWiredCondition {
    private static final WiredConditionType type = WiredConditionType.NOT_ACTOR_WEARS_EFFECT;

    protected int effectId;

    public WiredConditionNotHabboHasEffect(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotHabboHasEffect(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null) return false;
        return roomUnit.getEffectId() != this.effectId;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.effectId
        ));
    }

    @Override
    public void loadWiredSettings(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.effectId = data.effectId;
        } else {
            this.effectId = Integer.parseInt(wiredData);
        }
    }

    @Override
    public WiredConditionType getType() {
        return type;
    }

    @Override
    public boolean saveData() {
        if(this.getWiredSettings().getIntegerParams().length < 1) return false;
        this.effectId = this.getWiredSettings().getIntegerParams()[0];

        return true;
    }

    static class JsonData {
        int effectId;

        public JsonData(int effectId) {
            this.effectId = effectId;
        }
    }
}
