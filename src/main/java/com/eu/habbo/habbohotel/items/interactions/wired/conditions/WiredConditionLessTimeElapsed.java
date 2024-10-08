package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.wired.WiredConditionType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionLessTimeElapsed extends InteractionWiredCondition {
    public final int PARAM_CYCLE = 0;

    public WiredConditionLessTimeElapsed(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionLessTimeElapsed(int id, HabboInfo ownerInfo, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, ownerInfo, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        int cycles = this.getWiredSettings().getIntegerParams().get(PARAM_CYCLE);
        return (Emulator.getIntUnixTimestamp() - room.getLastTimerReset()) / 0.5 < cycles;
    }

    @Override
    public void loadDefaultIntegerParams() {
        if(this.getWiredSettings().getIntegerParams().isEmpty()) {
            this.getWiredSettings().getIntegerParams().add(0);
        }
    }

    @Override
    public WiredConditionType getType() {
        return WiredConditionType.TIME_LESS_THAN;
    }
}
