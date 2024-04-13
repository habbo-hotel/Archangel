package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.campaign.CalendarCampaign;
import com.eu.habbo.habbohotel.catalog.TargetOffer;
import com.eu.habbo.habbohotel.messenger.Messenger;
import com.eu.habbo.habbohotel.modtool.ModToolSanctionItem;
import com.eu.habbo.habbohotel.modtool.ModToolSanctions;
import com.eu.habbo.habbohotel.navigation.NavigatorSavedSearch;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.habbohotel.users.clothingvalidation.ClothingValidationManager;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionHabboClub;
import com.eu.habbo.messages.NoAuthMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.TargetedOfferComposer;
import com.eu.habbo.messages.outgoing.events.calendar.CampaignCalendarDataMessageComposer;
import com.eu.habbo.messages.outgoing.gamecenter.Game2AccountGameStatusMessageComposer;
import com.eu.habbo.messages.outgoing.gamecenter.GameListMessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.HabboBroadcastMessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.MOTDNotificationComposer;
import com.eu.habbo.messages.outgoing.habboway.nux.InClientLinkMessageComposer;
import com.eu.habbo.messages.outgoing.habboway.nux.NoobnessLevelMessageComposer;
import com.eu.habbo.messages.outgoing.handshake.*;
import com.eu.habbo.messages.outgoing.inventory.AvatarEffectsMessageComposer;
import com.eu.habbo.messages.outgoing.inventory.BadgePointLimitsComposer;
import com.eu.habbo.messages.outgoing.modtool.CfhTopicsInitComposer;
import com.eu.habbo.messages.outgoing.modtool.ModeratorInitMessageComposer;
import com.eu.habbo.messages.outgoing.modtool.SanctionStatusComposer;
import com.eu.habbo.messages.outgoing.mysterybox.MysteryBoxKeysMessageComposer;
import com.eu.habbo.messages.outgoing.navigator.NavigatorSavedSearchesComposer;
import com.eu.habbo.messages.outgoing.unknown.BuildersClubSubscriptionStatusMessageComposer;
import com.eu.habbo.messages.outgoing.users.*;
import com.eu.habbo.plugin.events.emulator.SSOAuthenticationEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;
import com.eu.habbo.roleplay.users.HabboRoleplayStats;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

@NoAuthMessage
public class SSOTicketEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSOTicketEvent.class);


    @Override
    public void handle() throws Exception {
        if (!this.client.getChannel().isOpen()) {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
            return;
        }

        if (!Emulator.isReady)
            return;

        if (Emulator.getConfig().getBoolean("encryption.forced", false) && Emulator.getCrypto().isEnabled() && !this.client.isHandshakeFinished()) {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
            LOGGER.warn("Encryption is forced and TLS Handshake isn't finished! Closed connection...");
            return;
        }

        String sso = this.packet.readString().replace(" ", "");

        if (Emulator.getPluginManager().fireEvent(new SSOAuthenticationEvent(sso)).isCancelled()) {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
            LOGGER.info("SSO Authentication is cancelled by a plugin. Closed connection...");
            return;
        }

        if (sso.isEmpty()) {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
            LOGGER.debug("Client is trying to connect without SSO ticket! Closed connection...");
            return;
        }

        if (this.client.getHabbo() == null) {
            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().loadHabbo(sso);
            if (habbo != null) {
                try {
                    habbo.setClient(this.client);
                    this.client.setHabbo(habbo);
                    if (!this.client.getHabbo().connect()) {
                        Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
                        return;
                    }

                    if (this.client.getHabbo().getHabboInfo() == null) {
                        Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
                        return;
                    }

                    if (this.client.getHabbo().getHabboInfo().getPermissionGroup() == null) {
                        throw new NullPointerException(habbo.getHabboInfo().getUsername() + " has a NON EXISTING RANK!");
                    }

                    Emulator.getThreading().run(habbo);
                    Emulator.getGameEnvironment().getHabboManager().addHabbo(habbo);
                } catch (Exception e) {
                    LOGGER.error("Caught exception", e);
                    Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
                    return;
                }

                if (ClothingValidationManager.VALIDATE_ON_LOGIN) {
                    String validated = ClothingValidationManager.validateLook(this.client.getHabbo());
                    if (!validated.equals(this.client.getHabbo().getHabboInfo().getLook())) {
                        this.client.getHabbo().getHabboInfo().setLook(validated);
                    }
                }

                ArrayList<ServerMessage> messages = new ArrayList<>();

                messages.add(new AuthenticationOKMessageComposer().compose());

                int roomIdToEnter = 0;

                if (!this.client.getHabbo().getHabboStats().isNux() || Emulator.getConfig().getBoolean("retro.style.homeroom") && this.client.getHabbo().getHabboInfo().getHomeRoom() != 0)
                    roomIdToEnter = this.client.getHabbo().getHabboInfo().getHomeRoom();
                else if (!this.client.getHabbo().getHabboStats().isNux() || Emulator.getConfig().getBoolean("retro.style.homeroom") && RoomManager.HOME_ROOM_ID > 0)
                    roomIdToEnter = RoomManager.HOME_ROOM_ID;

                boolean calendar = false;
                if (!this.client.getHabbo().getHabboStats().getAchievementProgress().containsKey(Emulator.getGameEnvironment().getAchievementManager().getAchievement("Login"))) {
                    AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("Login"));
                    calendar = true;
                } else {
                    int previousOnline = (int) this.client.getHabbo().getHabboStats().getCache().get("previousOnline");
                    long daysBetween = ChronoUnit.DAYS.between(new Date((long) previousOnline * 1000L).toInstant(), new Date().toInstant());

                    Date lastLogin = new Date(previousOnline);
                    Calendar c1 = Calendar.getInstance();
                    c1.add(Calendar.DAY_OF_YEAR, -1);

                    Calendar c2 = Calendar.getInstance();
                    c2.setTime(lastLogin);

                    if (daysBetween == 1) {
                        if (this.client.getHabbo().getHabboStats().getAchievementProgress().get(Emulator.getGameEnvironment().getAchievementManager().getAchievement("Login")) == this.client.getHabbo().getHabboStats().getLoginStreak()) {
                            AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("Login"));
                        }
                        this.client.getHabbo().getHabboStats().setLoginStreak(client.getHabbo().getHabboStats().getLoginStreak()+1);
                        calendar = true;
                    } else if (daysBetween >= 1) {
                        calendar = true;
                    } else {
                        if (((lastLogin.getTime() / 1000) - Emulator.getIntUnixTimestamp()) > 86400) {
                            this.client.getHabbo().getHabboStats().setLoginStreak(0);
                        }
                    }
                }

                if (!this.client.getHabbo().getHabboStats().getAchievementProgress().containsKey(Emulator.getGameEnvironment().getAchievementManager().getAchievement("RegistrationDuration"))) {
                    AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("RegistrationDuration"), 0);
                } else {
                    int daysRegistered = ((Emulator.getIntUnixTimestamp() - this.client.getHabbo().getHabboInfo().getAccountCreated()) / 86400);

                    int days = this.client.getHabbo().getHabboStats().getAchievementProgress(
                            Emulator.getGameEnvironment().getAchievementManager().getAchievement("RegistrationDuration")
                    );

                    if (daysRegistered - days > 0) {
                        AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("RegistrationDuration"), daysRegistered - days);
                    }
                }

                if (!this.client.getHabbo().getHabboStats().getAchievementProgress().containsKey(Emulator.getGameEnvironment().getAchievementManager().getAchievement("TraderPass"))) {
                    AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("TraderPass"));
                }


                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement achievementQueueStatement = connection.prepareStatement("SELECT * FROM users_achievements_queue WHERE user_id = ?")) {
                    achievementQueueStatement.setInt(1, this.client.getHabbo().getHabboInfo().getId());

                    try (ResultSet achievementSet = achievementQueueStatement.executeQuery()) {
                        while (achievementSet.next()) {
                            AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement(achievementSet.getInt("achievement_id")), achievementSet.getInt("amount"));
                        }
                    }

                    try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM users_achievements_queue WHERE user_id = ?")) {
                        deleteStatement.setInt(1, this.client.getHabbo().getHabboInfo().getId());
                        deleteStatement.execute();
                    }
                }

                messages.add(new NavigatorSettingsComposer(this.client.getHabbo().getHabboInfo().getHomeRoom(), roomIdToEnter).compose());
                messages.add(new AvatarEffectsMessageComposer(habbo, this.client.getHabbo().getInventory().getEffectsComponent().getEffects().values()).compose());
                messages.add(new FigureSetIdsComposer(this.client.getHabbo()).compose());
                messages.add(new NoobnessLevelMessageComposer(habbo).compose());
                messages.add(new UserRightsMessageComposer(this.client.getHabbo()).compose());
                messages.add(new AvailabilityStatusMessageComposer(true, false, true).compose());
                messages.add(new PingMessageComposer().compose());
                messages.add(new EnableNotificationsComposer(Emulator.getConfig().getBoolean("bubblealerts.enabled", true)).compose());
                messages.add(new AchievementsScoreComposer(this.client.getHabbo()).compose());
                messages.add(new IsFirstLoginOfDayComposer(true).compose());
                messages.add(new MysteryBoxKeysMessageComposer().compose());
                messages.add(new BuildersClubSubscriptionStatusMessageComposer().compose());
                messages.add(new CfhTopicsInitComposer().compose());
                messages.add(new FavouriteChangedComposer(this.client.getHabbo()).compose());
                messages.add(new GameListMessageComposer().compose());
                messages.add(new Game2AccountGameStatusMessageComposer(3, 100).compose());
                messages.add(new Game2AccountGameStatusMessageComposer(0, 100).compose());

                messages.add(new ScrSendUserInfoComposer(this.client.getHabbo(), SubscriptionHabboClub.HABBO_CLUB, ScrSendUserInfoComposer.RESPONSE_TYPE_LOGIN).compose());

                if (this.client.getHabbo().hasPermissionRight(Permission.ACC_SUPPORTTOOL)) {
                    messages.add(new ModeratorInitMessageComposer(this.client.getHabbo()).compose());
                }


                CalendarCampaign campaign = Emulator.getGameEnvironment().getCalendarManager().getCalendarCampaign(Emulator.getConfig().getValue("hotel.calendar.default"));
                if (campaign != null) {
                    long daysBetween = DAYS.between(new Timestamp(campaign.getStartTimestamp() * 1000L).toInstant(), new Date().toInstant());
                    if (daysBetween >= 0) {
                        messages.add(new CampaignCalendarDataMessageComposer(campaign.getName(), campaign.getImage(), campaign.getTotalDays(), (int) daysBetween, this.client.getHabbo().getHabboStats().getCalendarRewardsClaimed(), campaign.getLockExpired()).compose());
                        if(Emulator.getConfig().getBoolean("hotel.login.show.calendar", false)) {
                            messages.add(new InClientLinkMessageComposer("openView/calendar").compose());
                        }
                    }
                }

                if (TargetOffer.ACTIVE_TARGET_OFFER_ID > 0) {
                    TargetOffer offer = Emulator.getGameEnvironment().getCatalogManager().getTargetOffer(TargetOffer.ACTIVE_TARGET_OFFER_ID);

                    if (offer != null) {
                        messages.add(new TargetedOfferComposer(this.client.getHabbo(), offer).compose());
                    }
                }

                this.client.sendResponses(messages);

                //Hardcoded
                //this.client.sendResponse(new ForumsTestComposer());
                this.client.sendResponse(new BadgePointLimitsComposer());

                ModToolSanctions modToolSanctions = Emulator.getGameEnvironment().getModToolSanctions();

                if (Emulator.getConfig().getBoolean("hotel.sanctions.enabled")) {
                    THashMap<Integer, ArrayList<ModToolSanctionItem>> modToolSanctionItemsHashMap = Emulator.getGameEnvironment().getModToolSanctions().getSanctions(habbo.getHabboInfo().getId());
                    ArrayList<ModToolSanctionItem> modToolSanctionItems = modToolSanctionItemsHashMap.get(habbo.getHabboInfo().getId());

                    if (modToolSanctionItems != null && modToolSanctionItems.size() > 0) {
                        ModToolSanctionItem item = modToolSanctionItems.get(modToolSanctionItems.size() - 1);

                        if (item.getSanctionLevel() > 0 && item.getProbationTimestamp() != 0 && item.getProbationTimestamp() > Emulator.getIntUnixTimestamp()) {
                            this.client.sendResponse(new SanctionStatusComposer(this.client.getHabbo()));
                        } else if (item.getSanctionLevel() > 0 && item.getProbationTimestamp() != 0 && item.getProbationTimestamp() <= Emulator.getIntUnixTimestamp()) {
                            modToolSanctions.updateSanction(item.getId(), 0);
                        }

                        if (item.getTradeLockedUntil() > 0 && item.getTradeLockedUntil() <= Emulator.getIntUnixTimestamp()) {
                            modToolSanctions.updateTradeLockedUntil(item.getId(), 0);
                            habbo.getHabboStats().setAllowTrade(true);
                        } else if (item.getTradeLockedUntil() > 0 && item.getTradeLockedUntil() > Emulator.getIntUnixTimestamp()) {
                            habbo.getHabboStats().setAllowTrade(false);
                        }

                        if (item.isMuted() && item.getMuteDuration() <= Emulator.getIntUnixTimestamp()) {
                            modToolSanctions.updateMuteDuration(item.getId(), 0);
                            habbo.unMute();
                        } else if (item.isMuted() && item.getMuteDuration() > Emulator.getIntUnixTimestamp()) {
                            Date muteDuration = new Date((long) item.getMuteDuration() * 1000);
                            long diff = muteDuration.getTime() - Emulator.getDate().getTime();
                            habbo.mute(Math.toIntExact(diff), false);
                        }
                    }
                }

                Emulator.getPluginManager().fireEvent(new UserLoginEvent(habbo, this.client.getHabbo().getHabboInfo().getIpLogin()));

                if (Emulator.getConfig().getBoolean("hotel.welcome.alert.enabled")) {
                    final Habbo finalHabbo = habbo;
                    Emulator.getThreading().run(() -> {
                        if (Emulator.getConfig().getBoolean("hotel.welcome.alert.oldstyle")) {
                            SSOTicketEvent.this.client.sendResponse(new MOTDNotificationComposer(HabboManager.WELCOME_MESSAGE.replace("%username%", finalHabbo.getHabboInfo().getUsername()).replace("%user%", finalHabbo.getHabboInfo().getUsername()).split("<br/>")));
                        } else {
                            SSOTicketEvent.this.client.sendResponse(new HabboBroadcastMessageComposer(HabboManager.WELCOME_MESSAGE.replace("%username%", finalHabbo.getHabboInfo().getUsername()).replace("%user%", finalHabbo.getHabboInfo().getUsername())));
                        }

                    }, Emulator.getConfig().getInt("hotel.welcome.alert.delay", 5000));
                }

                Emulator.getThreading().run(() -> {
                    while (this.client.getHabbo().getRoomUnit().getRoom()== null) {
                        try {
                            Thread.sleep(100); // Adjust the sleep time as needed
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    HabboRoleplayStats rpStats = this.client.getHabbo().getHabboRoleplayStats();
                    RoomTile lastUserPos = this.client.getHabbo().getRoomUnit().getRoom().getLayout().getTile(rpStats.getLastPosX(),rpStats.getLastPosY());
                    this.client.getHabbo().getRoomUnit().setLocation(lastUserPos);
                }, 250);

                if (SubscriptionHabboClub.HC_PAYDAY_ENABLED) {
                    SubscriptionHabboClub.processUnclaimed(habbo);
                }

                SubscriptionHabboClub.processClubBadge(habbo);

                Messenger.checkFriendSizeProgress(habbo);

                if (!habbo.getHabboStats().isHasGottenDefaultSavedSearches()) {
                    habbo.getHabboStats().setHasGottenDefaultSavedSearches(true);
                    Emulator.getThreading().run(habbo.getHabboStats());

                    habbo.getHabboInfo().addSavedSearch(new NavigatorSavedSearch("official-root", ""));
                    habbo.getHabboInfo().addSavedSearch(new NavigatorSavedSearch("my", ""));
                    habbo.getHabboInfo().addSavedSearch(new NavigatorSavedSearch("favorites", ""));

                    this.client.sendResponse(new NavigatorSavedSearchesComposer(this.client.getHabbo().getHabboInfo().getSavedSearches()));
                }

            } else {
                Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
                LOGGER.warn("Someone tried to login with a non-existing SSO token! Closed connection...");
            }
        } else {
            Emulator.getGameServer().getGameClientManager().disposeClient(this.client);
        }
    }
}
