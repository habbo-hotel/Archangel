package com.eu.habbo.habbohotel;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.CreditsScheduler;
import com.eu.habbo.core.GotwPointsScheduler;
import com.eu.habbo.core.PixelScheduler;
import com.eu.habbo.core.PointsScheduler;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.bots.BotManager;
import com.eu.habbo.habbohotel.campaign.CalendarManager;
import com.eu.habbo.habbohotel.catalog.CatalogManager;
import com.eu.habbo.habbohotel.commands.CommandsManager;
import com.eu.habbo.habbohotel.crafting.CraftingManager;
import com.eu.habbo.habbohotel.guides.GuideManager;
import com.eu.habbo.habbohotel.guilds.GuildManager;
import com.eu.habbo.habbohotel.hotelview.HotelViewManager;
import com.eu.habbo.habbohotel.items.ItemManager;
import com.eu.habbo.habbohotel.modtool.ModToolManager;
import com.eu.habbo.habbohotel.modtool.ModToolSanctions;
import com.eu.habbo.habbohotel.modtool.WordFilter;
import com.eu.habbo.habbohotel.navigation.NavigatorManager;
import com.eu.habbo.habbohotel.permissions.PermissionsManager;
import com.eu.habbo.habbohotel.pets.PetManager;
import com.eu.habbo.habbohotel.polls.PollManager;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionManager;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionScheduler;
import com.eu.habbo.roleplay.game.TimeOfDayManager;
import com.eu.habbo.roleplay.corp.CorpManager;
import com.eu.habbo.roleplay.weapons.WeaponsManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class GameEnvironment {
    private CreditsScheduler creditsScheduler;
    private PixelScheduler pixelScheduler;
    private PointsScheduler pointsScheduler;
    private GotwPointsScheduler gotwPointsScheduler;
    private SubscriptionScheduler subscriptionScheduler;
    private HabboManager habboManager;
    private NavigatorManager navigatorManager;
    private GuildManager guildManager;
    private ItemManager itemManager;
    private CatalogManager catalogManager;
    private HotelViewManager hotelViewManager;
    private RoomManager roomManager;
    private CommandsManager commandsManager;
    private PermissionsManager permissionsManager;
    private BotManager botManager;
    private ModToolManager modToolManager;
    private ModToolSanctions modToolSanctions;
    private PetManager petManager;
    private AchievementManager achievementManager;
    private GuideManager guideManager;
    private WordFilter wordFilter;
    private CraftingManager craftingManager;
    private PollManager pollManager;
    private SubscriptionManager subscriptionManager;
    private CalendarManager calendarManager;
    private CorpManager corpManager;
    private WeaponsManager weaponsManager;
    private TimeOfDayManager timeOfDayManager;


    public void load() throws Exception {
        log.info("GameEnvironment -> Loading...");

        this.permissionsManager = new PermissionsManager();
        this.habboManager = new HabboManager();
        this.hotelViewManager = new HotelViewManager();
        this.itemManager = new ItemManager();
        this.itemManager.load();
        this.botManager = new BotManager();
        this.petManager = new PetManager();
        this.guildManager = new GuildManager();
        this.catalogManager = new CatalogManager();
        this.roomManager = new RoomManager();
        this.navigatorManager = new NavigatorManager();
        this.commandsManager = new CommandsManager();
        this.modToolManager = new ModToolManager();
        this.modToolSanctions = new ModToolSanctions();
        this.achievementManager = new AchievementManager();
        this.achievementManager.reload();
        this.guideManager = new GuideManager();
        this.wordFilter = new WordFilter();
        this.craftingManager = new CraftingManager();
        this.pollManager = new PollManager();
        this.calendarManager = new CalendarManager();
        this.corpManager = CorpManager.getInstance();
        this.weaponsManager = WeaponsManager.getInstance();
        this.timeOfDayManager = TimeOfDayManager.getInstance();

        this.roomManager.loadAllRooms();
        this.navigatorManager.loadNavigator();

        this.creditsScheduler = new CreditsScheduler();
        Emulator.getThreading().run(this.creditsScheduler);
        this.pixelScheduler = new PixelScheduler();
        Emulator.getThreading().run(this.pixelScheduler);
        this.pointsScheduler = new PointsScheduler();
        Emulator.getThreading().run(this.pointsScheduler);
        this.gotwPointsScheduler = new GotwPointsScheduler();
        Emulator.getThreading().run(this.gotwPointsScheduler);

        this.subscriptionManager = new SubscriptionManager();
        this.subscriptionManager.init();

        this.subscriptionScheduler = new SubscriptionScheduler();
        Emulator.getThreading().run(this.subscriptionScheduler);

        log.info("GameEnvironment -> Loaded!");
    }

    public void dispose() {
        this.pointsScheduler.setDisposed(true);
        this.pixelScheduler.setDisposed(true);
        this.creditsScheduler.setDisposed(true);
        this.gotwPointsScheduler.setDisposed(true);
        this.craftingManager.dispose();
        this.habboManager.dispose();
        this.commandsManager.dispose();
        this.guildManager.dispose();
        this.catalogManager.dispose();
        this.roomManager.dispose();
        this.itemManager.dispose();
        this.hotelViewManager.dispose();
        this.subscriptionManager.dispose();
        this.calendarManager.dispose();
        log.info("GameEnvironment -> Disposed!");
    }

}
