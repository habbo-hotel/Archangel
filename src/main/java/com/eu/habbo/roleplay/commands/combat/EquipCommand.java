package com.eu.habbo.roleplay.commands.combat;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.roleplay.users.HabboWeapon;
import com.eu.habbo.roleplay.weapons.Weapon;
import com.eu.habbo.roleplay.weapons.WeaponsManager;


public class EquipCommand extends Command {
    public EquipCommand() {
        super("cmd_equip");
    }
    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params.length < 2) {
            Weapon prevEquippedWeapon = gameClient.getHabbo().getInventory().getWeaponsComponent().getEquippedWeapon().getWeapon();
            gameClient.getHabbo().getInventory().getWeaponsComponent().setEquippedWeapon(null);
            gameClient.getHabbo().getRoomUnit().giveEffect(0, -1);
            gameClient.getHabbo().shout(prevEquippedWeapon.getUnequipMessage().replace(":displayName", prevEquippedWeapon.getDisplayName()));
            return true;
        }

        String weaponUniqueName = params[1];

        if (weaponUniqueName == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_equip_no_weapon_specified"));
            return true;
        }

        HabboWeapon matchingWeapon = gameClient.getHabbo().getInventory().getWeaponsComponent().getWeaponByUniqueName(weaponUniqueName);
        if (matchingWeapon == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.roleplay.cmd_equip_weapon_not_found").replace(":uniqueName", weaponUniqueName));
            return true;
        }

        gameClient.getHabbo().getInventory().getWeaponsComponent().setEquippedWeapon(matchingWeapon);
        gameClient.getHabbo().getRoomUnit().giveEffect(WeaponsManager.getInstance().getWeaponByID(matchingWeapon.getWeaponID()).getEquipEffect(), -1);
        gameClient.getHabbo().shout(matchingWeapon.getWeapon().getEquipMessage().replace(":displayName", matchingWeapon.getWeapon().getDisplayName()));

        return true;
    }
}
