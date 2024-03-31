package com.eu.habbo.roleplay.facility;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FacilityHospitalsManager {

    public static String HOSPITAL_ROOM_TAG = "hospital";

    private static FacilityHospitalsManager instance;

    public static FacilityHospitalsManager getInstance() {
        if (instance == null) {
            instance = new FacilityHospitalsManager();
        }
        return instance;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(FacilityHospitalsManager.class);
    private final List<Habbo> usersToHeal;
    private FacilityHospitalsManager() {
        long millis = System.currentTimeMillis();
        this.usersToHeal = new CopyOnWriteArrayList<>();
        LOGGER.info("Hospital Manager -> Loaded! (" + (System.currentTimeMillis() - millis) + " MS)");
    }
    public Room getHospital() {
        return FacilityManager.getFirstRoomWithTag(FacilityHospitalsManager.HOSPITAL_ROOM_TAG);
    }

    public void addUserToHeal(Habbo user) {
        this.usersToHeal.add(user);
        user.shout(Emulator.getTexts().getValue("roleplay.hospital.starts_healing"));
    }

    public void removeUserToHeal(Habbo user) {
        if (this.usersToHeal.isEmpty()) {
            return;
        }
        if (this.usersToHeal.get(user.getHabboInfo().getId()) == null) {
            return;
        }
        this.usersToHeal.remove(user);
        user.shout(Emulator.getTexts().getValue("roleplay.hospital.stops_healing"));
    }

    public void cycle() {
        for (Habbo user : usersToHeal) {
            if ((user.getHabboRoleplayStats().getHealthNow() + 1) > user.getHabboRoleplayStats().getHealthMax()) {
                this.removeUserToHeal(user);
                return;
            }
            user.getHabboRoleplayStats().setHealth(user.getHabboRoleplayStats().getHealthNow() + 1);
        }
    }

    public void dispose() {
    }
}