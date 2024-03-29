package com.eu.habbo.roleplay.gangs;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class Gang {

    private static final Logger LOGGER = LoggerFactory.getLogger(GangPosition.class);

    @Getter
    private int id;
    @Getter
    private int userID;
    @Getter
    @Setter
    private int roomID;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String description;
    @Getter
    private TIntObjectHashMap<GangPosition> positions;
    private TIntObjectHashMap<Habbo> invitedUsers;

    public void addInvitedUser(Habbo habbo) {
        this.invitedUsers.put(habbo.getHabboInfo().getId(), habbo);
    }

    public Habbo getInvitedUser(Habbo habbo) {
        return this.invitedUsers.get(habbo.getHabboInfo().getId());
    }

    public void removeInvitedUser(Habbo habbo) {
        this.invitedUsers.remove(habbo.getHabboInfo().getId());
    }

    public GangPosition getPositionByID(int positionID) {
        return this.positions.get(positionID);
    }

    public GangPosition getPositionByOrderID(int orderID) {
        int[] keys = positions.keys();
        for (int key : keys) {
            GangPosition position = positions.get(key);
            if (position.getOrderID() == orderID) {
                return position;
            }
        }
        return null;
    }

    public void addPosition(GangPosition position) {
        this.positions.put(position.getId(), position);
    }

    public Gang(ResultSet set) throws SQLException {
        this.load(set);
    }

    public void load(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.userID = set.getInt("user_id");
        this.name = set.getString("name");
        this.description = set.getString("description");
        this.positions = new TIntObjectHashMap<>();
        this.invitedUsers = new TIntObjectHashMap<>();
        this.loadPositions();
    }

    private void loadPositions() {
        this.positions.clear();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM rp_gangs_positions WHERE gang_id = ?")) {
                statement.setInt(1, this.getId());
                ResultSet set = statement.executeQuery();
                while (set.next()) {
                    GangPosition position = null;
                    if (!this.positions.containsKey(set.getInt("id"))) {
                        position = new GangPosition(set);
                        this.positions.put(set.getInt("id"), position);
                    } else {
                        position = this.positions.get(set.getInt("id"));
                        position.load(set);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }


}