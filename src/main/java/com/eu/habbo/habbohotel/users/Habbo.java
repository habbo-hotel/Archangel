package com.eu.habbo.habbohotel.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.catalog.ClothItem;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.messenger.Messenger;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.chat.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.constants.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.constants.RoomUserAction;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomHabbo;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.units.type.Avatar;
import com.eu.habbo.habbohotel.users.inventory.BadgesComponent;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.generic.alerts.*;
import com.eu.habbo.messages.outgoing.inventory.*;
import com.eu.habbo.messages.outgoing.rooms.FloodControlMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.RoomForwardMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.users.*;
import com.eu.habbo.messages.outgoing.users.*;
import com.eu.habbo.plugin.events.users.UserCreditsEvent;
import com.eu.habbo.plugin.events.users.UserDisconnectEvent;
import com.eu.habbo.plugin.events.users.UserGetIPAddressEvent;
import com.eu.habbo.plugin.events.users.UserPointsEvent;
import com.eu.habbo.roleplay.users.HabboRoleplayStats;
import gnu.trove.TIntCollection;
import gnu.trove.map.hash.THashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Habbo extends Avatar implements Runnable {

    @Getter
    private RoomHabbo roomUnit;
    @Getter
    private final HabboInfo habboInfo;
    @Getter
    private final HabboStats habboStats;
    @Getter
    private final Messenger messenger;
    @Getter
    private final HabboInventory inventory;
    @Setter
    @Getter
    private GameClient client;

    @Setter
    @Getter
    private final HabboRoleplayStats habboRoleplayStats;

    private volatile boolean update;
    private volatile boolean disconnected = false;
    private volatile boolean disconnecting = false;

    public Habbo(ResultSet set) throws SQLException {
        this.client = null;
        this.habboInfo = new HabboInfo(set);
        this.habboStats = HabboStats.load(this.habboInfo);
        this.habboRoleplayStats = HabboRoleplayStats.load(this);
        this.inventory = new HabboInventory(this);

        this.messenger = new Messenger();
        this.messenger.loadFriends(this);
        this.messenger.loadFriendRequests(this);

        this.roomUnit = new RoomHabbo();
        this.roomUnit.setUnit(this);

        this.update = false;
    }

    public boolean isOnline() {
        return this.habboInfo.isOnline();
    }

    void isOnline(boolean value) {
        this.habboInfo.setOnline(value);
        this.update();
    }

    void update() {
        this.update = true;
        this.run();
    }

    void needsUpdate(boolean value) {
        this.update = value;
    }

    boolean needsUpdate() {
        return this.update;
    }


    public boolean connect() {
        String ip = "";

        if (!Emulator.getConfig().getBoolean("networking.tcp.proxy") && this.client.getChannel().remoteAddress() != null) {
            SocketAddress address = this.client.getChannel().remoteAddress();
            ip = ((InetSocketAddress) address).getAddress().getHostAddress();
        }

        if (Emulator.getPluginManager().isRegistered(UserGetIPAddressEvent.class, true)) {
            UserGetIPAddressEvent event = Emulator.getPluginManager().fireEvent(new UserGetIPAddressEvent(this, ip));
            if (event.hasChangedIP()) {
                ip = event.getUpdatedIp();
            }
        }

        if (!ip.isEmpty()) {
            this.habboInfo.setIpLogin(ip);
        }

        if (Emulator.getGameEnvironment().getModToolManager().hasMACBan(this.client)) {
            return false;
        }

        if (Emulator.getGameEnvironment().getModToolManager().hasIPBan(this.habboInfo.getIpLogin())) {
            return false;
        }

        this.habboInfo.setMachineID(this.client.getMachineId());
        this.isOnline(true);
        this.habboStats.getCache().put("previousOnline", this.habboInfo.getLastOnline());
        this.habboInfo.setLastOnline(Emulator.getIntUnixTimestamp());

        this.messenger.connectionChanged(this, true, false);

        Emulator.getGameEnvironment().getRoomManager().loadRoomsForHabbo(this);

        log.info("{} logged in from IP {}", this.habboInfo.getUsername(), this.habboInfo.getIpLogin());
        return true;
    }


    public synchronized void disconnect() {
        if (!Emulator.isShuttingDown) {
            if (Emulator.getPluginManager().fireEvent(new UserDisconnectEvent(this)).isCancelled()) return;
        }

        if (this.disconnected || this.disconnecting) {
            return;
        }

        int logoutDelay = Emulator.getConfig().getInt("roleplay.logout.delay", 10000);
        log.info("{} logging out in " + (logoutDelay/1000) + " seconds", this.habboInfo.getUsername());
        this.shout(Emulator.getTexts().getValue("roleplay.generic.logout_started").replace(":seconds", String.valueOf(logoutDelay / 1000)));

        RoomTile currPos = this.getRoomUnit().getCurrentPosition();
        this.habboRoleplayStats.setLastPos(currPos.getX(), currPos.getY());

        Emulator.getThreading().run(() -> {
            this.disconnecting = true;

            try {
                if (this.getRoomUnit().getRoom() != null) {
                    Emulator.getGameEnvironment().getRoomManager().leaveRoom(this, this.getRoomUnit().getRoom());
                }
                if (this.getHabboInfo().getRoomQueueId() > 0) {
                    Room room = Emulator.getGameEnvironment().getRoomManager().getActiveRoomById(this.getHabboInfo().getRoomQueueId());

                    if (room != null) {
                        room.removeFromQueue(this);
                    }
                }
            } catch (Exception e) {
                log.error("Caught exception", e);
            }

            try {
                Emulator.getGameEnvironment().getGuideManager().userLogsOut(this);
                this.isOnline(false);
                this.needsUpdate(true);
                this.run();
                this.getInventory().dispose();
                this.messenger.connectionChanged(this, false, false);
                this.messenger.dispose();
                this.disconnected = true;
                AchievementManager.saveAchievements(this);

                this.habboStats.dispose();
                this.habboRoleplayStats.dispose();
            } catch (Exception e) {
                log.error("Caught exception", e);
                return;
            } finally {
                Emulator.getGameEnvironment().getRoomManager().unloadRoomsForHabbo(this);
                Emulator.getGameEnvironment().getHabboManager().removeHabbo(this);
            }
            log.info("{} disconnected.", this.habboInfo.getUsername());
            this.client = null;
        },  logoutDelay);
    }

    @Override
    public void run() {
        if (this.needsUpdate()) {
            this.habboInfo.run();
            this.needsUpdate(false);
        }
    }

    public boolean canExecuteCommand(String key) {
        return this.canExecuteCommand(key, false);
    }

    public boolean canExecuteCommand(String key, boolean hasRoomRights) {
        return this.getHabboInfo().getPermissionGroup().canExecuteCommand(key, hasRoomRights);
    }

    public boolean hasPermissionRight(String key) {
        return this.hasPermissionRight(key, false);
    }

    public boolean hasPermissionRight(String key, boolean hasRoomRights) {
        return this.getHabboInfo().getPermissionGroup().hasPermissionRight(key, hasRoomRights);
    }


    public void giveCredits(int credits) {
        if (credits == 0)
            return;

        UserCreditsEvent event = new UserCreditsEvent(this, credits);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled())
            return;

        this.getHabboInfo().addCredits(event.getCredits());

        if (this.client != null) this.client.sendResponse(new CreditBalanceComposer(this.client.getHabbo()));
    }


    public void givePixels(int pixels) {
        if (pixels == 0)
            return;


        UserPointsEvent event = new UserPointsEvent(this, pixels, 0);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled())
            return;

        this.getHabboInfo().addPixels(event.getPoints());
        if (this.client != null) this.client.sendResponse(new ActivityPointsMessageComposer(this.client.getHabbo()));
    }


    public void givePoints(int points) {
        this.givePoints(Emulator.getConfig().getInt("seasonal.primary.type"), points);
    }


    public void givePoints(int type, int points) {
        if (points == 0)
            return;

        UserPointsEvent event = new UserPointsEvent(this, points, type);
        if (Emulator.getPluginManager().fireEvent(event).isCancelled())
            return;

        this.getHabboInfo().addCurrencyAmount(event.getType(), event.getPoints());
        if (this.client != null)
            this.client.sendResponse(new HabboActivityPointNotificationMessageComposer(this.client.getHabbo().getHabboInfo().getCurrencyAmount(type), event.getPoints(), event.getType()));
    }


    public void whisper(String message) {
        this.whisper(message, this.habboStats.getChatColor());
    }


    public void whisper(String message, RoomChatMessageBubbles bubble) {
        if (this.getRoomUnit().isInRoom()) {
            this.client.sendResponse(new WhisperMessageComposer(new RoomChatMessage(message, this.client.getHabbo().getRoomUnit(), bubble)));
        }
    }


    public void talk(String message) {
        this.talk(message, this.habboStats.getChatColor());
    }


    public void talk(String message, RoomChatMessageBubbles bubble) {
        if (this.getRoomUnit().isInRoom()) {
            this.getRoomUnit().getRoom().sendComposer(new ChatMessageComposer(new RoomChatMessage(message, this.client.getHabbo().getRoomUnit(), bubble)).compose());
        }
    }


    public void shout(String message) {
        this.shout(message, this.habboStats.getChatColor());
    }


    public void shout(String message, RoomChatMessageBubbles bubble) {
        if (this.getRoomUnit().isInRoom()) {
            this.getRoomUnit().getRoom().sendComposer(new ShoutMessageComposer(new RoomChatMessage(message, this.client.getHabbo().getRoomUnit(), bubble)).compose());
        }
    }


    public void alert(String message) {
        if (Emulator.getConfig().getBoolean("hotel.alert.oldstyle")) {
            this.client.sendResponse(new MOTDNotificationComposer(new String[]{message}));
        } else {
            this.client.sendResponse(new HabboBroadcastMessageComposer(message));
        }
    }


    public void alert(String[] messages) {
        this.client.sendResponse(new MOTDNotificationComposer(messages));
    }


    public void alertWithUrl(String message, String url) {
        this.client.sendResponse(new ModeratorMessageComposer(message, url));
    }


    public void goToRoom(int id) {
        this.client.sendResponse(new RoomForwardMessageComposer(id));
    }


    public void addFurniture(RoomItem item) {
        this.inventory.getItemsComponent().addItem(item);
        this.client.sendResponse(new UnseenItemsComposer(item));
        this.client.sendResponse(new FurniListInvalidateComposer());
    }


    public void removeFurniture(RoomItem item) {
        this.inventory.getItemsComponent().removeHabboItem(item);
        this.client.sendResponse(new FurniListRemoveComposer(item.getId()));
    }


    public void addBot(Bot bot) {
        this.inventory.getBotsComponent().addBot(bot);
        this.client.sendResponse(new BotAddedToInventoryComposer(bot));
    }


    public void removeBot(Bot bot) {
        this.inventory.getBotsComponent().removeBot(bot);
        this.client.sendResponse(new BotRemovedFromInventoryComposer(bot));
    }


    public void deleteBot(Bot bot) {
        this.removeBot(bot);
        bot.getRoomUnit().getRoom().getRoomUnitManager().getRoomBotManager().removeBot(bot);
        Emulator.getGameEnvironment().getBotManager().deleteBot(bot);
    }


    public void addPet(Pet pet) {
        this.inventory.getPetsComponent().addPet(pet);
        this.client.sendResponse(new PetAddedToInventoryComposer(pet));
    }


    public void removePet(Pet pet) {
        this.inventory.getPetsComponent().removePet(pet);
        this.client.sendResponse(new PetRemovedFromInventoryComposer(pet));
    }


    public boolean addBadge(String code) {
        if (!this.inventory.getBadgesComponent().hasBadge(code)) {
            HabboBadge badge = BadgesComponent.createBadge(code, this);
            this.inventory.getBadgesComponent().addBadge(badge);
            this.client.sendResponse(new BadgeReceivedComposer(badge));
            this.client.sendResponse(new UnseenItemsComposer(badge.getId(), UnseenItemsComposer.AddHabboItemCategory.BADGE));

            THashMap<String, String> keys = new THashMap<>();
            keys.put("display", "BUBBLE");
            keys.put("image", "${image.library.url}album1584/" + badge.getCode() + ".gif");
            keys.put("message", Emulator.getTexts().getValue("commands.generic.cmd_badge.received"));
            this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.RECEIVED_BADGE.getKey(), keys));

            return true;
        }

        return false;
    }


    public void deleteBadge(HabboBadge badge) {
        if (badge != null) {
            this.inventory.getBadgesComponent().removeBadge(badge);
            BadgesComponent.deleteBadge(this.getHabboInfo().getId(), badge.getCode());
            this.client.sendResponse(new BadgesComposer(this));
        }
    }

    public void mute(int seconds, boolean isFlood) {
        if (seconds <= 0) {
            log.warn("Tried to mute user for {} seconds, which is invalid.", seconds);
            return;
        }

        if (!this.hasPermissionRight(Permission.ACC_NO_MUTE)) {
            int remaining = this.habboStats.addMuteTime(seconds);
            this.client.sendResponse(new FloodControlMessageComposer(remaining));
            this.client.sendResponse(new RemainingMutePeriodComposer(remaining));

            Room room = this.client.getHabbo().getRoomUnit().getRoom();
            if (room != null && !isFlood) {
                room.sendComposer(new IgnoreResultMessageComposer(this, IgnoreResultMessageComposer.MUTED).compose());
            }
        }
    }

    public void unMute() {
        this.habboStats.unMute();
        this.client.sendResponse(new FloodControlMessageComposer(3));
        Room room = this.client.getHabbo().getRoomUnit().getRoom();
        if (room != null) {
            room.sendComposer(new IgnoreResultMessageComposer(this, IgnoreResultMessageComposer.UNIGNORED).compose());
        }
    }

    public int noobStatus() {

        return 1;

    }

    public void clearCaches() {
        int timestamp = Emulator.getIntUnixTimestamp();
        THashMap<Integer, List<Integer>> newLog = new THashMap<>();
        for (Map.Entry<Integer, List<Integer>> ltdLog : this.habboStats.getLtdPurchaseLog().entrySet()) {
            for (Integer time : ltdLog.getValue()) {
                if (time > timestamp) {
                    if (!newLog.containsKey(ltdLog.getKey())) {
                        newLog.put(ltdLog.getKey(), new ArrayList<>());
                    }

                    newLog.get(ltdLog.getKey()).add(time);
                }
            }
        }

        this.habboStats.setLtdPurchaseLog(newLog);
    }


    public void respect(Habbo target) {
        if (target != null && target != this.client.getHabbo()) {
            target.getHabboStats().setRespectPointsReceived(target.getHabboStats().getRespectPointsReceived()+1);
            this.client.getHabbo().getHabboStats().setRespectPointsGiven(this.client.getHabbo().getHabboStats().getRespectPointsGiven()+1);
            this.client.getHabbo().getHabboStats().setRespectPointsToGive(this.client.getHabbo().getHabboStats().getRespectPointsToGive()-1);
            this.client.getHabbo().getRoomUnit().getRoom().sendComposer(new RoomUserRespectComposer(target).compose());
            this.client.getHabbo().getRoomUnit().getRoom().sendComposer(new ExpressionMessageComposer(this.client.getHabbo().getRoomUnit(), RoomUserAction.THUMB_UP).compose());

            AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("RespectGiven"));
            AchievementManager.progressAchievement(target, Emulator.getGameEnvironment().getAchievementManager().getAchievement("RespectEarned"));

            this.client.getHabbo().getRoomUnit().unIdle();
            this.client.getHabbo().getRoomUnit().setDance(DanceType.NONE);
        }
    }

    public Set<Integer> getForbiddenClothing() {
        TIntCollection clothingIDs = this.getInventory().getWardrobeComponent().getClothing();

        return Emulator.getGameEnvironment().getCatalogManager().clothing.values().stream()
                .filter(c -> !clothingIDs.contains(c.getId()))
                .map(ClothItem::getSetId)
                .flatMap(c -> Arrays.stream(c).boxed())
                .collect(Collectors.toSet());
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendInt(this.habboInfo.getId());
        message.appendString(this.habboInfo.getUsername());
        message.appendString(this.habboInfo.getMotto());
        message.appendString(this.habboInfo.getLook());
        message.appendInt(this.roomUnit.getVirtualId());
        message.appendInt(this.roomUnit.getCurrentPosition().getX());
        message.appendInt(this.roomUnit.getCurrentPosition().getY());
        message.appendString(String.valueOf(this.roomUnit.getCurrentZ()));
        message.appendInt(this.roomUnit.getBodyRotation().getValue());
        message.appendInt(1);
        message.appendString(this.habboInfo.getGender().name().toUpperCase());
        message.appendInt(this.habboStats.getGuild() != 0 ? this.habboStats.getGuild() : -1);
        message.appendInt(this.habboStats.getGuild() != 0 ? 1 : -1);

        String name = "";
        if (this.habboStats.getGuild() != 0) {
            Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(this.habboStats.getGuild());

            if (g != null)
                name = g.getName();
        }
        message.appendString(name);

        message.appendString("");
        message.appendInt(this.habboStats.getAchievementScore());
        message.appendBoolean(true);
    }
}
