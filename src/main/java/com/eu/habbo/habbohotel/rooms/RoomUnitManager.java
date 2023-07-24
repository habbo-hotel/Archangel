package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.rooms.entities.RoomRotation;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnitType;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomBot;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomHabbo;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomPet;
import com.eu.habbo.habbohotel.units.Unit;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.hotelview.CloseConnectionMessageComposer;
import com.eu.habbo.messages.outgoing.inventory.PetAddedToInventoryComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.RoomPetComposer;
import com.eu.habbo.messages.outgoing.rooms.users.UserRemoveMessageComposer;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.eu.habbo.database.DatabaseConstants.CAUGHT_SQL_EXCEPTION;

@Slf4j
@Getter
public class RoomUnitManager {
    private final Room room;
    private final ConcurrentHashMap<Integer, RoomUnit> currentRoomUnits;
    private final ConcurrentHashMap<Integer, Habbo> currentRoomHabbos;
    private final ConcurrentHashMap<Integer, Bot> currentRoomBots;
    private final ConcurrentHashMap<Integer, Pet> currentRoomPets;
    private volatile int roomUnitCounter;
    public final Object roomUnitLock;

    public RoomUnitManager(Room room) {
        this.room = room;
        this.currentRoomUnits = new ConcurrentHashMap<>();
        this.currentRoomHabbos = new ConcurrentHashMap<>();
        this.currentRoomBots = new ConcurrentHashMap<>();
        this.currentRoomPets = new ConcurrentHashMap<>();
        this.roomUnitCounter = 0;
        this.roomUnitLock = new Object();
    }

    public synchronized void load(Connection connection) {
        this.loadBots(connection);
        this.loadPets(connection);
    }

    private synchronized void loadBots(Connection connection) {
        this.currentRoomBots.clear();

        try (PreparedStatement statement = connection.prepareStatement("SELECT users.username AS owner_name, bots.* FROM bots INNER JOIN users ON bots.user_id = users.id WHERE room_id = ?")) {
            statement.setInt(1, this.room.getRoomInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Bot bot = Emulator.getGameEnvironment().getBotManager().loadBot(set);

                    if (bot != null) {
                        bot.setRoom(this.room);
                        bot.setRoomUnit(new RoomBot());
                        bot.getRoomUnit().setRoom(this.room);
                        bot.getRoomUnit().setLocation(this.room.getLayout().getTile((short) set.getInt("x"), (short) set.getInt("y")));
                        if (bot.getRoomUnit().getCurrentPosition() == null || bot.getRoomUnit().getCurrentPosition().getState() == RoomTileState.INVALID) {
                            bot.getRoomUnit().setCurrentZ(this.room.getLayout().getDoorTile().getStackHeight());
                            bot.getRoomUnit().setLocation(this.room.getLayout().getDoorTile());
                            bot.getRoomUnit().setRotation(RoomRotation.fromValue(this.room.getLayout().getDoorDirection()));
                        } else {
                            bot.getRoomUnit().setCurrentZ(set.getDouble("z"));
                            bot.getRoomUnit().setPreviousLocationZ(set.getDouble("z"));
                            bot.getRoomUnit().setRotation(RoomRotation.values()[set.getInt("rot")]);
                        }
                        bot.getRoomUnit().setRoomUnitType(RoomUnitType.BOT);
                        bot.getRoomUnit().setDanceType(DanceType.values()[set.getInt("dance")]);
                        bot.getRoomUnit().setInRoom(true);

                        bot.getRoomUnit().giveEffect(set.getInt("effect"), Integer.MAX_VALUE, false);

                        this.addRoomUnit(bot);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        } catch (Exception e) {
            log.error("Caught Exception", e);
        }
    }

    private synchronized void loadPets(Connection connection) {
        this.currentRoomPets.clear();

        try (PreparedStatement statement = connection.prepareStatement("SELECT users.username as pet_owner_name, users_pets.* FROM users_pets INNER JOIN users ON users_pets.user_id = users.id WHERE room_id = ?")) {
            statement.setInt(1, this.room.getRoomInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Pet pet = PetManager.loadPet(set);

                    pet.setRoom(this.room);
                    pet.setRoomUnit(new RoomPet());
                    pet.getRoomUnit().setRoom(this.room);
                    pet.getRoomUnit().setLocation(this.room.getLayout().getTile((short) set.getInt("x"), (short) set.getInt("y")));
                    if (pet.getRoomUnit().getCurrentPosition() == null || pet.getRoomUnit().getCurrentPosition().getState() == RoomTileState.INVALID) {
                        pet.getRoomUnit().setCurrentZ(this.room.getLayout().getDoorTile().getStackHeight());
                        pet.getRoomUnit().setLocation(this.room.getLayout().getDoorTile());
                        pet.getRoomUnit().setRotation(RoomRotation.fromValue(this.room.getLayout().getDoorDirection()));
                    } else {
                        pet.getRoomUnit().setCurrentZ(set.getDouble("z"));
                        pet.getRoomUnit().setRotation(RoomRotation.values()[set.getInt("rot")]);
                    }
                    pet.getRoomUnit().setRoomUnitType(RoomUnitType.PET);
                    pet.getRoomUnit().setCanWalk(true);
                    this.addRoomUnit(pet);
                    this.room.getFurniOwnerNames().put(pet.getUserId(), set.getString("pet_owner_name"));
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        } catch (Exception e) {
            log.error("Caught Exception", e);
        }
    }

    public void addRoomUnit(Unit unit) {
        synchronized (this.roomUnitLock) {
            unit.getRoomUnit().setVirtualId(this.roomUnitCounter);
            this.currentRoomUnits.put(unit.getRoomUnit().getVirtualId(), unit.getRoomUnit());
            this.roomUnitCounter++;

            switch (unit.getRoomUnit().getRoomUnitType()) {
                case HABBO -> {
                    this.currentRoomHabbos.put(((Habbo) unit).getHabboInfo().getId(), (Habbo) unit);
                    unit.getRoomUnit().getRoom().updateDatabaseUserCount();
                }
                case BOT -> this.currentRoomBots.put(((Bot) unit).getId(), (Bot) unit);
                case PET -> {
                    this.currentRoomPets.put(((Pet) unit).getId(), (Pet) unit);
                    Habbo habbo = this.getRoomHabboById(((Pet) unit).getUserId());
                    if (habbo != null) {
                        unit.getRoomUnit().getRoom().getFurniOwnerNames().put(((Pet) unit).getUserId(), this.getRoomHabboById(((Pet) unit).getUserId()).getHabboInfo().getUsername());
                    }
                }
            }
        }
    }

    public Collection<RoomUnit> getRoomUnitsAt(RoomTile tile) {
        return this.currentRoomUnits.values().stream().filter(roomUnit -> roomUnit.getCurrentPosition().equals(tile)).collect(Collectors.toSet());
    }

    public boolean areRoomUnitsAt(RoomTile tile) {
        return this.currentRoomUnits.values().stream().anyMatch(roomUnit -> roomUnit.getCurrentPosition().equals(tile));
    }

    public Collection<RoomUnit> getAvatarsAt(RoomTile tile) {
        return Stream.concat(this.getHabbosAt(tile).stream(), this.getBotsAt(tile).stream()).map(Unit::getRoomUnit).collect(Collectors.toList());
    }

    public Collection<Habbo> getRoomHabbos() {
        return this.currentRoomHabbos.values();
    }

    public int getRoomHabbosCount() {
        return this.currentRoomHabbos.size();
    }

    public boolean hasHabbosAt(RoomTile tile) {
        return this.currentRoomHabbos.values().stream().anyMatch(habbo -> habbo.getRoomUnit().getCurrentPosition().equals(tile));
    }

    public Collection<Habbo> getHabbosAt(RoomTile tile) {
        return this.currentRoomHabbos.values().stream().filter(habbo -> habbo.getRoomUnit().getCurrentPosition().equals(tile)).collect(Collectors.toSet());
    }

    public Habbo getRoomHabboById(int habboId) {
        return this.currentRoomHabbos.get(habboId);
    }

    public Habbo getRoomHabboByUsername(String username) {
        return this.currentRoomHabbos.values().stream().filter(habbo -> habbo.getHabboInfo().getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    public Habbo getHabboByVirtualId(int virtualId) {
        return this.currentRoomHabbos.values().stream().filter(habbo -> habbo.getRoomUnit().getVirtualId() == virtualId).findFirst().orElse(null);
    }

    public Habbo getHabboByRoomUnit(RoomUnit roomUnit) {
        return this.currentRoomHabbos.values().stream().filter(habbo -> habbo.getRoomUnit() == roomUnit).findFirst().orElse(null);
    }

    public Bot getRoomBotById(int botId) {
        return this.currentRoomBots.get(botId);
    }

    public List<Bot> getBotsByName(String name) {
        synchronized (this.currentRoomBots) {
            return currentRoomBots.values().stream().filter(bot -> bot.getName().equalsIgnoreCase(name)).toList();
        }
    }

    public Bot getBotByRoomUnit(RoomUnit roomUnit) {
        return this.currentRoomBots.values().stream().filter(bot -> bot.getRoomUnit() == roomUnit).findFirst().orElse(null);
    }

    public boolean hasBotsAt(RoomTile tile) {
        return this.currentRoomBots.values().stream().anyMatch(bot -> bot.getRoomUnit().getCurrentPosition().equals(tile));
    }

    public Collection<Bot> getBotsAt(RoomTile tile) {
        return this.currentRoomBots.values().stream().filter(bot -> bot.getRoomUnit().getCurrentPosition().equals(tile)).collect(Collectors.toSet());
    }

    public void placePet(Pet pet, Room room, short x, short y, double z) {
        synchronized (this.currentRoomPets) {
            RoomTile tile = room.getLayout().getTile(x, y);

            if (tile == null) {
                tile = room.getLayout().getDoorTile();
            }

            pet.setRoomUnit(new RoomPet());
            pet.setRoom(room);
            pet.getRoomUnit()
                    .setGoalLocation(tile)
                    .setLocation(tile)
                    .setRoomUnitType(RoomUnitType.PET)
                    .setCanWalk(true)
                    .setPreviousLocationZ(z)
                    .setCurrentZ(z)
                    .setRoom(room);

            if (pet.getRoomUnit().getCurrentPosition() == null) {
                pet.getRoomUnit()
                        .setLocation(room.getLayout().getDoorTile())
                        .setRotation(RoomRotation.fromValue(room.getLayout().getDoorDirection()));
            }

            pet.setNeedsUpdate(true);
            room.getFurniOwnerNames().put(pet.getUserId(), this.getRoomHabboById(pet.getUserId()).getHabboInfo().getUsername());
            this.addRoomUnit(pet);
            room.sendComposer(new RoomPetComposer(pet).compose());
        }
    }

    public boolean hasPetsAt(RoomTile tile) {
        return this.currentRoomPets.values().stream().anyMatch(pet -> pet.getRoomUnit().getCurrentPosition().equals(tile));
    }

    public Collection<Pet> getPetsAt(RoomTile tile) {
        return this.currentRoomPets.values().stream().filter(pet -> pet.getRoomUnit().getCurrentPosition().equals(tile)).collect(Collectors.toSet());
    }

    public Pet getRoomPetById(int petId) {
        return this.currentRoomPets.get(petId);
    }

    public Pet getPetByRoomUnit(RoomUnit roomUnit) {
        return this.currentRoomPets.values().stream().filter(pet -> pet.getRoomUnit() == roomUnit).findFirst().orElse(null);
    }

    public void pickUpMyPets(Habbo owner) {
        THashSet<Pet> pets = new THashSet<>();

        synchronized (this.currentRoomPets) {
            for (Pet pet : this.currentRoomPets.values()) {
                if (pet.getUserId() == owner.getHabboInfo().getId()) {
                    pets.add(pet);
                }
            }
        }

        for (Pet pet : pets) {
            pet.removeFromRoom();
            Emulator.getThreading().run(pet);
            owner.getInventory().getPetsComponent().addPet(pet);
            owner.getClient().sendResponse(new PetAddedToInventoryComposer(pet));
            this.currentRoomPets.remove(pet.getId());
        }
    }

    public void removeHabbo(Habbo habbo, boolean sendRemovePacket) {
        if(!this.currentRoomHabbos.containsKey(habbo.getHabboInfo().getId())) {
            return;
        }

        RoomUnit roomUnit = habbo.getRoomUnit();

        if(roomUnit == null || !(roomUnit instanceof RoomHabbo roomHabbo)) {
            return;
        }

        if(roomHabbo.getCurrentPosition() != null) {
            roomHabbo.getCurrentPosition().removeUnit(habbo.getRoomUnit());
        }

        synchronized (this.roomUnitLock) {
            this.currentRoomHabbos.remove(habbo.getHabboInfo().getId());
            this.currentRoomUnits.remove(roomHabbo.getVirtualId());
        }

        roomHabbo.getRoom().sendComposer(new UserRemoveMessageComposer(roomHabbo).compose());

        RoomItem item = roomHabbo.getRoom().getRoomItemManager().getTopItemAt(roomHabbo.getCurrentPosition());

        if (item != null) {
            try {
                item.onWalkOff(habbo.getRoomUnit(), roomHabbo.getRoom(), new Object[]{});
            } catch (Exception e) {
                log.error("Caught Exception", e);
            }
        }

        if (habbo.getHabboInfo().getCurrentGame() != null && roomHabbo.getRoom().getGame(habbo.getHabboInfo().getCurrentGame()) != null) {
            roomHabbo.getRoom().getGame(habbo.getHabboInfo().getCurrentGame()).removeHabbo(habbo);
        }

        RoomTrade trade = roomHabbo.getRoom().getActiveTradeForHabbo(habbo);

        if (trade != null) {
            trade.stopTrade(habbo);
        }

        if (!roomHabbo.getRoom().getRoomInfo().isRoomOwner(habbo)) {
            this.pickUpMyPets(habbo);
        }

        roomHabbo.getRoom().updateDatabaseUserCount();

        roomHabbo.clear();
    }

    public boolean removeBot(Bot bot) {
        synchronized (this.currentRoomBots) {
            if (this.currentRoomBots.containsKey(bot.getId())) {
                if (bot.getRoomUnit() != null && bot.getRoomUnit().getCurrentPosition() != null) {
                    bot.getRoomUnit().getCurrentPosition().removeUnit(bot.getRoomUnit());
                }

                this.currentRoomBots.remove(bot.getId());
                this.currentRoomUnits.remove(bot.getRoomUnit().getVirtualId());

                bot.getRoomUnit().setInRoom(false);
                bot.setRoom(null);
                bot.getRoomUnit().getRoom().sendComposer(new UserRemoveMessageComposer(bot.getRoomUnit()).compose());
                bot.setRoomUnit(null);
                return true;
            }
        }

        return false;
    }

    public Pet removePet(int petId) {
        Pet pet = this.currentRoomPets.get(petId);
        this.currentRoomUnits.remove(pet.getRoomUnit().getVirtualId());
        return this.currentRoomPets.remove(petId);
    }

    public void removeAllPetsExceptRoomOwner() {
        ArrayList<Pet> toRemovePets = new ArrayList<>();
        ArrayList<Pet> removedPets = new ArrayList<>();
        synchronized (this.currentRoomPets) {
            for (Pet pet : this.currentRoomPets.values()) {
                try {
                    if (pet.getUserId() != pet.getRoomUnit().getRoom().getRoomInfo().getOwnerInfo().getId()) {
                        toRemovePets.add(pet);
                    }

                } catch (NoSuchElementException e) {
                    log.error("Caught exception", e);
                    break;
                }
            }
        }

        for (Pet pet : toRemovePets) {
            removedPets.add(pet);

            pet.removeFromRoom();

            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(pet.getUserId());
            if (habbo != null) {
                habbo.getInventory().getPetsComponent().addPet(pet);
                habbo.getClient().sendResponse(new PetAddedToInventoryComposer(pet));
            }

            pet.setNeedsUpdate(true);
            pet.run();
        }

        for (Pet pet : removedPets) {
            this.currentRoomPets.remove(pet.getId());
            this.currentRoomUnits.remove(pet.getRoomUnit().getVirtualId());
        }
    }

    public void clear() {
        synchronized (this.roomUnitLock) {
            this.currentRoomUnits.clear();
            this.currentRoomHabbos.clear();
            this.currentRoomBots.clear();
            this.currentRoomPets.clear();
            this.roomUnitCounter = 0;
        }
    }

    public void dispose(Room room) {
        for(Habbo habbo : this.currentRoomHabbos.values()) {
            Emulator.getGameEnvironment().getRoomManager().leaveRoom(habbo, room);
        }

        room.sendComposer(new CloseConnectionMessageComposer().compose());

        this.currentRoomHabbos.clear();

        Iterator<Bot> botIterator = this.currentRoomBots.values().iterator();

        while(botIterator.hasNext()) {
            try {
                Bot bot = botIterator.next();
                bot.needsUpdate(true);
                Emulator.getThreading().run(bot);
            } catch (NoSuchElementException e) {
                log.error("Caught Exception", e);
                break;
            }
        }

        this.currentRoomBots.clear();
        this.currentRoomPets.clear();

        this.currentRoomUnits.clear();
    }
}
