package com.eu.habbo.roleplay.commands.gang;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;

public class GangInviteUserCommand extends Command {
    public GangInviteUserCommand() {
        super("cmd_gang_invite");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null) {
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getGang() == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_invite_not_in_a_gang"));
            return true;
        }


        String targetedUsername = params[1];

        if (targetedUsername == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found"));
            return true;
        }

        Habbo targetedHabbo = gameClient.getHabbo().getRoomUnit().getRoom().getRoomUnitManager().getRoomHabboByUsername(targetedUsername);

        if (targetedHabbo == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found"));
            return true;
        }

        if (targetedHabbo.getHabboRoleplayStats().getGang() != null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_invite_user_already_in_gang"));
            return true;
        }

        gameClient.getHabbo().getHabboRoleplayStats().getGang().addInvitedUser(targetedHabbo);

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_invite_sent").replace(":username", targetedHabbo.getHabboInfo().getUsername()));

        targetedHabbo.whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_invite_received").replace(":gang",gameClient.getHabbo().getHabboRoleplayStats().getGang().getName()));

        return true;
    }
}