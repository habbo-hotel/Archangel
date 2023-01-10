package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateNavigatorCommand extends Command {
    public UpdateNavigatorCommand() {
        super("cmd_update_navigator", Emulator.getTexts().getValue("commands.keys.cmd_update_navigator").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Emulator.getGameEnvironment().getNavigatorManager().loadNavigator();
        Emulator.getGameEnvironment().getRoomManager().loadRoomModels();
        Emulator.getGameEnvironment().getRoomManager().loadPublicRooms();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_navigator"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}