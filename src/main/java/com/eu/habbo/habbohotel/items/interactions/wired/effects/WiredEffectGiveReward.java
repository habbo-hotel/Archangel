package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredGiveRewardItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.generic.alerts.WiredValidationErrorComposer;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class WiredEffectGiveReward extends InteractionWiredEffect {
    public final static int LIMIT_ONCE = 0;
    public final static int LIMIT_N_DAY = 1;
    public final static int LIMIT_N_HOURS = 2;
    public final static int LIMIT_N_MINUTES = 3;
    private final static WiredEffectType type = WiredEffectType.GIVE_REWARD;
    private int limit;
    private int limitationInterval;
    private int given;
    private int rewardTime;
    private boolean uniqueRewards;
    private final THashSet<WiredGiveRewardItem> rewardItems = new THashSet<>();

    public WiredEffectGiveReward(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveReward(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Habbo habbo = room.getHabbo(roomUnit);

        return habbo != null && WiredHandler.getReward(habbo, this);
    }

    @Override
    public String getWiredData() {
        ArrayList<WiredGiveRewardItem> rewards = new ArrayList<>(this.rewardItems);
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.limit, this.given, this.rewardTime, this.uniqueRewards, this.limitationInterval, rewards, this.getWiredSettings().getDelay()));
    }

    @Override
    public void loadWiredSettings(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            this.getWiredSettings().setDelay(data.delay);
            this.limit = data.limit;
            this.given = data.given;
            this.rewardTime = data.reward_time;
            this.uniqueRewards = data.unique_rewards;
            this.limitationInterval = data.limit_interval;
            this.rewardItems.clear();
            this.rewardItems.addAll(data.rewards);
        } else {
            String[] data = wiredData.split(":");
            if (data.length > 0) {
                this.limit = Integer.parseInt(data[0]);
                this.given = Integer.parseInt(data[1]);
                this.rewardTime = Integer.parseInt(data[2]);
                this.uniqueRewards = data[3].equals("1");
                this.limitationInterval = Integer.parseInt(data[4]);
                this.getWiredSettings().setDelay(Integer.parseInt(data[5]));

                if (data.length > 6) {
                    if (!data[6].equalsIgnoreCase("\t")) {
                        String[] items = data[6].split(";");

                        this.rewardItems.clear();

                        for (String s : items) {
                            try {
                                this.rewardItems.add(new WiredGiveRewardItem(s));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

                this.needsUpdate(true);
            }
        }
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        super.onClick(client, room, objects);

        if (client.getHabbo().hasRight(Permission.ACC_SUPERWIRED)) {
            client.getHabbo().whisper(Emulator.getTexts().getValue("hotel.wired.superwired.info"), RoomChatMessageBubbles.BOT);
        }
    }

    @Override
    public boolean saveData() throws WiredSaveException {
            if (this.getWiredSettings().getIntegerParams().length < 4) throw new WiredSaveException("Invalid data");
            this.rewardTime = this.getWiredSettings().getIntegerParams()[0];
            this.uniqueRewards = this.getWiredSettings().getIntegerParams()[1] == 1;
            this.limit = this.getWiredSettings().getIntegerParams()[2];
            this.limitationInterval = this.getWiredSettings().getIntegerParams()[3];
            this.given = 0;

            String data = this.getWiredSettings().getStringParam();

            String[] items = data.split(";");

            this.rewardItems.clear();

            int i = 1;
            for (String s : items) {
                String[] d = s.split(",");

                if (d.length == 3) {
                    if (!(d[1].contains(":") || d[1].contains(";"))) {
                        this.rewardItems.add(new WiredGiveRewardItem(i, d[0].equalsIgnoreCase("0"), d[1], Integer.parseInt(d[2])));
                        continue;
                    }
                }

                //TODO THROW ERROR
//                gameClient.sendResponse(new WiredValidationErrorComposer(Emulator.getTexts().getValue("alert.superwired.invalid")));
                return false;
            }

            this.getWiredSettings().setDelay(this.getWiredSettings().getDelay());

            WiredHandler.dropRewards(this.getId());
            return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    @Override
    protected long requiredCooldown() {
        return 0;
    }

    public void incrementGiven() {
        given++;
    }

    static class JsonData {
        int limit;
        int given;
        int reward_time;
        boolean unique_rewards;
        int limit_interval;
        List<WiredGiveRewardItem> rewards;
        int delay;

        public JsonData(int limit, int given, int reward_time, boolean unique_rewards, int limit_interval, List<WiredGiveRewardItem> rewards, int delay) {
            this.limit = limit;
            this.given = given;
            this.reward_time = reward_time;
            this.unique_rewards = unique_rewards;
            this.limit_interval = limit_interval;
            this.rewards = rewards;
            this.delay = delay;
        }
    }
}
