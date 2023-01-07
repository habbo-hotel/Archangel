package com.eu.habbo.habbohotel.pets;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.users.UserUpdateComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class representing a command that can be given to a pet.
 */
public class PetCommand implements Comparable<PetCommand> {

    /** The ID of the command. */
    public final int id;

    /** The key (name) of the command. */
    public final String key;

    /** The level required to use the command. */
    public final int level;

    /** The amount of XP rewarded for using the command. */
    public final int xp;

    /** The cost in energy to use the command. */
    public final int energyCost;

    /** The cost in happiness to use the command. */
    public final int happynessCost;

    /** The action associated with the command. */
    public final PetAction action;

    /**
     * Creates a new PetCommand instance.
     * @param set The ResultSet to get data from.
     * @param action The PetAction associated with the command.
     * @throws SQLException If a database error occurs.
     */
    public PetCommand(ResultSet set, PetAction action) throws SQLException {
        this.id = set.getInt("command_id");
        this.key = set.getString("text");
        this.level = set.getInt("required_level");
        this.xp = set.getInt("reward_xp");
        this.energyCost = set.getInt("cost_energy");
        this.happynessCost = set.getInt("cost_happyness");
        this.action = action;
    }

    /**
     * Compares this PetCommand to another PetCommand based on the required level to use them.
     * @param o The other PetCommand to compare to.
     * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(PetCommand o) {
        return this.level - o.level;
    }

    /**
     * Handles the execution of the command for a given pet, Habbo, and data.
     * If the pet does not have enough energy or happiness, or if a random check fails, the pet will "disobey" the command.
     * Otherwise, the action associated with the command is applied and the pet's energy, happiness, and experience are updated.
     * @param pet The pet to execute the command on.
     * @param habbo The Habbo giving the command.
     * @param data The data associated with the command.
     */
    public void handle(Pet pet, Habbo habbo, String[] data) {
        // check if enough energy, happiness, and randomize do or dont || should possibly add if not hungry and thirsty but @brenoepic does those - oliver
        if (this.action != null && pet.energy > this.energyCost && pet.happyness > this.happynessCost && Emulator.getRandom().nextInt((pet.level - this.level <= 0 ? 2 : pet.level - this.level) + 2) == 0) {
            if (this.action.petTask != pet.getTask()) {
                if (this.action.stopsPetWalking) {
                    pet.getRoomUnit().setGoalLocation(pet.getRoomUnit().getCurrentLocation());
                }
                if (this.action.apply(pet, habbo, data)) {
                    for (RoomUnitStatus status : this.action.statusToRemove) {
                        pet.getRoomUnit().removeStatus(status);
                    }

                    for (RoomUnitStatus status : this.action.statusToSet) {
                        pet.getRoomUnit().setStatus(status, "0");
                    }

                    pet.getRoomUnit().setStatus(RoomUnitStatus.GESTURE, this.action.gestureToSet);
                    pet.getRoom().sendComposer(new UserUpdateComposer(pet.getRoomUnit()).compose());
                    pet.addEnergy(-this.energyCost);
                    pet.addHappyness(-this.happynessCost);
                    pet.addExperience(this.xp);
                }
            }
        } else {
            // this is disobey
            if (this.action.apply(pet, habbo, data)) {
                pet.addEnergy(-this.energyCost / 2);
                pet.addHappyness(-this.happynessCost / 2);
            }

            pet.say(pet.petData.randomVocal(PetVocalsType.DISOBEY));
            return;
        }
    }
}

