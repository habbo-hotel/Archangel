package com.eu.habbo.roleplay.messages.outgoing.combat;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import com.eu.habbo.roleplay.users.HabboWeapon;
import gnu.trove.map.hash.THashMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MyWeaponListComposer extends MessageComposer {
    private final Habbo habbo;

    @Override
    protected ServerMessage composeInternal() {
        THashMap<Integer, HabboWeapon> habboWeapons = this.habbo.getInventory().getWeaponsComponent().getWeapons();
        this.response.init(Outgoing.myWeaponListComposer);
        for (HabboWeapon weapon : habboWeapons.values()) {
            this.response.appendString(
                    weapon.getWeapon().getUniqueName()
                        + ";" + weapon.getWeapon().getDisplayName()
                        + ";" + weapon.getWeapon().getEquipEffect()
            );
        }
        return this.response;
    }
}
