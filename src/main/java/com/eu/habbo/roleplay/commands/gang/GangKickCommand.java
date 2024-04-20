package com.eu.habbo.roleplay.commands.gang;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildRank;

public class GangKickCommand extends Command {
    public GangKickCommand() {
        super("cmd_gang_kick");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null) {
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getGang() == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.roleplay.not_in_a_gang"));
            return true;
        }

        Guild gang = gameClient.getHabbo().getHabboRoleplayStats().getGang();

        boolean isGangOwner = gameClient.getHabbo().getHabboRoleplayStats().getGangPosition().getRank() == GuildRank.OWNER;
        boolean hasKickRights = gameClient.getHabbo().getHabboRoleplayStats().getGangPosition().getRank() == GuildRank.ADMIN;

        if (!isGangOwner && !hasKickRights) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_kick_not_allowed"));
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

        targetedHabbo.getHabboRoleplayStats().setGang(null);

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_kick_success").replace(":username", targetedHabbo.getHabboInfo().getUsername()));
        targetedHabbo.shout(Emulator.getTexts().getValue("commands.roleplay.cmd_gang_kick_received").replace(":gang", gang.getName()));

        return true;
    }
}