package com.eu.habbo.habbohotel.commands.list;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.generic.alerts.MOTDNotificationComposer;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AboutCommand extends Command {
    public static String[] CONTRIBUTORS = {
            "TheGeneral",
            "Beny",
            "Alejandro",
            "Capheus",
            "Skeletor",
            "Harmonic",
            "Mike",
            "Remco",
            "zGrav",
            "Quadral",
            "Harmony",
            "Swirny",
            "ArpyAge",
            "Mikkel",
            "Rodolfo",
            "Rasmus",
            "Kitt Mustang",
            "Snaiker",
            "nttzx",
            "necmi",
            "Dome",
            "Jose Flores",
            "Cam",
            "Oliver",
            "Narzo",
            "Tenshie",
            "MartenM",
            "Ridge",
            "SenpaiDipper",
            "Thijmen"
    };

    public AboutCommand() {
        super("cmd_about");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Emulator.getRuntime().gc();

        StringBuilder message = new StringBuilder();

        message.append("<b>Roleplay Edition</b>\nBy LeChris\n\n");

        message.append("<b>Arcturus Contributors</b>\n");

        for (String contributor : AboutCommand.CONTRIBUTORS) {
            message.append(contributor + "\n");
        }

        gameClient.getHabbo().alert(message.toString());
        return true;
    }
}
