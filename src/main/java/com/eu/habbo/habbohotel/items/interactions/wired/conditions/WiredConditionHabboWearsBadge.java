package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionHabboWearsBadge extends InteractionWiredCondition {
    public WiredConditionHabboWearsBadge(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionHabboWearsBadge(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if(this.getWiredSettings().getStringParam().isEmpty()) {
            return true;
        }

        String badgeCode = this.getWiredSettings().getStringParam();
        Habbo habbo = room.getHabbo(roomUnit);

        if(habbo == null) {
            return false;
        }

        synchronized (habbo.getInventory().getBadgesComponent().getWearingBadges()) {
            for (HabboBadge badge : habbo.getInventory().getBadgesComponent().getWearingBadges()) {
                if (badge.getCode().equalsIgnoreCase(badgeCode)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public WiredConditionType getType() {
        return WiredConditionType.ACTOR_WEARS_BADGE;
    }
}
