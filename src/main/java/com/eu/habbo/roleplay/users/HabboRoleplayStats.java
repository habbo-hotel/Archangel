package com.eu.habbo.roleplay.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.corp.Corp;
import com.eu.habbo.roleplay.corp.CorpManager;
import com.eu.habbo.roleplay.corp.CorpPosition;
import com.eu.habbo.roleplay.corp.CorpShiftManager;
import com.eu.habbo.roleplay.government.GovernmentManager;
import com.eu.habbo.roleplay.items.interactions.InteractionHospitalBed;
import com.eu.habbo.roleplay.messages.outgoing.user.UserRoleplayStatsChangeComposer;
import com.eu.habbo.roleplay.room.FacilityHospitalManager;
import com.eu.habbo.roleplay.weapons.Weapon;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class HabboRoleplayStats implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HabboRoleplayStats.class);

    private static HabboRoleplayStats createNewStats(Habbo habbo) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO rp_users_stats (user_id, corporation_id, corporation_position_id) VALUES (?, ?, ?)")) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            statement.setInt(2, GovernmentManager.getInstance().getWelfareCorp().getGuild().getId());
            statement.setInt(3,GovernmentManager.getInstance().getWelfareCorp().getPositionByOrderID(1).getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return load(habbo);
    }

    public static HabboRoleplayStats load(Habbo habbo) {
        HabboRoleplayStats stats = null;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM rp_users_stats WHERE user_id = ? LIMIT 1")) {
                statement.setInt(1, habbo.getHabboInfo().getId());
                try (ResultSet set = statement.executeQuery()) {
                    set.next();
                    if (set.getRow() != 0) {
                        stats = new HabboRoleplayStats(set, habbo);
                    } else {
                        stats = createNewStats(habbo);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        return stats;
    }

    private Habbo habbo;
    @Getter
    private int healthNow;
    @Getter
    private int healthMax;
    @Getter
    private int energyNow;
    @Getter
    private int energyMax;
    @Getter
    private int hungerNow;
    @Getter
    private int hungerMax;
    private int corporationID;
    private int corporationPositionID;
    private Integer gangID;
    @Getter
    private boolean isDead;
    @Getter
    private boolean isStunned;
    @Getter
    private boolean isCuffed;
    @Getter
    private Habbo escortedBy;
    @Getter
    private Habbo isEscorting;
    @Getter
    private short lastPosX;
    @Getter
    private short lastPosY;

    public void setHealth(int healthCurrent) {
        this.setHealth(healthCurrent, false);
    }

    public void setHealth(int currentHealth, boolean overrideMaxHealth) {
        this.healthNow = currentHealth;

        if (this.healthNow > this.healthMax && overrideMaxHealth) {
            this.healthMax = this.healthNow;
        }

        if (this.healthNow < 0) {
            this.healthNow = 0;
        }

        if (this.healthNow == 0) {
            this.setIsDead(true);
        }

        if (this.healthNow > 0 && this.isDead) {
            this.setIsDead(false);
        }
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public Corp getCorp() {
        return CorpManager.getInstance().getCorpByID(this.corporationID);
    }

    public void setCorp(int corporationID, int corporationPositionID) {
        this.corporationID = corporationID;
        this.corporationPositionID = corporationPositionID;
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
        this.run();
    }

    public CorpPosition getCorpPosition() {
        return this.getCorp().getPositionByID(this.corporationPositionID);
    }

    public Guild getGang() {
        if (this.gangID == null) {
            return null;
        }
        return Emulator.getGameEnvironment().getGuildManager().getGuild(this.gangID);
    }

    public void setGang(Integer gangID ) {
        this.gangID = gangID;
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
        this.run();
    }

    public GuildMember getGangPosition() {
        return Emulator.getGameEnvironment().getGuildManager().getGuildMember(this.gangID, this.habbo.getHabboInfo().getId());
    }

    public void setIsDead(boolean isDead) {
        this.isDead = isDead;
        this.habbo.getRoomUnit().setCanWalk(isDead);

        if (this.isDead) {

            if (this.habbo.getHabboRoleplayStats().getIsEscorting() != null) {
                this.habbo.getHabboRoleplayStats().setIsEscorting(null);
            }

            this.habbo.shout(Emulator.getTexts().getValue("roleplay.user_is_dead"));
            this.habbo.getRoomUnit().setCanWalk(false);
            Room hospitalRoom = FacilityHospitalManager.getInstance().getHospital();

            if (hospitalRoom == null) {
                return;
            }

            if (this.habbo.getRoomUnit().getRoom().getRoomInfo().getId() != hospitalRoom.getRoomInfo().getId()) {
                this.habbo.goToRoom(hospitalRoom.getRoomInfo().getId());
            }

            Collection<RoomItem> hospitalBedItems = hospitalRoom.getRoomItemManager().getItemsOfType(InteractionHospitalBed.class);
            for (RoomItem hospitalBedItem : hospitalBedItems) {
                List<RoomTile> hospitalBedRoomTiles = hospitalBedItem.getOccupyingTiles(hospitalRoom.getLayout());
                RoomTile firstAvailableHospitalBedTile = hospitalBedRoomTiles.get(0);
                if (firstAvailableHospitalBedTile == null) {
                    return;
                }
                this.habbo.getRoomUnit().setLocation(firstAvailableHospitalBedTile);
            }

        }


        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public void setIsStunned(boolean isStunned) {
        this.isStunned = isStunned;
        this.habbo.getRoomUnit().setCanWalk(!isStunned);
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public void setIsCuffed(boolean isCuffed) {
        this.isCuffed = isCuffed;
        if (!this.isStunned) this.habbo.getRoomUnit().setCanWalk(!isCuffed);
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public void setEscortedBy(Habbo escortedBy) {
        this.escortedBy = escortedBy;
        if (!this.isCuffed) this.habbo.getRoomUnit().setCanWalk(escortedBy != null);
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public void setIsEscorting(Habbo user) {
        Habbo oldUser = this.isEscorting;
        if (oldUser != null && oldUser != user) {
            oldUser.getHabboRoleplayStats().setEscortedBy(null);
        }
        if (oldUser == null && user == null) {
            this.habbo.shout(Emulator.getTexts().getValue("commands.roleplay_cmd_escort_stop"));
        }
        if (user != null) {
            this.habbo.shout(Emulator.getTexts().getValue("commands.roleplay_cmd_escort_start").replace(":username", user.getHabboInfo().getUsername()));
        }
        this.isEscorting = user;
        this.habbo.getRoomUnit().getRoom().sendComposer(new UserRoleplayStatsChangeComposer(this.habbo).compose());
    }

    public void setLastPos(short x, short y) {
        this.lastPosX = x;
        this.lastPosY = y;
    }

    public int getDamageModifier() {
        Random random = new Random();
        int damageModifier = random.nextInt(10) + 1;
        if (this.habbo.getInventory().getWeaponsComponent().getEquippedWeapon() != null) {
            Weapon equippedWeapon = this.habbo.getInventory().getWeaponsComponent().getEquippedWeapon().getWeapon();
            damageModifier += random.nextInt(equippedWeapon.getMaxDamage() - equippedWeapon.getMinDamage() + equippedWeapon.getMaxDamage());
        }
        return damageModifier;
    }

    public boolean isWorking() {
        return CorpShiftManager.getInstance().isUserWorking(this.habbo);
    }

    private HabboRoleplayStats(ResultSet set, Habbo habbo) throws SQLException {
        this.habbo = habbo;
        this.isDead = set.getInt("health_now") <= 0;
        this.healthNow = set.getInt("health_now");
        this.healthMax = set.getInt("health_max");
        this.energyNow = set.getInt("energy_now");
        this.energyMax = set.getInt("energy_max");
        this.hungerNow = set.getInt("hunger_now");
        this.hungerMax = set.getInt("hunger_max");
        this.corporationID = set.getInt("corporation_id");
        this.corporationPositionID = set.getInt("corporation_position_id");
        this.gangID = set.getInt("gang_id") != 0 ? set.getInt("gang_id") : null;
        this.lastPosX = set.getShort("last_pos_x");
        this.lastPosY = set.getShort("last_pos_y");
    }

    public void dispose() {
        this.run();
        this.habbo = null;
    }

    @Override
    public void run() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE rp_users_stats SET health_now = ?, health_max = ?, energy_now = ?, energy_max = ?, hunger_now = ?, hunger_max = ?, corporation_id = ?, corporation_position_id = ?, gang_id = ?, last_pos_x = ?, last_pos_y = ? WHERE user_id = ? LIMIT 1")) {
                statement.setInt(1, this.healthNow);
                statement.setInt(2, this.healthMax);
                statement.setInt(3, this.energyNow);
                statement.setInt(4, this.energyMax);
                statement.setInt(5, this.hungerNow);
                statement.setInt(6, this.hungerMax);
                statement.setInt(7, this.corporationID);
                statement.setInt(8, this.corporationPositionID);

                if (this.gangID != null) statement.setInt(9, this.gangID);
                if (this.gangID == null) statement.setNull(9, Types.INTEGER);

                statement.setShort(10, this.lastPosX);
                statement.setShort(11, this.lastPosY);

                statement.setInt(12, this.habbo.getHabboInfo().getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }


}