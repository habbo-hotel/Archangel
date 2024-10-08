package com.eu.habbo.threading.runnables.hopper;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@AllArgsConstructor
class HopperActionTwo implements Runnable {

    private final RoomItem teleportOne;
    private final Room room;
    private final GameClient client;
    
    @Override
    public void run() {
        this.teleportOne.setExtraData("2");

        int targetRoomId = 0;
        int targetItemId = 0;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT items.id, items.room_id FROM items_hoppers INNER JOIN items ON items_hoppers.item_id = items.id WHERE base_item = ? AND items.id != ? AND room_id > 0 ORDER BY RAND() LIMIT 1")) {
            statement.setInt(1, this.teleportOne.getBaseItem().getId());
            statement.setInt(2, this.teleportOne.getId());

            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    targetItemId = set.getInt("id");
                    targetRoomId = set.getInt("room_id");
                }
            }
        } catch (SQLException e) {
            log.error("Caught SQL exception", e);
        }

        if (targetRoomId != 0 && targetItemId != 0) {
            Emulator.getThreading().run(new HopperActionThree(this.teleportOne, this.room, this.client, targetRoomId, targetItemId), 500);
        } else {
            this.teleportOne.setExtraData("0");
            this.client.getHabbo().getRoomUnit().setCanWalk(true);
            this.client.getHabbo().getRoomUnit().setTeleporting(false);
            Emulator.getThreading().run(new HopperActionFour(this.teleportOne, this.room, this.client), 500);
        }

        this.room.updateItem(this.teleportOne);
    }
}
