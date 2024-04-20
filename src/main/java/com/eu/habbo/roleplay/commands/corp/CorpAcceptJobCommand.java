package com.eu.habbo.roleplay.commands.corp;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.roleplay.corp.Corp;
import com.eu.habbo.roleplay.corp.CorpManager;

public class CorpAcceptJobCommand extends Command {
    public CorpAcceptJobCommand() {
        super("cmd_corp_acceptjob");
    }
    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null) {
            return true;
        }

        String corpName = params[1];

        if (corpName == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.corp_not_found"));
            return true;
        }

        Corp targetedCorp = CorpManager.getInstance().getCorpsByName(corpName);

        if (targetedCorp == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.corp_not_found"));
            return true;
        }

        if (targetedCorp.getInvitedUser(gameClient.getHabbo()) == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_corp_invite_missing"));
            return true;
        }


        gameClient.getHabbo().getHabboRoleplayStats().setCorp(targetedCorp.getGuild().getId(), targetedCorp.getPositionByOrderID(1).getId());

        gameClient.getHabbo().getHabboRoleplayStats().getCorp().removeInvitedUser(gameClient.getHabbo());

        gameClient.getHabbo().shout(Emulator.getTexts().getValue("commands.roleplay.cmd_corp_invite_accepted").replace(":corp", targetedCorp.getGuild().getName()));

        return true;
    }
}
