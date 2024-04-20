package com.eu.habbo.roleplay.commands.police;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.corp.CorpType;
import com.eu.habbo.roleplay.room.FacilityPrisonManager;

public class ReleaseCommand extends Command {
    public ReleaseCommand() {
        super("cmd_police_release");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null || params.length == 0) {
            return true;
        }

        String targetedUsername = params[1];

        if (gameClient.getHabbo().getHabboRoleplayStats().isStunned() || gameClient.getHabbo().getHabboRoleplayStats().isCuffed() || gameClient.getHabbo().getHabboRoleplayStats().getEscortedBy() != null || gameClient.getHabbo().getHabboRoleplayStats().isDead()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("roleplay.generic.not_allowed"));
            return true;
        }

        if (targetedUsername == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().isDead()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_hit_you_are_dead"));
            return true;
        }

        Habbo targetedHabbo = gameClient.getHabbo().getRoomUnit().getRoom().getRoomUnitManager().getRoomHabboByUsername(targetedUsername);

        if (targetedHabbo == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found").replace(":username", targetedUsername));
            return true;
        }

        if (!gameClient.getHabbo().getHabboRoleplayStats().getCorp().getTags().contains(CorpType.POLICE)) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.roleplay.police_only"));
            return true;
        }

        if (!gameClient.getHabbo().getHabboRoleplayStats().isWorking()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.roleplay.must_be_working"));
            return true;
        }

        FacilityPrisonManager.getInstance().removePrisonTime(targetedHabbo);

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay_cmd_release_success").replace(":username", targetedHabbo.getHabboInfo().getUsername()));

        return true;
    }
}