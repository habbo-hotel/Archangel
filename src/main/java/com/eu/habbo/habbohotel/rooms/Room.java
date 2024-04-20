package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.bots.Bot;
import com.eu.habbo.habbohotel.bots.VisitorBot;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.guilds.GuildMember;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.ICycleable;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.rooms.chat.RoomChatManager;
import com.eu.habbo.habbohotel.rooms.constants.RoomConfiguration;
import com.eu.habbo.habbohotel.rooms.constants.RoomRightLevels;
import com.eu.habbo.habbohotel.rooms.constants.RoomTileState;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import com.eu.habbo.habbohotel.rooms.infractions.RoomInfractionManager;
import com.eu.habbo.habbohotel.rooms.items.RoomItemManager;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.rooms.promotions.RoomPromotionManager;
import com.eu.habbo.habbohotel.rooms.trades.RoomTradeManager;
import com.eu.habbo.habbohotel.rooms.wordquiz.RoomWordQuizManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ISerialize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericErrorComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.HabboBroadcastMessageComposer;
import com.eu.habbo.messages.outgoing.guild.HabboGroupDetailsMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.FlatAccessDeniedMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.FlatAccessibleMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.HeightMapUpdateMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.items.ItemUpdateMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.items.ObjectUpdateMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.items.OneWayDoorStatusMessageComposer;
import com.eu.habbo.plugin.events.furniture.FurnitureStackHeightEvent;
import com.eu.habbo.plugin.events.rooms.RoomLoadedEvent;
import com.eu.habbo.plugin.events.rooms.RoomUnloadedEvent;
import com.eu.habbo.plugin.events.rooms.RoomUnloadingEvent;
import gnu.trove.TCollections;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;
import io.netty.util.internal.ConcurrentSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.eu.habbo.database.DatabaseConstants.CAUGHT_SQL_EXCEPTION;

@Slf4j
public class Room implements Comparable<Room>, ISerialize, Runnable {
    @Getter
    private final RoomInfo roomInfo;
    @Getter
    private final RoomUnitManager roomUnitManager;
    @Getter
    private final RoomItemManager roomItemManager;
    @Getter
    private final RoomRightsManager roomRightsManager;
    @Getter
    private final RoomWordFilterManager roomWordFilterManager;
    @Getter
    private RoomTraxManager roomTraxManager;
    @Getter
    private final RoomWordQuizManager roomWordQuizManager;
    @Getter
    private final RoomInfractionManager roomInfractionManager;
    @Getter
    private final RoomPromotionManager roomPromotionManager;
    @Getter
    private final RoomTradeManager roomTradeManager;
    @Getter
    private final RoomChatManager roomChatManager;
    public final ConcurrentHashMap<RoomTile, THashSet<RoomItem>> tileCache = new ConcurrentHashMap<>();

    @Getter
    private final TIntObjectMap<Habbo> habboQueue = TCollections.synchronizedMap(new TIntObjectHashMap<>(0));

    @Getter
    private final ConcurrentSet<Game> games;
    @Getter
    private final TIntObjectMap<String> furniOwnerNames;
    private final TIntIntMap furniOwnerCount;
    private final Object loadLock = new Object();
    //Use appropriately. Could potentially cause memory leaks when used incorrectly.
    public volatile boolean preventUnloading = true;
    public volatile boolean preventUncaching = true;
    public final ConcurrentHashMap.KeySetView<ServerMessage, Boolean> scheduledComposers = ConcurrentHashMap.newKeySet();

    public ConcurrentHashMap.KeySetView<Runnable, Boolean> scheduledTasks = ConcurrentHashMap.newKeySet();

    public ScheduledFuture<?> roomCycleTask;
    @Getter
    @Setter
    private RoomLayout layout;
    private final String layoutName;
    @Getter
    @Setter
    private volatile boolean allowBotsWalk;
    @Getter
    @Setter
    private volatile boolean allowEffects;
    @Setter
    private volatile boolean needsUpdate;
    @Getter
    private volatile boolean loaded;
    @Getter
    private volatile boolean preLoaded;
    @Getter
    @Setter
    private volatile int lastTimerReset = Emulator.getIntUnixTimestamp();
    @Getter
    private RoomSpecialTypes roomSpecialTypes;
    private boolean cycleOdd;
    @Getter
    private long cycleTimestamp;
    @Getter
    final HashMap<RoomTile, InteractionWiredTrigger> triggersOnRoom;

    public Room(ResultSet set) throws SQLException {
        this.roomInfo = new RoomInfo(set);
        this.roomUnitManager = new RoomUnitManager(this);
        this.roomItemManager = new RoomItemManager(this);
        this.roomRightsManager = new RoomRightsManager(this);
        this.roomWordFilterManager = new RoomWordFilterManager(this);
        this.roomWordQuizManager = new RoomWordQuizManager(this);
        this.roomInfractionManager = new RoomInfractionManager(this);
        this.roomPromotionManager = new RoomPromotionManager(this);
        this.roomTradeManager = new RoomTradeManager(this);
        this.roomChatManager = new RoomChatManager(this);

        this.layoutName = set.getString("model");

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            roomPromotionManager.loadPromotions(connection);
            roomInfractionManager.loadBans(connection);
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        this.preLoaded = true;
        this.allowBotsWalk = true;
        this.allowEffects = true;
        this.furniOwnerNames = TCollections.synchronizedMap(new TIntObjectHashMap<>(0));
        this.furniOwnerCount = TCollections.synchronizedMap(new TIntIntHashMap(0));

        this.games = new ConcurrentSet<>();

        this.triggersOnRoom = new HashMap<>();
    }

    public synchronized void loadData() {
        synchronized (this.loadLock) {
            if (!this.preLoaded || this.loaded) return;

            this.preLoaded = false;

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                this.roomUnitManager.clear();

                this.roomSpecialTypes = new RoomSpecialTypes();

                this.loadLayout();
                this.roomRightsManager.load(connection);
                this.roomItemManager.load(connection);
                this.loadHeightmap();
                this.roomUnitManager.load(connection);
                this.roomWordFilterManager.load(connection);

                this.loaded = true;
                this.roomCycleTask = Emulator.getThreading().getService().scheduleAtFixedRate(this, 500, 500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
            }

            this.roomTraxManager = new RoomTraxManager(this);

            if (this.roomInfo.isJukeboxEnabled()) {
                this.roomTraxManager.play(0);
                for (RoomItem item : this.roomSpecialTypes.getItemsOfType(InteractionJukeBox.class)) {
                    this.updateItem(item.setExtraData("1"));
                }
            }

            for (RoomItem item : this.roomSpecialTypes.getItemsOfType(InteractionFireworks.class)) {
                this.updateItem(item.setExtraData("1"));
            }
        }

        Emulator.getPluginManager().fireEvent(new RoomLoadedEvent(this));
    }

    private synchronized void loadLayout() {
        try {
            if (this.layout == null) {
                if (this.roomInfo.isModelOverridden()) {
                    this.layout = Emulator.getGameEnvironment().getRoomManager().loadCustomLayout(this);
                } else {
                    this.layout = Emulator.getGameEnvironment().getRoomManager().loadLayout(this.layoutName, this);
                }
            }
        } catch (Exception e) {
            log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
        }
    }

    private synchronized void loadHeightmap() {
        try {
            if (this.layout != null) {
                for (short x = 0; x < this.layout.getMapSizeX(); x++) {
                    for (short y = 0; y < this.layout.getMapSizeY(); y++) {
                        RoomTile tile = this.layout.getTile(x, y);
                        if (tile != null) {
                            this.updateTile(tile);
                        }
                    }
                }
            } else {
                log.error("Unknown Room Layout for Room (ID: {})", this.roomInfo.getId());
            }
        } catch (Exception e) {
            log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
        }
    }

    public void updateTile(RoomTile tile) {
        if (tile != null) {
            tile.setStackHeight(this.getStackHeight(tile.getX(), tile.getY(), false));
            tile.setState(this.calculateTileState(tile));
        }
    }

    public void updateTiles(THashSet<RoomTile> tiles) {
        for (RoomTile tile : tiles) {
            this.tileCache.remove(tile);
            updateTile(tile);
        }

        this.sendComposer(new HeightMapUpdateMessageComposer(this, tiles).compose());
    }

    public RoomTileState calculateTileState(RoomTile tile) {
        return this.calculateTileState(tile, null);
    }

    public RoomTileState calculateTileState(RoomTile tile, RoomItem exclude) {
        if (tile == null || tile.getState() == RoomTileState.INVALID) return RoomTileState.INVALID;

        RoomTileState result = RoomTileState.OPEN;
        THashSet<RoomItem> items = this.roomItemManager.getItemsAt(tile);

        if (items == null) return RoomTileState.INVALID;

        RoomItem tallestItem = null;

        for (RoomItem item : items) {
            if (exclude != null && item == exclude) continue;

            if (item.getBaseItem().allowLay()) {
                return RoomTileState.LAY;
            }

            if (tallestItem != null) {
                if (tallestItem.getCurrentZ() + Item.getCurrentHeight(tallestItem) > item.getCurrentZ() + Item.getCurrentHeight(item))
                    continue;
            }

            result = this.checkStateForItem(item, tile);
            tallestItem = item;
        }

        return result;
    }

    private RoomTileState checkStateForItem(RoomItem item, RoomTile tile) {
        RoomTileState result = RoomTileState.BLOCKED;

        if (item.isWalkable()) {
            result = RoomTileState.OPEN;
        } else if (item.getBaseItem().allowSit()) {
            result = RoomTileState.SIT;
        } else if (item.getBaseItem().allowLay()) {
            result = RoomTileState.LAY;
        }

        RoomTileState overriddenState = item.getOverrideTileState(tile, this);
        if (overriddenState != null) {
            result = overriddenState;
        }

        return result;
    }

    public void updateDatabaseUserCount() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE rooms SET users = ? WHERE id = ? LIMIT 1")) {
            statement.setInt(1, this.roomUnitManager.getRoomHabbosCount());
            statement.setInt(2, this.roomInfo.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    private void cycle() {
        this.cycleOdd = !this.cycleOdd;
        this.cycleTimestamp = System.currentTimeMillis();
        boolean foundRightHolder = false;

        boolean loaded;
        synchronized (this.loadLock) {
            loaded = this.loaded;
        }
        this.tileCache.clear();
        if (loaded) {
            if (!this.scheduledTasks.isEmpty()) {
                ConcurrentHashMap.KeySetView<Runnable, Boolean> tasks = this.scheduledTasks;
                this.scheduledTasks = ConcurrentHashMap.newKeySet();

                for (Runnable runnable : tasks) {
                    Emulator.getThreading().run(runnable);
                }
            }

            for (ICycleable task : this.roomSpecialTypes.getCycleTasks()) {
                task.cycle(this);
            }

            foundRightHolder = roomUnitManager.cycle(cycleOdd);
        }

        synchronized (this.habboQueue) {
            if (!this.habboQueue.isEmpty() && !foundRightHolder) {
                this.habboQueue.forEachEntry((a, b) -> {
                    if (b.isOnline()) {
                        if (b.getHabboInfo().getRoomQueueId() == this.roomInfo.getId()) {
                            b.getClient().sendResponse(new FlatAccessDeniedMessageComposer(""));
                        }
                    }
                    return true;
                });

                this.habboQueue.clear();
            }
        }

        if (!this.scheduledComposers.isEmpty()) {
            for (ServerMessage message : this.scheduledComposers) {
                this.sendComposer(message);
            }

            this.scheduledComposers.clear();
        }
    }


    @Override
    public void run() {
        synchronized (this.loadLock) {
            if (this.loaded) {
                try {
                    Emulator.getThreading().run(Room.this::cycle);
                } catch (Exception e) {
                    log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
                }
            }
        }

        this.save();
    }

    public void save() {
        if (this.needsUpdate) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                this.roomInfo.update(connection);
                this.needsUpdate = false;
            } catch (SQLException e) {
                log.error(CAUGHT_SQL_EXCEPTION, e);
            }
        }
    }

    @Override
    public void serialize(ServerMessage message) {
        message.appendInt(this.roomInfo.getId());
        message.appendString(this.roomInfo.getName());

        if (this.roomInfo.isPublicRoom()) {
            message.appendInt(0);
            message.appendString("");
        } else {
            message.appendInt(this.roomInfo.getOwnerInfo().getId());
            message.appendString(this.roomInfo.getOwnerInfo().getUsername());
        }

        message.appendInt(this.roomInfo.getState().ordinal());
        message.appendInt(this.roomUnitManager.getRoomHabbosCount());
        message.appendInt(this.roomInfo.getMaxUsers());
        message.appendString(this.roomInfo.getDescription());
        message.appendInt(this.roomInfo.getTradeMode());
        message.appendInt(this.roomInfo.getScore());
        message.appendInt(0);
        message.appendInt(this.roomInfo.getCategory() != null ? this.roomInfo.getCategory().getId() : 0);

        String[] tags = Arrays.stream(this.roomInfo.getTags().split(";")).filter(t -> !t.isEmpty()).toArray(String[]::new);
        message.appendInt(tags.length);
        for (String s : tags) {
            message.appendString(s);
        }

        int base = 0;

        if (this.roomInfo.hasGuild()) {
            base = base | 2;
        }

        if (this.roomInfo.isPromoted()) {
            base = base | 4;
        }

        if (!this.roomInfo.isPublicRoom()) {
            base = base | 8;
        }

        if (this.roomInfo.isAllowPets()) {
            base = base | 16;
        }

        message.appendInt(base);


        if (this.roomInfo.hasGuild()) {
            message.appendInt(this.roomInfo.getGuild().getId());
            message.appendString(this.roomInfo.getGuild().getName());
            message.appendString(this.roomInfo.getGuild().getBadge());
        }

        if (this.roomInfo.isPromoted()) {
            message.appendString(roomPromotionManager.getPromotion().getTitle());
            message.appendString(roomPromotionManager.getPromotion().getDescription());
            message.appendInt((roomPromotionManager.getPromotion().getEndTimestamp() - Emulator.getIntUnixTimestamp()) / 60);
        }
    }

    @Override
    public int compareTo(Room room) {
        return RoomConfiguration.SORT_USERS_COUNT.compare(this, room);
    }

    public synchronized void dispose() {
        synchronized (this.loadLock) {
            if (this.preventUnloading) return;

            if (Emulator.getPluginManager().fireEvent(new RoomUnloadingEvent(this)).isCancelled()) return;

            if (this.loaded) {
                try {

                    if (this.roomTraxManager != null && !this.roomTraxManager.disposed()) {
                        this.roomTraxManager.dispose();
                    }

                    this.roomCycleTask.cancel(false);
                    this.scheduledTasks.clear();
                    this.scheduledComposers.clear();
                    this.loaded = false;

                    this.tileCache.clear();

                    synchronized (getRoomInfractionManager().getMutedHabbos()) {
                        getRoomInfractionManager().getMutedHabbos().clear();
                    }

                    for (InteractionGameTimer timer : this.getRoomSpecialTypes().getGameTimers().values()) {
                        timer.setRunning(false);
                    }

                    for (Game game : this.games) {
                        game.dispose();
                    }

                    this.games.clear();

                    this.roomUnitManager.getRoomPetManager().removeAllPetsExceptRoomOwner();

                    this.roomItemManager.dispose();

                    if (this.roomSpecialTypes != null) {
                        this.roomSpecialTypes.dispose();
                    }

                    synchronized (this.habboQueue) {
                        this.habboQueue.clear();
                    }

                    this.roomUnitManager.dispose();
                } catch (Exception e) {
                    log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
                }
            }

            try {
                this.updateDatabaseUserCount();
                this.preLoaded = true;
                this.layout = null;
            } catch (Exception e) {
                log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
            }
        }

        Emulator.getPluginManager().fireEvent(new RoomUnloadedEvent(this));
    }

    public void setDiagonalMoveEnabled(boolean moveDiagonally) {
        this.roomInfo.setDiagonalMoveEnabled(moveDiagonally);
        this.layout.moveDiagonally(moveDiagonally);
        this.needsUpdate = true;
    }

    public Color getBackgroundTonerColor() {
        Color color = new Color(0, 0, 0);
        Iterator<RoomItem> iterator = this.roomItemManager.getCurrentItems().values().iterator();

        for (int i = this.roomItemManager.getCurrentItems().size(); i > 0; i--) {
            try {
                RoomItem object = iterator.next();

                if (object instanceof InteractionBackgroundToner) {
                    String[] extraData = object.getExtraData().split(":");

                    if (extraData.length == 4 && extraData[0].equalsIgnoreCase("1")) {
                        return Color.getHSBColor(Integer.parseInt(extraData[1]), Integer.parseInt(extraData[2]), Integer.parseInt(extraData[3]));

                    }
                }
            } catch (Exception ignored) {
            }
        }

        return color;
    }

    public void setRollerSpeed(int rollerSpeed) {
        this.roomInfo.setRollerSpeed(rollerSpeed);
        roomUnitManager.setRollerCycle(0);
        this.needsUpdate = true;
    }

    public boolean addGame(Game game) {
        synchronized (this.games) {
            return this.games.add(game);
        }
    }

    public boolean deleteGame(Game game) {
        game.stop();
        game.dispose();
        synchronized (this.games) {
            return this.games.remove(game);
        }
    }

    public Game getGame(Class<? extends Game> gameType) {
        if (gameType == null) return null;

        synchronized (this.games) {
            for (Game game : this.games) {
                if (gameType.isInstance(game)) {
                    return game;
                }
            }
        }

        return null;
    }

    public Game getGameOrCreate(Class<? extends Game> gameType) {
        Game game = this.getGame(gameType);
        if (game == null) {
            try {
                game = gameType.getDeclaredConstructor(Room.class).newInstance(this);
                this.addGame(game);
            } catch (Exception e) {
                log.error("Error getting game " + gameType.getName(), e);
            }
        }

        return game;
    }

    public String getFurniOwnerName(int userId) {
        return this.furniOwnerNames.get(userId);
    }

    public void addToQueue(Habbo habbo) {
        synchronized (this.habboQueue) {
            this.habboQueue.put(habbo.getHabboInfo().getId(), habbo);
        }
    }

    public boolean removeFromQueue(Habbo habbo) {
        try {
            this.sendComposer(new FlatAccessibleMessageComposer(habbo.getHabboInfo().getUsername()).compose());

            synchronized (this.habboQueue) {
                return this.habboQueue.remove(habbo.getHabboInfo().getId()) != null;
            }
        } catch (Exception e) {
            log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
        }

        return true;
    }

    public List<RoomItem> getPostItNotes() {
        return this.roomItemManager.getCurrentItems().values().stream().filter(i -> i.getBaseItem().getInteractionType().getType() == InteractionPostIt.class).toList();
    }

    public void kickHabbo(Habbo habbo, boolean alert) {
        if (alert) {
            habbo.getClient().sendResponse(new GenericErrorComposer(GenericErrorComposer.KICKED_OUT_OF_THE_ROOM));
        }

        habbo.getRoomUnit().setKicked(true);
        habbo.getRoomUnit().walkTo(this.layout.getDoorTile());

        if (habbo.getRoomUnit().getPath() == null || habbo.getRoomUnit().getPath().size() <= 1 || this.roomInfo.isPublicRoom()) {
            habbo.getRoomUnit().setCanWalk(true);
            Emulator.getGameEnvironment().getRoomManager().leaveRoom(habbo, this);
        }
    }

    public THashSet<Habbo> getHabbosOnItem(RoomItem item) {
        THashSet<Habbo> habbos = new THashSet<>();
        for (short x = item.getCurrentPosition().getX(); x < item.getCurrentPosition().getX() + item.getBaseItem().getLength(); x++) {
            for (short y = item.getCurrentPosition().getY(); y < item.getCurrentPosition().getY() + item.getBaseItem().getWidth(); y++) {
                RoomTile tile = this.layout.getTile(x, y);
                habbos.addAll(this.roomUnitManager.getHabbosAt(tile));
            }
        }

        return habbos;
    }

    public THashSet<Bot> getBotsOnItem(RoomItem item) {
        THashSet<Bot> bots = new THashSet<>();
        bots.addAll(this.roomUnitManager.getRoomBotManager().getBotsOnItem(item));
        return bots;
    }

    public THashSet<Pet> getPetsOnItem(RoomItem item) {
        THashSet<Pet> pets = new THashSet<>();
        pets.addAll(this.roomUnitManager.getRoomPetManager().getPetsOnItem(item));
        return pets;
    }

    public void teleportHabboToItem(Habbo habbo, RoomItem item) {
        this.teleportRoomUnitToLocation(habbo.getRoomUnit(), item.getCurrentPosition().getX(), item.getCurrentPosition().getY(), item.getCurrentZ() + Item.getCurrentHeight(item));
    }

    public void teleportRoomUnitToLocation(RoomUnit roomUnit, short x, short y, double z) {
        if (this.loaded) {
            RoomTile tile = this.layout.getTile(x, y);

            if (z < tile.getZ()) {
                z = tile.getZ();
            }

            roomUnit.setLocation(tile);
            roomUnit.walkTo(tile);
            roomUnit.setCurrentZ(z);
            roomUnitManager.updateRoomUnit(roomUnit);
        }
    }

    public void habboEntered(Habbo habbo) {
        synchronized (this.roomUnitManager.getRoomBotManager().getCurrentBots()) {
            if (habbo.getHabboInfo().getId() != this.roomInfo.getOwnerInfo().getId()) return;

            Iterator<Bot> botIterator = this.roomUnitManager.getRoomBotManager().getCurrentBots().values().iterator();

            while (botIterator.hasNext()) {
                try {
                    Bot bot = botIterator.next();

                    if (bot instanceof VisitorBot visitorBot) {
                        visitorBot.onUserEnter(habbo);
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }

        RoomItem doorTileTopItem = this.roomItemManager.getTopItemAt(habbo.getRoomUnit().getCurrentPosition().getX(), habbo.getRoomUnit().getCurrentPosition().getY());
        if (doorTileTopItem != null && !(doorTileTopItem instanceof InteractionTeleportTile)) {
            try {
                doorTileTopItem.onWalkOn(habbo.getRoomUnit(), this, new Object[]{});
            } catch (Exception e) {
                log.error(RoomConfiguration.CAUGHT_EXCEPTION, e);
            }
        }
    }

    public THashSet<RoomTile> getLockedTiles() {
        THashSet<RoomTile> lockedTiles = new THashSet<>();
        Iterator<RoomItem> iterator = this.roomItemManager.getCurrentItems().values().iterator();

        for (int itemsCount = this.roomItemManager.getCurrentItems().size(); itemsCount-- > 0; ) {
            RoomItem item;
            try {
                item = iterator.next();
            } catch (Exception e) {
                break;
            }
            if (item.getBaseItem().getType() != FurnitureType.FLOOR) continue;
            boolean found = lockedTiles.stream().anyMatch(tile -> tile.getX() == item.getCurrentPosition().getX() && tile.getY() == item.getCurrentPosition().getY());
            if (!found) {
                addLockedTiles(lockedTiles, item);
            }
        }
        return lockedTiles;
    }

    private void addLockedTiles(THashSet<RoomTile> lockedTiles, RoomItem item) {
        short length;
        short width;

        if (item.getRotation() == 0 || item.getRotation() == 4) {
            length = item.getBaseItem().getLength();
            width = item.getBaseItem().getWidth();
        } else {
            width = item.getBaseItem().getLength();
            length = item.getBaseItem().getWidth();
        }

        for (short y = 0; y < length; y++) {
            for (short x = 0; x < width; x++) {
                RoomTile tile = this.layout.getTile((short) (item.getCurrentPosition().getX() + x), (short) (item.getCurrentPosition().getY() + y));
                if (tile != null) {
                    lockedTiles.add(tile);
                }
            }
        }
    }

    public double getStackHeight(short x, short y, boolean calculateHeightmap, RoomItem exclude) {
        if (x < 0 || y < 0 || this.layout == null) return calculateHeightmap ? Short.MAX_VALUE : 0.0;

        if (Emulator.getPluginManager().isRegistered(FurnitureStackHeightEvent.class, true)) {
            FurnitureStackHeightEvent event = Emulator.getPluginManager().fireEvent(new FurnitureStackHeightEvent(x, y, this));
            if (event.hasPluginHelper()) {
                return calculateHeightmap ? event.getHeight() * 256.0D : event.getHeight();
            }
        }

        double height = this.layout.getHeightAtSquare(x, y);
        boolean canStack = true;

        THashSet<RoomItem> stackHelpers = this.roomItemManager.getItemsAt(InteractionStackHelper.class, x, y);


        for (RoomItem item : stackHelpers) {
            if (item == exclude) continue;
            if (calculateHeightmap) {
                return item.getCurrentZ() * 256.0D;
            } else {
                return item.getCurrentZ();
            }
        }


        RoomItem item = this.roomItemManager.getTopItemAt(x, y, exclude);
        if (item != null) {
            canStack = item.getBaseItem().allowStack();
            height = item.getCurrentZ() + (item.getBaseItem().allowSit() ? 0 : Item.getCurrentHeight(item));
        }

        if (calculateHeightmap) {
            return (canStack ? height * 256.0D : Short.MAX_VALUE);
        }

        return canStack ? height : -1;
    }

    public double getStackHeight(short x, short y, boolean calculateHeightmap) {
        return this.getStackHeight(x, y, calculateHeightmap, null);
    }

    public boolean canSitOrLayAt(RoomTile tile) {
        if (tile == null || this.roomUnitManager.hasHabbosAt(tile)) {
            return false;
        }

        THashSet<RoomItem> items = this.roomItemManager.getItemsAt(tile);

        return this.canSitAt(items) || this.canLayAt(items);
    }

    public boolean canSitAt(int x, int y) {
        RoomTile tile = this.layout.getTile((short) x, (short) y);

        if (tile == null) {
            return false;
        }

        if (this.roomUnitManager.hasHabbosAt(tile)) return false;

        return this.canSitAt(this.roomItemManager.getItemsAt(tile));
    }

    boolean canSitAt(THashSet<RoomItem> items) {
        if (items == null) return false;
        RoomItem tallestItem = null;
        double tallestItemHeight = 0;
        for (RoomItem item : items) {
            double currentItemHeight = item.getCurrentZ() + Item.getCurrentHeight(item);
            if (tallestItem == null || tallestItemHeight <= currentItemHeight) {
                tallestItem = item;
                tallestItemHeight = currentItemHeight;
            }
        }
        return tallestItem != null && tallestItem.getBaseItem().allowSit();
    }

    public boolean canLayAt(RoomTile tile) {
        return this.canLayAt(this.roomItemManager.getItemsAt(tile));
    }

    boolean canLayAt(Set<RoomItem> roomItems) {
        if (roomItems == null || roomItems.isEmpty()) return true;

        RoomItem topItem = roomItems.iterator().next();
        topItem = roomItems.stream().filter(Objects::nonNull).max(Comparator.comparingDouble(RoomItem::getCurrentZ)).orElse(topItem);

        return topItem.getBaseItem().allowLay();
    }

    public RoomTile getRandomWalkableTile() {
        for (int i = 0; i < 10; i++) {
            RoomTile tile = this.layout.getTile((short) (Math.random() * this.layout.getMapSizeX()), (short) (Math.random() * this.layout.getMapSizeY()));
            if (tile != null && tile.getState() != RoomTileState.BLOCKED && tile.getState() != RoomTileState.INVALID) {
                return tile;
            }
        }

        return null;
    }

    public void alert(String message) {
        this.sendComposer(new HabboBroadcastMessageComposer(message).compose());
    }

    public void sendComposer(ServerMessage message) {
        for (Habbo habbo : this.roomUnitManager.getCurrentHabbos().values()) {
            if (habbo.getClient() == null) continue;

            habbo.getClient().sendResponse(message);
        }
    }

    public void petChat(ServerMessage message) {
        for (Habbo habbo : this.roomUnitManager.getCurrentHabbos().values()) {
            if (!habbo.getHabboStats().isIgnorePets()) habbo.getClient().sendResponse(message);
        }
    }

    public void botChat(ServerMessage message) {
        if (message == null) {
            return;
        }

        for (Habbo habbo : this.roomUnitManager.getCurrentHabbos().values()) {
            if (habbo == null) {
                return;
            }
            if (!habbo.getHabboStats().isIgnoreBots()) habbo.getClient().sendResponse(message);
        }
    }


    public RoomRightLevels getGuildRightLevel(Habbo habbo) {
        if (!this.roomInfo.hasGuild()) {
            return RoomRightLevels.NONE;
        }

        if (habbo.getHabboStats().hasGuild(this.roomInfo.getGuild().getId())) {
            if (Emulator.getGameEnvironment().getGuildManager().getOnlyAdmins(this.roomInfo.getGuild()).get(habbo.getHabboInfo().getId()) != null)
                return RoomRightLevels.GUILD_ADMIN;

            if (this.roomInfo.getGuild().isRights()) {
                return RoomRightLevels.GUILD_RIGHTS;
            }
        }

        return RoomRightLevels.NONE;
    }

    public void updateItem(RoomItem item) {
        if (!this.isLoaded()) {
            return;
        }

        if (item != null && item.getRoomId() == this.roomInfo.getId() && item.getBaseItem() != null) {
            if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
                this.sendComposer(new ObjectUpdateMessageComposer(item).compose());
                this.updateTiles(this.layout.getTilesAt(this.layout.getTile(item.getCurrentPosition().getX(), item.getCurrentPosition().getY()), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), item.getRotation()));
            } else if (item.getBaseItem().getType() == FurnitureType.WALL) {
                this.sendComposer(new ItemUpdateMessageComposer(item).compose());
            }
        }

    }

    public void updateItemState(RoomItem item) {
        if (!item.isLimited()) {
            this.sendComposer(new OneWayDoorStatusMessageComposer(item).compose());
        } else {
            this.sendComposer(new ObjectUpdateMessageComposer(item).compose());
        }

        if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
            if (this.layout == null) return;

            this.updateTiles(this.layout.getTilesAt(this.layout.getTile(item.getCurrentPosition().getX(), item.getCurrentPosition().getY()), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), item.getRotation()));

            if (item instanceof InteractionMultiHeight interactionMultiHeight) {
                interactionMultiHeight.updateUnitsOnItem(this);
            }
        }
    }

    public int getUserFurniCount(int userId) {
        return this.furniOwnerCount.get(userId);
    }

    public int getUserUniqueFurniCount(int userId) {
        THashSet<Item> items = new THashSet<>();

        for (RoomItem item : this.roomItemManager.getCurrentItems().values()) {
            if (!items.contains(item.getBaseItem()) && item.getOwnerInfo().getId() == userId)
                items.add(item.getBaseItem());
        }

        return items.size();
    }

    public void refreshGuild(Guild guild) {
        if (guild.getRoomId() == this.roomInfo.getId()) {
            THashSet<GuildMember> members = Emulator.getGameEnvironment().getGuildManager().getGuildMembers(guild.getId());

            for (Habbo habbo : this.roomUnitManager.getCurrentHabbos().values()) {
                Optional<GuildMember> member = members.stream().filter(m -> m.getUserId() == habbo.getHabboInfo().getId()).findAny();

                if (member.isEmpty()) continue;

                habbo.getClient().sendResponse(new HabboGroupDetailsMessageComposer(guild, habbo.getClient(), false, member.get()));
            }
        }

        this.refreshGuildRightsInRoom();
    }

    public void refreshGuildColors(Guild guild) {
        if (guild.getRoomId() == this.roomInfo.getId()) {
            Iterator<RoomItem> iterator = this.roomItemManager.getCurrentItems().values().iterator();

            for (int i = this.roomItemManager.getCurrentItems().size(); i-- > 0; ) {
                RoomItem roomItem = iterator.next();
                if (roomItem instanceof InteractionGuildFurni interactionGuildFurni && interactionGuildFurni.getGuildId() == guild.getId()) {
                    this.updateItem(roomItem);
                }
            }
        }
    }

    public void refreshGuildRightsInRoom() {
        for (Habbo habbo : this.roomUnitManager.getCurrentHabbos().values()) {
            if ((habbo.getRoomUnit().getRoom() == this && habbo.getHabboInfo().getId() != this.roomInfo.getOwnerInfo().getId()) && (!(habbo.hasPermissionRight(Permission.ACC_ANYROOMOWNER) || habbo.hasPermissionRight(Permission.ACC_MOVEROTATE))))
                this.getRoomRightsManager().refreshRightsForHabbo(habbo);
        }
    }
}