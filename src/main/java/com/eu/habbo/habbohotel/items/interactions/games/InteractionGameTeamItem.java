package com.eu.habbo.habbohotel.items.interactions.games;

import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.users.HabboInfo;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class InteractionGameTeamItem extends RoomItem {
    public final GameTeamColors teamColor;

    protected InteractionGameTeamItem(ResultSet set, Item baseItem, GameTeamColors teamColor) throws SQLException {
        super(set, baseItem);

        this.teamColor = teamColor;
    }

    protected InteractionGameTeamItem(int id, HabboInfo ownerInfo, Item item, String extradata, int limitedStack, int limitedSells, GameTeamColors teamColor) {
        super(id, ownerInfo, item, extradata, limitedStack, limitedSells);

        this.teamColor = teamColor;
    }
}
