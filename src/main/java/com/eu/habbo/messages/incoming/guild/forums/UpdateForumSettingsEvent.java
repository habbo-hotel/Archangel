package com.eu.habbo.messages.incoming.guild.forums;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.SettingsState;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.NotificationDialogMessageComposer;
import com.eu.habbo.messages.outgoing.guild.forums.ForumDataMessageComposer;
import com.eu.habbo.messages.outgoing.handshake.ErrorReportComposer;

public class UpdateForumSettingsEvent extends MessageHandler {
    @Override
    public void handle() {
        int guildId = packet.readInt();
        int canRead = packet.readInt();
        int postMessages = packet.readInt();
        int postThreads = packet.readInt();
        int modForum = packet.readInt();

        Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(guildId);

        if (guild == null) {
            this.client.sendResponse(new ErrorReportComposer(404));
            return;
        }

        if (guild.getOwnerId() != this.client.getHabbo().getHabboInfo().getId()) {
            this.client.sendResponse(new ErrorReportComposer(403));
            return;
        }

        guild.setReadForum(SettingsState.fromValue(canRead));
        guild.setPostMessages(SettingsState.fromValue(postMessages));
        guild.setPostThreads(SettingsState.fromValue(postThreads));
        guild.setModForum(SettingsState.fromValue(modForum));

        guild.needsUpdate = true;

        Emulator.getThreading().run(guild);

        this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.FORUMS_FORUM_SETTINGS_UPDATED.getKey()).compose());

        this.client.sendResponse(new ForumDataMessageComposer(guild, this.client.getHabbo()));
    }
}