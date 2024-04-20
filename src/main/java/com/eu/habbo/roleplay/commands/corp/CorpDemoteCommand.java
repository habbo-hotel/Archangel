package com.eu.habbo.roleplay.commands.corp;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.corp.CorpPosition;

public class CorpDemoteCommand extends Command {
    public CorpDemoteCommand() {
        super("cmd_corp_demote");
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
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_user_is_self"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getCorp().getGuild().getId() != targetedHabbo.getHabboRoleplayStats().getCorp().getGuild().getId()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed"));
            return true;
        }

        if (!gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().isCanDemote()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().getOrderID() <= targetedHabbo.getHabboRoleplayStats().getCorpPosition().getOrderID()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed"));
            return true;
        }

        if (targetedHabbo.getHabboRoleplayStats().getCorpPosition().getOrderID() == 1) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed_too_low"));
            return true;
        }

        CorpPosition newPosition = targetedHabbo.getHabboRoleplayStats().getCorp().getPositionByOrderID(targetedHabbo.getHabboRoleplayStats().getCorpPosition().getOrderID() - 1);

        if (newPosition == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed_too_low"));
            return true;
        }

        if (gameClient.getHabbo().getHabboRoleplayStats().getCorpPosition().getOrderID() < newPosition.getOrderID()) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_not_allowed"));
            return true;
        }

        gameClient.getHabbo().getHabboRoleplayStats().setCorp(targetedHabbo.getHabboRoleplayStats().getCorp().getGuild().getId(), newPosition.getId());

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_demote_success")
                .replace(":username", targetedHabbo.getHabboInfo().getUsername())
                .replace(":corp", targetedHabbo.getHabboRoleplayStats().getCorp().getGuild().getName())
                .replace(":position", targetedHabbo.getHabboRoleplayStats().getCorpPosition().getName()));

        targetedHabbo.shout(Emulator.getTexts().getValue("generic.roleplay.started_new_job").
                replace(":corp", targetedHabbo.getHabboRoleplayStats().getCorp().getGuild().getName())
                .replace(":position", newPosition.getName()));

        return true;
    }
}