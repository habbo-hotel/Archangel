package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.interfaces.WiredTriggerReset;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.threading.runnables.WiredExecuteTask;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredTriggerAtSetTime extends InteractionWiredTrigger implements WiredTriggerReset {
    public final int PARAM_EXECUTE_TIME = 0;

    public int taskId;

    public WiredTriggerAtSetTime(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerAtSetTime(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        Emulator.getThreading().run(new WiredExecuteTask(this, Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId())), this.getWiredSettings().getIntegerParams().get(PARAM_EXECUTE_TIME)); //TODO *500?
        return true;
    }

    @Override
    public void loadDefaultIntegerParams() {
        if(this.getWiredSettings().getIntegerParams().size() == 0) {
            this.getWiredSettings().getIntegerParams().add(1);
        }
    }

    @Override
    public void resetTimer() {
        this.taskId++;

        Emulator.getThreading().run(new WiredExecuteTask(this, Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId())), this.getWiredSettings().getIntegerParams().get(PARAM_EXECUTE_TIME));
    }

    @Override
    public WiredTriggerType getType() {
        return WiredTriggerType.AT_GIVEN_TIME;
    }
}
