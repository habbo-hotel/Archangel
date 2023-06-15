package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WiredEffectGiveScore extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.GIVE_SCORE;

    private int score;
    private int count;

    private final TObjectIntMap<Map.Entry<Integer, Integer>> data = new TObjectIntHashMap<>();

    public WiredEffectGiveScore(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveScore(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        if (habbo != null && habbo.getHabboInfo().getCurrentGame() != null) {
            Game game = room.getGame(habbo.getHabboInfo().getCurrentGame());

            if (game == null)
                return false;

            int gameStartTime = game.getStartTime();

            TObjectIntMap<Map.Entry<Integer, Integer>> dataClone = new TObjectIntHashMap<>(this.data);

            TObjectIntIterator<Map.Entry<Integer, Integer>> iterator = dataClone.iterator();

            for (int i = dataClone.size(); i-- > 0; ) {
                iterator.advance();

                Map.Entry<Integer, Integer> map = iterator.key();

                if (map.getValue() == habbo.getHabboInfo().getId()) {
                    if (map.getKey() == gameStartTime) {
                        if (iterator.value() < this.count) {
                            iterator.setValue(iterator.value() + 1);

                            habbo.getHabboInfo().getGamePlayer().addScore(this.score, true);

                            return true;
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }

            try {
                this.data.put(new AbstractMap.SimpleEntry<>(gameStartTime, habbo.getHabboInfo().getId()), 1);
            }
            catch(IllegalArgumentException ignored) {

            }


            if (habbo.getHabboInfo().getGamePlayer() != null) {
                habbo.getHabboInfo().getGamePlayer().addScore(this.score, true);
            }

            return true;
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.score, this.count, this.getWiredSettings().getDelay()));
    }

    @Override
    public void loadWiredSettings(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if(wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.score = data.score;
            this.count = data.count;
            this.getWiredSettings().setDelay(data.delay);
        }
        else {
            String[] data = wiredData.split(";");

            if (data.length == 3) {
                this.score = Integer.parseInt(data[0]);
                this.count = Integer.parseInt(data[1]);
                this.getWiredSettings().setDelay(Integer.parseInt(data[2]));
            }

            this.needsUpdate(true);
        }
    }

    @Override
    public WiredEffectType getType() {
        return WiredEffectGiveScore.type;
    }

    @Override
    public boolean saveData() throws WiredSaveException {
        if(this.getWiredSettings().getIntegerParams().length < 2) throw new WiredSaveException("Invalid data");

        int score = this.getWiredSettings().getIntegerParams()[0];

        if(score < 1 || score > 100)
            throw new WiredSaveException("Score is invalid");

        int timesPerGame = this.getWiredSettings().getIntegerParams()[1];

        if(timesPerGame < 1 || timesPerGame > 10)
            throw new WiredSaveException("Times per game is invalid");

        int delay = this.getWiredSettings().getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.score = score;
        this.count = timesPerGame;
        this.getWiredSettings().setDelay(delay);

        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    static class JsonData {
        int score;
        int count;
        int delay;

        public JsonData(int score, int count, int delay) {
            this.score = score;
            this.count = count;
            this.delay = delay;
        }
    }
}
