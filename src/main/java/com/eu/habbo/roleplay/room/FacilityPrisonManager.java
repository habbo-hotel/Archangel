package com.eu.habbo.roleplay.room;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.items.interactions.InteractionPrisonBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FacilityPrisonManager {
    
    private static FacilityPrisonManager instance;

    public static FacilityPrisonManager getInstance() {
        if (instance == null) {
            instance = new FacilityPrisonManager();
        }
        return instance;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(FacilityPrisonManager.class);
    private final List<PrisonSentence> usersInJail;
    private FacilityPrisonManager() {
        long millis = System.currentTimeMillis();
        this.usersInJail = new CopyOnWriteArrayList<>();
        LOGGER.info("Prison Manager -> Loaded! (" + (System.currentTimeMillis() - millis) + " MS)");
    }
    public Room getPrison() {
        return FacilityManager.getFirstRoomWithTag(RoomType.PRISON);
    }

    public PrisonSentence getPrisonTime(Habbo user) {
        if (this.usersInJail.isEmpty()) {
            return null;
        }
        return this.usersInJail.get(user.getHabboInfo().getId());
    }
    public void addPrisonTime(Habbo habbo, String crime, int timeLeft) {
        this.usersInJail.add(new PrisonSentence(habbo, crime, timeLeft, 0));

        Room prison = FacilityPrisonManager.getInstance().getPrison();

        if (habbo.getRoomUnit().getRoom().getRoomInfo().getId() != prison.getRoomInfo().getId()) {
            habbo.goToRoom(prison.getRoomInfo().getId());
        }

        Collection<RoomItem> prisonBenches = prison.getRoomItemManager().getItemsOfType(InteractionPrisonBench.class);
        for (RoomItem hospitalBedItem : prisonBenches) {
            List<RoomTile> hospitalBedRoomTiles = hospitalBedItem.getOccupyingTiles(prison.getLayout());
            RoomTile firstAvailableHospitalBedTile = hospitalBedRoomTiles.get(0);
            if (firstAvailableHospitalBedTile == null) {
                return;
            }
            habbo.getRoomUnit().setLocation(firstAvailableHospitalBedTile);
        }

        habbo.shout(Emulator.getTexts().getValue("roleplay.prison.starts_sentence"));
    }

    public void removePrisonTime(Habbo user) {
        if (this.getPrisonTime(user) == null) {
            return;
        }
        this.usersInJail.remove(user.getHabboInfo().getId());
        user.shout(Emulator.getTexts().getValue("roleplay.prison.finishes_sentence"));
    }

    public void cycle() {
        for (PrisonSentence userSentence : usersInJail) {
            if (userSentence.getTimeLeft() == 0) {
                this.removePrisonTime(userSentence.getHabbo());
                return;
            }
            userSentence.getHabbo().shout(Emulator.getTexts().
                    getValue("roleplay.prison.sentence_time_left")
                    .replace(":timeLeft", Integer.toString(userSentence.getTimeLeft()))
                    .replace(":timeServed", Integer.toString(userSentence.getTimeServed()))
            );
        }
    }

    public void dispose() {
    }
}