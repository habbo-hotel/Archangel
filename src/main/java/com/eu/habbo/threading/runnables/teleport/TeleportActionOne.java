package com.eu.habbo.threading.runnables.teleport;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.interactions.InteractionTeleportTile;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.constants.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.entities.RoomRotation;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.messages.outgoing.rooms.users.UserUpdateComposer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TeleportActionOne implements Runnable {
    private final RoomItem currentTeleport;
    private final Room room;
    private final GameClient client;

    @Override
    public void run() {
        if (this.client.getHabbo().getRoomUnit().getRoom() != this.room)
            return;

        if (this.client.getHabbo().getRoomUnit().getCurrentPosition() != this.room.getLayout().getTile(this.currentTeleport.getCurrentPosition().getX(), this.currentTeleport.getCurrentPosition().getY())) {
            this.client.getHabbo().getRoomUnit().setLocation(this.room.getLayout().getTile(this.currentTeleport.getCurrentPosition().getX(), this.currentTeleport.getCurrentPosition().getY()));
            this.client.getHabbo().getRoomUnit().setRotation(RoomRotation.values()[(this.currentTeleport.getRotation() + 4) % 8]);
            this.client.getHabbo().getRoomUnit().addStatus(RoomUnitStatus.MOVE, this.currentTeleport.getCurrentPosition().getX() + "," + this.currentTeleport.getCurrentPosition().getY() + "," + this.currentTeleport.getCurrentZ());
            this.room.scheduledComposers.add(new UserUpdateComposer(this.client.getHabbo().getRoomUnit()).compose());
            this.client.getHabbo().getRoomUnit().setLocation(this.room.getLayout().getTile(this.currentTeleport.getCurrentPosition().getX(), this.currentTeleport.getCurrentPosition().getY()));
        }

        Emulator.getThreading().run(new TeleportActionTwo(this.currentTeleport, this.room, this.client));
    }
}
