package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.users.HabboGender;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItem;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InteractionFXBox extends InteractionDefault {
    public InteractionFXBox(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
     //   this.setExtradata("0");
    }

    public InteractionFXBox(int id, HabboInfo ownerInfo, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, ownerInfo, item, extradata, limitedStack, limitedSells);
       // this.setExtradata("0");
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) {
        if (client != null && this.getOwnerInfo().getId() == client.getHabbo().getHabboInfo().getId()) {
            if (this.getExtraData().equals("1"))
                return;

            int effectId = -1;

            if (client.getHabbo().getHabboInfo().getGender().equals(HabboGender.M)) {
                if (this.getBaseItem().getEffectM() > 0) {
                    effectId = this.getBaseItem().getEffectM();
                }
            }

            if (client.getHabbo().getHabboInfo().getGender().equals(HabboGender.F)) {
                if (this.getBaseItem().getEffectF() > 0) {
                    effectId = this.getBaseItem().getEffectF();
                }
            }

            if (effectId < 0)
                return;

            if (client.getHabbo().getInventory().getEffectsComponent().ownsEffect(effectId))
                return;

            client.getHabbo().getInventory().getEffectsComponent().createEffect(effectId, 0);
            client.getHabbo().getInventory().getEffectsComponent().enableEffect(effectId);

            this.setExtraData("1");
            room.updateItemState(this);
            room.getRoomItemManager().removeRoomItem(this);
            RoomItem item = this;
            Emulator.getThreading().run(() -> {
                new QueryDeleteHabboItem(item.getId()).run();
                room.sendComposer(new RemoveFloorItemComposer(item).compose());
                room.updateTile(room.getLayout().getTile(this.getCurrentPosition().getX(), this.getCurrentPosition().getY()));
            }, 500);
        }
    }
}