package com.eu.habbo.roleplay.commands.corp;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;

public class CorpOfferUserJobCommand extends Command {
    public CorpOfferUserJobCommand() {
        super("cmd_corp_offerjob");
    }
    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null) {
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

        if (!gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().isCanHire()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_hire_not_allowed"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getCorp().getGuild().getId() == targetedHabbo.getHabboRoleplayStats().getCorp().getGuild().getId()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_hire_user_has_same_employer"));
            return true;
        }


        gameClient.getHabbo().getHabboRoleplayStats().getCorp().addInvitedUser(targetedHabbo);

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_corp_invite_sent").replace(":username", targetedHabbo.getHabboInfo().getUsername()));

        targetedHabbo.whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_corp_invite_received").replace(":corp",gameClient.getHabbo().getHabboRoleplayStats().getCorp().getGuild().getName()));

        return true;
    }
}
