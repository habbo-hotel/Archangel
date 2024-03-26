package com.eu.habbo.roleplay.corporations;

import com.eu.habbo.Emulator;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

@Getter
public class Corporation {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorporationPosition.class);
    private int id;
    private int userID;
    @Setter
    private int roomID;
    @Setter
    private String name;
    @Setter
    private String description;
    private TIntObjectHashMap<CorporationPosition> positions;

    public CorporationPosition getPositionByOrderID(int orderID) {
        int[] keys = positions.keys();
        for (int key : keys) {
            CorporationPosition position = positions.get(key);
            if (position.getOrderID() == orderID) {
                return position;
            }
        }
        return null;
    }

    @Getter
    private List<String> tags;

    public CorporationPosition getPositionByID(int positionID) {
        return this.positions.get(positionID);
    }

    public Corporation(ResultSet set) throws SQLException {
        this.load(set);
    }

    public void load(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.userID = set.getInt("user_id");
        this.name = set.getString("name");
        this.description = set.getString("description");
        this.positions = new TIntObjectHashMap<>();
        this.tags = Arrays.stream(set.getString("tags").split(";")).toList();
        this.loadPositions();
    }

    private void loadPositions() {
        this.positions.clear();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM rp_corporation_positions WHERE corporation_id = ? LIMIT 1")) {
                statement.setInt(1, this.getId());
                ResultSet set = statement.executeQuery();
                while (set.next()) {
                    CorporationPosition position = null;
                    if (!this.positions.containsKey(set.getInt("id"))) {
                        position = new CorporationPosition(set);
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