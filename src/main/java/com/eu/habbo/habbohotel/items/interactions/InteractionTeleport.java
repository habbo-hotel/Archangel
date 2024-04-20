package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomHabbo;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.threading.runnables.RoomUnitWalkToLocation;
import com.eu.habbo.threading.runnables.teleport.TeleportActionOne;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InteractionTeleport extends RoomItem {
    private int targetId;
    private int targetRoomId;
    private int roomUnitID = -1;
    private boolean walkable;

    public InteractionTeleport(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        walkable = baseItem.allowWalk();
        this.setExtraData("0");
    }

    public InteractionTeleport(int id, HabboInfo ownerInfo, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, ownerInfo, item, extradata, limitedStack, limitedSells);
        walkable = item.allowWalk();
        this.setExtraData("0");
    }

    @Override
    public void serializeExtradata(ServerMessage serverMessage) {
        serverMessage.appendInt((this.isLimited() ? 256 : 0));
        serverMessage.appendString(this.getExtraData());

        super.serializeExtradata(serverMessage);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return this.getBaseItem().allowWalk() || roomUnit.getVirtualId() == this.roomUnitID;
    }

    @Override
    public boolean isWalkable() {
        return walkable;
    }

    private void tryTeleport(GameClient client, Room room) {
        /*
            if user is on item, startTeleport
	        else if user is on infront, set state 1 and walk on item
	        else move to infront and interact
         */

        Habbo habbo = client.getHabbo();

        if (habbo == null) {
            return;
        }

        RoomHabbo roomHabbo = habbo.getRoomUnit();

        if (roomHabbo == null) {
            return;
        }

        RoomTile currentItemLocation = room.getLayout().getTile(this.getCurrentPosition().getX(), this.getCurrentPosition().getY());

        if (currentItemLocation == null) {
            return;
        }

        RoomTile inFrontTile = room.getLayout().getTileInFront(currentItemLocation, this.getRotation());

        if (!canUseTeleport(client, room)) {
            return;
        }

        if (this.roomUnitID == roomHabbo.getVirtualId() && roomHabbo.getCurrentPosition().equals(currentItemLocation)) {
            this.startTeleport(room, habbo);
            this.walkable = true;

            try {
                super.onClick(client, room, new Object[]{"TOGGLE_OVERRIDE"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (roomHabbo.getCurrentPosition().equals(currentItemLocation) || roomHabbo.getCurrentPosition().equals(inFrontTile)) {
            // set state 1 and walk on item
            this.roomUnitID = roomHabbo.getVirtualId();
            this.setExtraData("1");
            room.updateItemState(this);
            roomHabbo.walkTo(inFrontTile);

            List<Runnable> onSuccess = new ArrayList<>();
            List<Runnable> onFail = new ArrayList<>();

            onSuccess.add(() -> {
                room.updateTile(currentItemLocation);
                this.tryTeleport(client, room);
                roomHabbo.removeOverrideTile(currentItemLocation);
                roomHabbo.setCanLeaveRoomByDoor(true);
                this.walkable = this.getBaseItem().allowWalk();
            });

            onFail.add(() -> {
                this.walkable = this.getBaseItem().allowWalk();
                room.updateTile(currentItemLocation);
                this.setExtraData("0");
                room.updateItemState(this);
                this.roomUnitID = -1;
                roomHabbo.removeOverrideTile(currentItemLocation);
                roomHabbo.setCanLeaveRoomByDoor(true);
            });

            this.walkable = true;

            room.updateTile(currentItemLocation);
            roomHabbo.addOverrideTile(currentItemLocation);
            roomHabbo.walkTo(currentItemLocation);
            roomHabbo.setCanLeaveRoomByDoor(false);

            Emulator.getThreading().run(new RoomUnitWalkToLocation(roomHabbo, currentItemLocation, room, onSuccess, onFail));
        } else {
            // walk to teleport and interact
            List<Runnable> onSuccess = new ArrayList<>();
            List<Runnable> onFail = new ArrayList<>();

            onSuccess.add(() -> tryTeleport(client, room));

            roomHabbo.walkTo(inFrontTile);
            Emulator.getThreading().run(new RoomUnitWalkToLocation(roomHabbo, inFrontTile, room, onSuccess, onFail));
        }
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) {
        if (room != null && client != null && objects != null && objects.length <= 1) {
            tryTeleport(client, room);
        }
    }

    @Override
    public void onWalk(RoomUnit roomUnit, Room room, Object[] objects) {
    }

    @Override
    public void run() {
        if (!this.getExtraData().equals("0")) {
            this.setExtraData("0");

            Room room = Emulator.getGameEnvironment().getRoomManager().getActiveRoomById(this.getRoomId());
            if (room != null) {
                room.updateItem(this);
            }
        }
        super.run();
    }

    @Override
    public void onPickUp(Room room) {
        this.targetId = 0;
        this.targetRoomId = 0;
        this.roomUnitID = -1;
        this.setExtraData("0");
    }

    public int getTargetId() {
        return this.targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public int getTargetRoomId() {
        return this.targetRoomId;
    }

    public void setTargetRoomId(int targetRoomId) {
        this.targetRoomId = targetRoomId;
    }

    @Override
    public boolean allowWiredResetState() {
        return false;
    }

    public boolean canUseTeleport(GameClient client, Room room) {

        Habbo habbo = client.getHabbo();

        if (habbo == null) {
            return false;
        }

        RoomHabbo roomHabbo = habbo.getRoomUnit();

        if (roomHabbo == null) {
            return false;
        }

        return !roomHabbo.isRiding();
    }

    public void startTeleport(Room room, Habbo habbo) {
        if (habbo.getRoomUnit().isTeleporting()) {
            this.walkable = this.getBaseItem().allowWalk();
            return;
        }

        this.roomUnitID = -1;
        habbo.getRoomUnit().setTeleporting(true);
        Emulator.getThreading().run(new TeleportActionOne(this, room, habbo.getClient()), 0);
    }

    @Override
    public boolean isUsable() {
        return true;
    }
}
