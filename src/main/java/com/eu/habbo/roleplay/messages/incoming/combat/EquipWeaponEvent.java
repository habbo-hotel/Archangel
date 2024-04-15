package com.eu.habbo.roleplay.messages.incoming.combat;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.roleplay.commands.combat.EquipCommand;

public class EquipWeaponEvent extends MessageHandler {
    @Override
    public void handle() {
        String weaponUniqueName = this.packet.readString();

        if (weaponUniqueName == null) {
            return;
        }

        new EquipCommand().handle(this.client, new String[] {null, weaponUniqueName});
    }
}