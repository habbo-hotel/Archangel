package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.WiredValidationErrorComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSavedComposer;

public class UpdateTriggerEvent extends MessageHandler {
    @Override
    public void handle() {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasRight(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasRight(Permission.ACC_MOVEROTATE)) {
                InteractionWiredTrigger trigger = room.getRoomSpecialTypes().getTrigger(itemId);

                try {
                    if (trigger == null) {
                        throw new WiredSaveException(String.format("Wired trigger with item id %s not found in room", itemId));
                    }

                    trigger.saveWiredSettings(this.packet, room);
                    this.client.sendResponse(new WiredSavedComposer());
                    trigger.needsUpdate(true);
                    Emulator.getThreading().run(trigger);
                }
                catch (WiredSaveException e) {
                    this.client.sendResponse(new WiredValidationErrorComposer(e.getMessage()));
                }
            }
        }
    }
}
