package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.rooms.chat.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.constants.RoomChatType;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserTalkEvent extends UserEvent {
    public final RoomChatMessage chatMessage;
    public final RoomChatType chatType;

    public UserTalkEvent(Habbo habbo, RoomChatMessage chatMessage, RoomChatType chatType) {
        super(habbo);
        this.chatMessage = chatMessage;
        this.chatType = chatType;
    }
}