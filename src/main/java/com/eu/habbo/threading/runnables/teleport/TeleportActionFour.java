package com.eu.habbo.threading.runnables.teleport;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class TeleportActionFour implements Runnable {
    private final RoomItem currentTeleport;
    private final Room room;
    private final GameClient client;


    @Override
    public void run() {
        if (this.client.getHabbo().getRoomUnit().getRoom() != this.room) {
            this.client.getHabbo().getRoomUnit().setCanWalk(true);
            this.currentTeleport.setExtraData("0");
            this.room.updateItem(this.currentTeleport);
            return;
        }

        if(this.client.getHabbo().getRoomUnit() != null) {
            this.client.getHabbo().getRoomUnit().setLeavingTeleporter(true);
        }

        Emulator.getThreading().run(new TeleportActionFive(this.currentTeleport, this.room, this.client), 0);
    }
}
