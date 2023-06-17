package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredConditionNotFurniTypeMatch extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.NOT_STUFF_IS;
    private final THashSet<HabboItem> items = new THashSet<>();

    public WiredConditionNotFurniTypeMatch(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionNotFurniTypeMatch(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if(this.getWiredSettings().getItemIds().isEmpty()) {
            return true;
        }

        if(stuff.length == 0) {
            return true;
        }

        if (stuff[0] instanceof HabboItem triggeringItem) {
            return this.getWiredSettings().getItems(room).stream().noneMatch(item -> item == triggeringItem);
        }

        return false;
    }

    @Override
    public WiredConditionType getType() {
        return WiredConditionType.NOT_STUFF_IS;
    }
}
