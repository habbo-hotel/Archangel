package com.eu.habbo.threading.runnables.teleport;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleport;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleportTile;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.constants.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.entities.RoomRotation;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class TeleportActionThree implements Runnable {
    private final RoomItem currentTeleport;
    private final Room room;
    private final GameClient client;

    @Override
    public void run() {
        if (this.client.getHabbo().getRoomUnit().getRoom() != this.room)
            return;

        RoomItem targetTeleport;
        Room targetRoom = this.room;

        if (this.currentTeleport.getRoomId() != ((InteractionTeleport) this.currentTeleport).getTargetRoomId()) {
            targetRoom = Emulator.getGameEnvironment().getRoomManager().getRoom(((InteractionTeleport) this.currentTeleport).getTargetRoomId());
        }

        if (targetRoom == null) {
            Emulator.getThreading().run(new TeleportActionFive(this.currentTeleport, this.room, this.client), 0);
            return;
        }

        if (targetRoom.isPreLoaded()) {
            targetRoom.loadData();
        }

        int id = ((InteractionTeleport) this.currentTeleport).getTargetId();
        targetTeleport = targetRoom.getRoomItemManager().getRoomItemById(id);

        if (targetTeleport == null) {
            Emulator.getThreading().run(new TeleportActionFive(this.currentTeleport, this.room, this.client), 0);
            return;
        }

        RoomTile teleportLocation = targetRoom.getLayout().getTile(targetTeleport.getCurrentPosition().getX(), targetTeleport.getCurrentPosition().getY());

        if (teleportLocation == null) {
            Emulator.getThreading().run(new TeleportActionFive(this.currentTeleport, this.room, this.client), 0);
            return;
        }
        this.client.getHabbo().getRoomUnit().setLocation(teleportLocation);
        this.client.getHabbo().getRoomUnit().getPath().clear();
        this.client.getHabbo().getRoomUnit().removeStatus(RoomUnitStatus.MOVE);
        this.client.getHabbo().getRoomUnit().setCurrentZ(teleportLocation.getStackHeight());

        if (targetRoom != this.room) {
            this.room.getRoomUnitManager().removeHabbo(this.client.getHabbo(), false);
            Emulator.getGameEnvironment().getRoomManager().enterRoom(this.client.getHabbo(), targetRoom.getRoomInfo().getId(), "", Emulator.getConfig().getBoolean("hotel.teleport.locked.allowed"), teleportLocation);
        }

        this.client.getHabbo().getRoomUnit().setRotation(RoomRotation.values()[targetTeleport.getRotation() % 8]);

        targetTeleport.setExtraData("2");
        targetRoom.updateItem(targetTeleport);
        this.client.getHabbo().getRoomUnit().setRoom(targetRoom);
        Emulator.getThreading().run(new TeleportActionFour(targetTeleport, targetRoom, this.client), 0);

    }
}
