package com.eu.habbo.roleplay.commands.corp;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.corp.Corp;
import com.eu.habbo.roleplay.government.GovernmentManager;

public class CorpFireUserCommand extends Command {
    public CorpFireUserCommand() {
        super("cmd_corp_fire");
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
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found").replace(":username", targetedUsername));
            return true;
        }

        if (targetedHabbo == gameClient.getHabbo()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_fire_user_is_self"));
            return true;
        }

        if (!gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().isCanFire()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_fire_not_allowed"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().getOrderID() <= targetedHabbo.getHabboRoleplayStats().getCorpPosition().getOrderID()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_fire_not_allowed"));
            return true;
        }

        Corp welfareCorp = GovernmentManager.getInstance().getWelfareCorp();

        gameClient.getHabbo().getHabboRoleplayStats().setCorp(welfareCorp.getGuild().getId(), welfareCorp.getPositionByOrderID(1).getId());

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_fire_success").replace(":username", targetedHabbo.getHabboInfo().getUsername()));
        targetedHabbo.shout(Emulator.getTexts().getValue("commands.roleplay.cmd_fire_impacted"));

        return true;
    }
}