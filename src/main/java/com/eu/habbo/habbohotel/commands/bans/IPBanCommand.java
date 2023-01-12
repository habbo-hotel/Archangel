package com.eu.habbo.habbohotel.commands.bans;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.modtool.ModToolBanType;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IPBanCommand extends BaseBanCommand {


    public IPBanCommand() {
        super("cmd_ip_ban", Emulator.getTexts().getValue("commands.keys.cmd_ip_ban").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        int count = 0;
        if (habboInfo != null) {
            if (habboInfo == gameClient.getHabbo().getHabboInfo()) {
                gameClient.getHabbo().whisper(getTextsValue("commands.error.cmd_ip_ban.ban_self"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (habboInfo.getRank().getId() >= gameClient.getHabbo().getHabboInfo().getRank().getId()) {
                gameClient.getHabbo().whisper(getTextsValue("commands.error.cmd_ban.target_rank_higher"), RoomChatMessageBubbles.ALERT);
                return true;
            }

            Emulator.getGameEnvironment().getModToolManager().ban(habboInfo.getId(), gameClient.getHabbo(), reason, TEN_YEARS, ModToolBanType.IP, -1);
            count++;
            for (Habbo h : Emulator.getGameServer().getGameClientManager().getHabbosWithIP(habboInfo.getIpLogin())) {
                if (h != null) {
                    count++;
                    Emulator.getGameEnvironment().getModToolManager().ban(h.getHabboInfo().getId(), gameClient.getHabbo(), reason, TEN_YEARS, ModToolBanType.IP, -1);
                }
            }
        } else {
            gameClient.getHabbo().whisper(getTextsValue("commands.error.cmd_ban.user_offline"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        gameClient.getHabbo().whisper(getTextsValue("commands.succes.cmd_ip_ban").replace("%count%", count + ""), RoomChatMessageBubbles.ALERT);

        return true;
    }
}
