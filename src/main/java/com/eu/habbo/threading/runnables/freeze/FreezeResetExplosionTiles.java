package com.eu.habbo.threading.runnables.freeze;

import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeTile;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;
import gnu.trove.set.hash.THashSet;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class FreezeResetExplosionTiles implements Runnable {
    private final THashSet<InteractionFreezeTile> tiles;
    private final Room room;


    @Override
    public void run() {
        for (RoomItem item : this.tiles) {
            item.setExtradata("0");
            this.room.updateItem(item);
        }
    }
}
