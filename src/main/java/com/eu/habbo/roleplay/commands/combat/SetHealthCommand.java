package com.eu.habbo.roleplay.commands.combat;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;

public class SetHealthCommand extends Command {
    public SetHealthCommand() {
        super("cmd_sethealth");
    }
    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        String targetedUsername = params[1];

        if (targetedUsername == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found"));
            return true;
        }

        Habbo targetedHabbo = gameClient.getHabbo().getRoomUnit().getRoom().getRoomUnitManager().getRoomHabboByUsername(params[1]);

        if (targetedHabbo == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("generic.user_not_found"));
            return true;
        }

        int updatedHealth = Integer.parseInt(params[2]);

        String healthGivenMessage = Emulator.getTexts().getValue("roleplay.set_health_gave")
                .replace(":username", targetedUsername)
                .replace(":healthRestored", Integer.toString(updatedHealth));

        gameClient.getHabbo().shout(healthGivenMessage);

        targetedHabbo.getHabboRoleplayStats().setHealth(updatedHealth);

        targetedHabbo.shout(Emulator.getTexts().
                getValue("commands.roleplay.user_health_remaining")
                .replace(":currentHealth", Integer.toString(targetedHabbo.getHabboRoleplayStats().getHealthNow()))
                .replace(":maximumHealth", Integer.toString(targetedHabbo.getHabboRoleplayStats().getHealthMax()))
        );

        return true;
    }
}
