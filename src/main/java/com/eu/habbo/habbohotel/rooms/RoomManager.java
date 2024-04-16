package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.RoomUserPetComposer;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.games.battlebanzai.BattleBanzaiGame;
import com.eu.habbo.habbohotel.games.football.FootballGame;
import com.eu.habbo.habbohotel.games.freeze.FreezeGame;
import com.eu.habbo.habbohotel.games.tag.BunnyrunGame;
import com.eu.habbo.habbohotel.games.tag.IceTagGame;
import com.eu.habbo.habbohotel.games.tag.RollerskateGame;
import com.eu.habbo.habbohotel.games.wired.WiredGame;
import com.eu.habbo.habbohotel.guilds.Guild;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.navigation.NavigatorFilterComparator;
import com.eu.habbo.habbohotel.navigation.NavigatorFilterField;
import com.eu.habbo.habbohotel.navigation.NavigatorManager;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.pets.PetData;
import com.eu.habbo.habbohotel.pets.PetTasks;
import com.eu.habbo.habbohotel.polls.Poll;
import com.eu.habbo.habbohotel.polls.PollManager;
import com.eu.habbo.habbohotel.rooms.constants.RoomConfiguration;
import com.eu.habbo.habbohotel.rooms.constants.RoomRightLevels;
import com.eu.habbo.habbohotel.rooms.constants.RoomState;
import com.eu.habbo.habbohotel.rooms.constants.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.entities.RoomRotation;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnitType;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomHabbo;
import com.eu.habbo.habbohotel.rooms.infractions.RoomBan;
import com.eu.habbo.habbohotel.rooms.items.entities.RoomItem;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.incoming.users.NewUserExperienceScriptProceedEvent;
import com.eu.habbo.messages.outgoing.generic.alerts.GenericErrorComposer;
import com.eu.habbo.messages.outgoing.hotelview.CloseConnectionMessageComposer;
import com.eu.habbo.messages.outgoing.polls.PollOfferComposer;
import com.eu.habbo.messages.outgoing.rooms.*;
import com.eu.habbo.messages.outgoing.rooms.items.ItemsComposer;
import com.eu.habbo.messages.outgoing.rooms.items.ObjectsMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.pets.RoomPetComposer;
import com.eu.habbo.messages.outgoing.rooms.promotions.RoomEventComposer;
import com.eu.habbo.messages.outgoing.rooms.users.*;
import com.eu.habbo.messages.outgoing.users.RemainingMutePeriodComposer;
import com.eu.habbo.plugin.events.navigator.NavigatorRoomCreatedEvent;
import com.eu.habbo.plugin.events.rooms.RoomFloorItemsLoadEvent;
import com.eu.habbo.plugin.events.rooms.RoomUncachedEvent;
import com.eu.habbo.plugin.events.rooms.UserVoteRoomEvent;
import com.eu.habbo.plugin.events.users.HabboAddedToRoomEvent;
import com.eu.habbo.plugin.events.users.UserEnterRoomEvent;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;
import com.eu.habbo.roleplay.corp.CorpShiftManager;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.eu.habbo.database.DatabaseConstants.CAUGHT_SQL_EXCEPTION;

@Slf4j
public class RoomManager {

    //Configuration. Loaded from database & updated accordingly.
    public static int MAXIMUM_ROOMS_USER = 25;
    public static int MAXIMUM_ROOMS_HC = 35;
    public static int HOME_ROOM_ID = 0;
    public static boolean SHOW_PUBLIC_IN_POPULAR_TAB = false;
    @Getter
    private final THashMap<Integer, RoomCategory> roomCategories;
    private final List<String> mapNames;
    private final ConcurrentHashMap<Integer, Room> activeRooms;
    @Getter
    private final ArrayList<Class<? extends Game>> gameTypes;

    public RoomManager() {
        long millis = System.currentTimeMillis();
        this.roomCategories = new THashMap<>();
        this.mapNames = new ArrayList<>();
        this.activeRooms = new ConcurrentHashMap<>();
        this.loadRoomCategories();
        this.loadRoomModels();

        this.gameTypes = new ArrayList<>();

        registerGameType(BattleBanzaiGame.class);
        registerGameType(FreezeGame.class);
        registerGameType(WiredGame.class);
        registerGameType(FootballGame.class);
        registerGameType(BunnyrunGame.class);
        registerGameType(IceTagGame.class);
        registerGameType(RollerskateGame.class);

        log.info("Room Manager -> Loaded! (" + (System.currentTimeMillis() - millis) + " MS)");
    }

    public void loadRoomModels() {
        this.mapNames.clear();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM room_models")) {
            while (set.next()) {
                this.mapNames.add(set.getString("name"));
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    public CustomRoomLayout loadCustomLayout(Room room) {
        CustomRoomLayout layout = null;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM room_models_custom WHERE id = ? LIMIT 1")) {
            statement.setInt(1, room.getRoomInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    layout = new CustomRoomLayout(set, room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return layout;
    }

    private void loadRoomCategories() {
        this.roomCategories.clear();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM navigator_flatcats")) {
            while (set.next()) {
                this.roomCategories.put(set.getInt("id"), new RoomCategory(set));
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    public void loadAllRooms() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms ORDER BY id DESC")) {
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Room room = new Room(set);
                    room.preventUncaching = true;
                    this.activeRooms.put(set.getInt("id"), room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    public void loadPublicRooms() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms WHERE is_public = ? OR is_staff_picked = ? ORDER BY id DESC")) {
            statement.setString(1, "1");
            statement.setString(2, "1");
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Room room = new Room(set);
                    room.preventUncaching = true;
                    this.activeRooms.put(set.getInt("id"), room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    public THashMap<Integer, List<Room>> findRooms(NavigatorFilterField filterField, String value, int category, boolean showInvisible) {
        THashMap<Integer, List<Room>> rooms = new THashMap<>();
        String query = filterField.getDatabaseQuery() + " AND rooms.state NOT LIKE " + (showInvisible ? "''" : "'invisible'") + (category >= 0 ? "AND rooms.category = '" + category + "'" : "") + "  ORDER BY rooms.users, rooms.id DESC LIMIT " + NavigatorManager.MAXIMUM_RESULTS_PER_PAGE;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, (filterField.getComparator() == NavigatorFilterComparator.EQUALS ? value : "%" + value + "%"));
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Room room = this.activeRooms.get(set.getInt("id"));

                    if (room == null) {
                        room = new Room(set);
                        this.activeRooms.put(set.getInt("id"), room);
                    }

                    if (!rooms.containsKey(set.getInt("category"))) {
                        rooms.put(set.getInt("category"), new ArrayList<>());
                    }

                    rooms.get(set.getInt("category")).add(room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public RoomCategory getCategory(int id) {
        return roomCategories.values().stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public RoomCategory getCategory(String name) {
        return roomCategories.values().stream().filter(c -> c.getCaption().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public RoomCategory getCategoryBySafeCaption(String safeCaption) {
        return roomCategories.values().stream().filter(c -> c.getCaptionSave().equalsIgnoreCase(safeCaption)).findFirst().orElse(null);
    }

    public List<RoomCategory> roomCategoriesForHabbo(Habbo habbo) {
        List<RoomCategory> categories = new ArrayList<>();
        for (RoomCategory category : this.roomCategories.values()) {
            if (category.getMinRank() <= habbo.getHabboInfo().getPermissionGroup().getId())
                categories.add(category);
        }

        Collections.sort(categories);

        return categories;
    }

    public boolean hasCategory(int categoryId, Habbo habbo) {
        return roomCategories.values().stream().anyMatch(c -> c.getId() == categoryId && c.getMinRank() <= habbo.getHabboInfo().getPermissionGroup().getId());
    }

    public List<Room> getRoomsByScore() {
        List<Room> rooms = new ArrayList<>(this.activeRooms.values());
        rooms.sort(RoomConfiguration.SORT_SCORE);

        return rooms;
    }

    public List<Room> getActiveRoomsByCategoryId(int categoryId) {
        List<Room> rooms = new ArrayList<>();
        for (Room room : this.activeRooms.values()) {
            if (categoryId == room.getRoomInfo().getCategory().getId() || categoryId == -1)
                rooms.add(room);
        }
        Collections.sort(rooms);
        return rooms;
    }

    //TODO Move to HabboInfo class.
    public ArrayList<Room> getRoomsForHabbo(Habbo habbo) {
        ArrayList<Room> rooms = new ArrayList<>();
        for (Room room : this.activeRooms.values()) {
            if (room.getRoomInfo().isRoomOwner(habbo)) {
                rooms.add(room);
            }
        }

        rooms.sort(RoomConfiguration.SORT_ID);

        return rooms;
    }

    public ArrayList<Room> getRoomsForHabbo(String username) {
        Habbo h = Emulator.getGameEnvironment().getHabboManager().getHabbo(username);
        if (h != null) {
            return this.getRoomsForHabbo(h);
        }

        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms WHERE owner_name = ? ORDER BY id DESC LIMIT 25")) {
            statement.setString(1, username);
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    rooms.add(this.getRoom(set.getInt("id")));
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public Room getRoom(int roomId) {
        return getRoom(roomId, false);
    }

    public Room getRoom(int id, boolean loadData) {
        Room room = null;

        if (this.activeRooms.containsKey(id)) {
            room = this.getActiveRoomById(id);

            if (loadData && (room.isPreLoaded() && !room.isLoaded())) {
                room.loadData();
            }

            return room;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms WHERE id = ? LIMIT 1")) {
            statement.setInt(1, id);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    room = new Room(set);
                    if (loadData) {
                        room.loadData();
                    }
                }
            }

            if (room != null) {
                this.activeRooms.put(room.getRoomInfo().getId(), room);
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return room;
    }


    public Room createRoom(int ownerId, String ownerName, String name, String description, String modelName, int maxUsers, int categoryId, int tradeType) {
        Room room = null;

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO rooms (owner_id, owner_name, name, description, model, users_max, category, trade_mode) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, ownerId);
            statement.setString(2, ownerName);
            statement.setString(3, name);
            statement.setString(4, description);
            statement.setString(5, modelName);
            statement.setInt(6, maxUsers);
            statement.setInt(7, categoryId);
            statement.setInt(8, tradeType);
            statement.execute();
            try (ResultSet set = statement.getGeneratedKeys()) {
                if (set.next())
                    room = this.getRoom(set.getInt(1));
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return room;
    }

    public Room createRoomForHabbo(Habbo habbo, String name, String description, String modelName, int usersMax, int categoryId, int tradeType) {
        Room room = this.createRoom(habbo.getHabboInfo().getId(), habbo.getHabboInfo().getUsername(), name, description, modelName, usersMax, categoryId, tradeType);
        Emulator.getPluginManager().fireEvent(new NavigatorRoomCreatedEvent(habbo, room));
        return room;
    }

    public void loadRoomsForHabbo(Habbo habbo) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms WHERE owner_id = ?")) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (!this.activeRooms.containsKey(set.getInt("id")))
                        this.activeRooms.put(set.getInt("id"), new Room(set));
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }
    public void unloadRoomsForHabbo(Habbo habbo) {
        List<Room> roomsToDispose = new ArrayList<>();

        for (Room room : this.activeRooms.values()) {
            RoomInfo roomInfo = room.getRoomInfo();
            RoomUnitManager roomUnitManager = room.getRoomUnitManager();
            RoomCategory roomCategory = roomInfo.getCategory();

            if (roomInfo.isPublicRoom() || roomInfo.isStaffPicked())
                continue;

            if (roomInfo.getOwnerInfo().getId() != habbo.getHabboInfo().getId())
                continue;

            if (roomUnitManager.getRoomHabbosCount() != 0)
                continue;

            if (!roomCategories.containsKey(roomCategory) ||
                    (roomCategories.get(roomCategory.getId()) != null && roomCategories.get(roomCategory.getId()).isPublic()))
                continue;

            roomsToDispose.add(room);
        }

        for (Room room : roomsToDispose) {
            if (!Emulator.getPluginManager().fireEvent(new RoomUncachedEvent(room)).isCancelled()) {
                room.dispose();
                this.activeRooms.remove(room.getRoomInfo().getId());
            }
        }
    }

    public void clearInactiveRooms() {
        THashSet<Room> roomsToDispose = new THashSet<>();
        for (Room room : this.activeRooms.values()) {
            if (!room.getRoomInfo().isPublicRoom()) {
                if (!room.getRoomInfo().isStaffPicked() && !Emulator.getGameServer().getGameClientManager().containsHabbo(room.getRoomInfo().getOwnerInfo().getId()) && room.isPreLoaded()) {
                    roomsToDispose.add(room);
                }
            }
        }

        for (Room room : roomsToDispose) {
            room.dispose();
            if (room.getRoomUnitManager().getRoomHabbosCount() == 0)
                this.activeRooms.remove(room.getRoomInfo().getId());
        }
    }

    public boolean layoutExists(String name) {
        return this.mapNames.contains(name);
    }

    public RoomLayout loadLayout(String name, Room room) {
        RoomLayout layout = null;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM room_models WHERE name LIKE ? LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    layout = new RoomLayout(set, room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return layout;
    }

    public void uncacheRoom(Room room) {
        this.activeRooms.remove(room.getRoomInfo().getId());
    }

    public void voteForRoom(Habbo habbo, Room room) {
        if (habbo.getRoomUnit().getRoom() != null && room != null && habbo.getRoomUnit().getRoom() == room) {
            if (this.hasVotedForRoom(habbo, room))
                return;

            UserVoteRoomEvent event = new UserVoteRoomEvent(room, habbo);
            if (Emulator.getPluginManager().fireEvent(event).isCancelled()) return;

            room.getRoomInfo().setScore(room.getRoomInfo().getScore() + 1);
            room.setNeedsUpdate(true);
            habbo.getHabboStats().getVotedRooms().push(room.getRoomInfo().getId());
            for (Habbo h : room.getRoomUnitManager().getCurrentHabbos().values()) {
                h.getClient().sendResponse(new RoomRatingComposer(room.getRoomInfo().getScore(), !this.hasVotedForRoom(h, room)));
            }

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO room_votes VALUES (?, ?)")) {
                statement.setInt(1, habbo.getHabboInfo().getId());
                statement.setInt(2, room.getRoomInfo().getId());
                statement.execute();
            } catch (SQLException e) {
                log.error(CAUGHT_SQL_EXCEPTION, e);
            }
        }
    }

    boolean hasVotedForRoom(Habbo habbo, Room room) {
        if (room.getRoomInfo().getOwnerInfo().getId() == habbo.getHabboInfo().getId())
            return true;

        for (int i : habbo.getHabboStats().getVotedRooms().toArray()) {
            if (i == room.getRoomInfo().getId())
                return true;
        }

        return false;
    }

    public Room getActiveRoomById(int roomId) {
        return this.activeRooms.get(roomId);
    }

    public List<Room> getActiveRooms() {
        return new ArrayList<>(this.activeRooms.values());
    }

    public int loadedRoomsCount() {
        return this.activeRooms.size();
    }

    public void enterRoom(Habbo habbo, int roomId, String password, boolean forceEnter) {
        this.enterRoom(habbo, roomId, password, forceEnter, null);
    }

    public void enterRoom(Habbo habbo, int roomId, String password, boolean forceEnter, RoomTile spawnLocation) {
        Room room = this.getRoom(roomId, true);

        if (habbo.getHabboRoleplayStats().isDead()) {
            return;
        }

        if (room == null) {
            log.error("User (ID: {}) is trying to enter a corrupted room (ID: {})", habbo.getHabboInfo().getId(), roomId);
            return;
        }

        if (habbo.getRoomUnit().isLoadingRoom() && room.getRoomInfo().getId() != habbo.getRoomUnit().getLoadingRoom().getRoomInfo().getId()) {
            habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
            habbo.getRoomUnit().setLoadingRoom(null);
            return;
        }

        //Fire Plugin Event
        if (Emulator.getPluginManager().fireEvent(new UserEnterRoomEvent(habbo, room)).isCancelled() && habbo.getRoomUnit().getRoom() == null) {
            habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
            habbo.getRoomUnit().setLoadingRoom(null);
            return;
        }

        //If Habbo is banned AND doesn't have Permissions can't enter to room
        if (room.getRoomInfractionManager().isBanned(habbo) && !room.getRoomInfo().isRoomOwner(habbo) && !habbo.hasPermissionRight(Permission.ACC_ENTERANYROOM)) {
            habbo.getClient().sendResponse(new CantConnectMessageComposer(CantConnectMessageComposer.ROOM_ERROR_BANNED));
            return;
        }

        //If room is full AND user doesn't have Permissions can't enter to room
        if (room.getRoomUnitManager().getRoomHabbosCount() >= room.getRoomInfo().getMaxUsers() && !room.getRoomRightsManager().hasRights(habbo) && !habbo.hasPermissionRight(Permission.ACC_FULLROOMS)) {
            habbo.getClient().sendResponse(new CantConnectMessageComposer(CantConnectMessageComposer.ROOM_ERROR_GUESTROOM_FULL));
            return;
        }

        if (habbo.getHabboInfo().getRoomQueueId() != roomId) {
            Room queRoom = Emulator.getGameEnvironment().getRoomManager().getActiveRoomById(roomId);

            if (queRoom != null) {
                queRoom.removeFromQueue(habbo);
            }
        }

        /**
         * If override checks open room
         * If habbo is owner open room
         * If room state is 'OPEN' open room
         * If habbo has permissions open room
         * If habbo has guild rights open room
         */
        if (forceEnter || room.getRoomInfo().isRoomOwner(habbo) || room.getRoomInfo().getState() == RoomState.OPEN || habbo.hasPermissionRight(Permission.ACC_ENTERANYROOM) || room.getRoomRightsManager().hasRights(habbo) || (room.getRoomInfo().getState().equals(RoomState.INVISIBLE) && room.getRoomRightsManager().hasRights(habbo)) || (room.getRoomInfo().hasGuild() && room.getGuildRightLevel(habbo).isGreaterThan(RoomRightLevels.GUILD_RIGHTS))) {
            this.openRoom(habbo, room, spawnLocation);
        } else if (room.getRoomInfo().getState() == RoomState.LOCKED) {
            boolean habbosWithRights = false;

            synchronized (room.getRoomUnitManager().roomUnitLock) {
                for (Habbo current : room.getRoomUnitManager().getCurrentHabbos().values()) {
                    if (room.getRoomRightsManager().hasRights(current) || current.getHabboInfo().getId() == room.getRoomInfo().getOwnerInfo().getId() || (room.getRoomInfo().hasGuild() && room.getGuildRightLevel(current).isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS))) {
                        current.getClient().sendResponse(new DoorbellMessageComposer(habbo.getHabboInfo().getUsername()));
                        habbosWithRights = true;
                    }
                }
            }

            if (!habbosWithRights) {
                habbo.getClient().sendResponse(new FlatAccessDeniedMessageComposer(""));
                habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
                habbo.getRoomUnit().setLoadingRoom(null);
                return;
            }

            habbo.getHabboInfo().setRoomQueueId(roomId);
            habbo.getClient().sendResponse(new DoorbellMessageComposer(""));
            room.addToQueue(habbo);
        } else if (room.getRoomInfo().getState() == RoomState.PASSWORD) {
            if (room.getRoomInfo().getPassword().equalsIgnoreCase(password)) {
                this.openRoom(habbo, room, spawnLocation);
            } else {
                habbo.getClient().sendResponse(new GenericErrorComposer(GenericErrorComposer.WRONG_PASSWORD_USED));
                habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
                habbo.getRoomUnit().setLoadingRoom(null);
            }
        } else {
            habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
            habbo.getRoomUnit().setLoadingRoom(null);
        }

        if (CorpShiftManager.getInstance().isUserWorking(habbo) && !habbo.getHabboRoleplayStats().getCorpPosition().isCanWorkAnywhere()) {
            CorpShiftManager.getInstance().stopUserShift(habbo, false, false);
        }

        habbo.getHabboInfo().setHomeRoom(room.getRoomInfo().getId());
    }

    void openRoom(Habbo habbo, Room room, RoomTile spawnLocation) {
        if (room.getLayout() == null) {
            return;
        }

        if (Emulator.getConfig().getBoolean("hotel.room.enter.logs")) {
            this.logEnter(habbo, room);
        }

        if (habbo.getHabboInfo().getRoomQueueId() > 0) {
            Room queuedRoom = this.getActiveRoomById(habbo.getHabboInfo().getRoomQueueId());

            if (queuedRoom != null) {
                queuedRoom.removeFromQueue(habbo);
            }
        }

        habbo.getHabboInfo().setRoomQueueId(0);
        habbo.getClient().sendResponse(new FlatAccessibleMessageComposer(""));

        RoomHabbo roomHabbo = habbo.getRoomUnit();

        roomHabbo.clear();

        if (roomHabbo.getCurrentPosition() == null) {
            RoomTile spawnTile = spawnLocation == null ? room.getLayout().getDoorTile() : spawnLocation;
            RoomRotation spawnDirection = RoomRotation.values()[room.getLayout().getDoorDirection()];

            if (spawnLocation != null) {
                roomHabbo.setCanLeaveRoomByDoor(false);
                roomHabbo.setTeleporting(true);
                RoomItem topItem = room.getRoomItemManager().getTopItemAt(spawnLocation);

                if (topItem != null) {
                    spawnDirection = RoomRotation.values()[topItem.getRotation()];
                }
            }

            this.handleSpawnLocation(roomHabbo, spawnTile, spawnDirection);
        }

        habbo.getClient().sendResponse(new OpenConnectionMessageComposer());

        if (!habbo.getHabboStats().isBlockFollowing() && roomHabbo.getRoom() == null) {
            habbo.getMessenger().connectionChanged(habbo, true, true);
        }

        if (roomHabbo.isLoadingRoom()) {
            roomHabbo.getLoadingRoom().removeFromQueue(habbo);
            roomHabbo.setLoadingRoom(null);
        }

        roomHabbo.setLoadingRoom(room);

        habbo.getClient().sendResponse(new RoomReadyMessageComposer(room));

        if (!room.getRoomInfo().getWallPaint().equals("0.0"))
            habbo.getClient().sendResponse(new RoomPropertyMessageComposer("wallpaper", room.getRoomInfo().getWallPaint()));

        if (!room.getRoomInfo().getFloorPaint().equals("0.0"))
            habbo.getClient().sendResponse(new RoomPropertyMessageComposer("floor", room.getRoomInfo().getFloorPaint()));

        habbo.getClient().sendResponse(new RoomPropertyMessageComposer("landscape", room.getRoomInfo().getLandscapePaint()));

        room.getRoomRightsManager().refreshRightsForHabbo(habbo);

        habbo.getClient().sendResponse(new RoomRatingComposer(room.getRoomInfo().getScore(), !this.hasVotedForRoom(habbo, room)));

        roomHabbo.setCmdFastWalkEnabled(roomHabbo.isCmdFastWalkEnabled() && habbo.canExecuteCommand("cmd_fastwalk", room.getRoomRightsManager().hasRights(habbo)));

        if (room.getRoomInfo().isPromoted()) {
            habbo.getClient().sendResponse(new RoomEventComposer(room, room.getRoomPromotionManager().getPromotion()));
        } else {
            habbo.getClient().sendResponse(new RoomEventComposer(null, null));
        }

        if (!room.getRoomInfo().isRoomOwner(habbo)) {
            if (!habbo.getHabboStats().visitedRoom(room.getRoomInfo().getId())) {
                AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("RoomEntry"));
            }
        }
    }

    public void enterRoom(final Habbo habbo, final Room room) {
        if (habbo.getRoomUnit().isLoadingRoom() && room.getRoomInfo().getId() != habbo.getRoomUnit().getLoadingRoom().getRoomInfo().getId()) {
            habbo.getClient().sendResponse(new CloseConnectionMessageComposer());
            habbo.getRoomUnit().setLoadingRoom(null);
            return;
        }

        RoomHabbo roomHabbo = habbo.getRoomUnit();

        roomHabbo.removeStatus(RoomUnitStatus.FLAT_CONTROL);
        roomHabbo.setHandItem(0);
        roomHabbo.setRightsLevel(RoomRightLevels.NONE);
        roomHabbo.setRoom(room);
        roomHabbo.setLoadingRoom(null);

        room.getRoomRightsManager().refreshRightsForHabbo(habbo);

        if (habbo.getRoomUnit().isKicked() && !habbo.getRoomUnit().isCanWalk()) {
            habbo.getRoomUnit().setCanWalk(true);
        }

        roomHabbo.setKicked(false);

        if (roomHabbo.getCurrentPosition() == null && !habbo.getRoomUnit().isTeleporting()) {
            RoomTile doorTile = room.getLayout().getDoorTile();
            RoomRotation doorDirection = RoomRotation.values()[room.getLayout().getDoorDirection()];

            if (doorTile != null) {
                this.handleSpawnLocation(roomHabbo, doorTile, doorDirection);
            }
        }

        roomHabbo.resetIdleTicks();
        roomHabbo.setInvisible(false);

        room.getRoomUnitManager().addRoomUnit(habbo);

        List<Habbo> visibleHabbos = new ArrayList<>();

        if (!room.getRoomUnitManager().getCurrentHabbos().values().isEmpty()) {
            Collection<Habbo> habbosToSendEnter = room.getRoomUnitManager().getCurrentHabbos().values();
            Collection<Habbo> allHabbos = room.getRoomUnitManager().getCurrentHabbos().values();

            if (Emulator.getPluginManager().isRegistered(HabboAddedToRoomEvent.class, false)) {
                HabboAddedToRoomEvent event = Emulator.getPluginManager().fireEvent(new HabboAddedToRoomEvent(habbo, room, habbosToSendEnter, allHabbos));
                habbosToSendEnter = event.getHabbosToSendEnter();
                allHabbos = event.getVisibleHabbos();
            }

            habbosToSendEnter.stream().map(Habbo::getClient).filter(Objects::nonNull).forEach(client -> {
                client.sendResponse(new RoomUsersComposer(habbo).compose());
                client.sendResponse(new UserUpdateComposer(habbo.getRoomUnit()).compose());
            });

            visibleHabbos = allHabbos.stream().filter(h -> !h.getRoomUnit().isInvisible()).toList();

            synchronized (room.getRoomUnitManager().roomUnitLock) {
                habbo.getClient().sendResponse(new RoomUsersComposer(visibleHabbos));
                habbo.getClient().sendResponse(new UserUpdateComposer(visibleHabbos));
            }

            if (habbo.getHabboStats().getGuild() != 0) {
                Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(habbo.getHabboStats().getGuild());

                if (guild != null) {
                    room.sendComposer(new HabboAddGroupBadgesMessageComposer(guild).compose());
                }
            }

            int effect = habbo.getInventory().getEffectsComponent().getActivatedEffect();
            habbo.getRoomUnit().giveEffect(effect, -1);
        }


        habbo.getClient().sendResponse(new RoomUsersComposer(room.getRoomUnitManager().getRoomBotManager().getCurrentBots().values()));

        if (!room.getRoomUnitManager().getRoomBotManager().getCurrentBots().isEmpty()) {
            room.getRoomUnitManager().getRoomBotManager().getCurrentBots().values().stream()
                    .filter(b -> !b.getRoomUnit().getDanceType().equals(DanceType.NONE))
                    .forEach(b -> habbo.getClient().sendResponse(new DanceMessageComposer(b.getRoomUnit())));

            room.getRoomUnitManager().getRoomBotManager().getCurrentBots().values()
                    .forEach(b -> habbo.getClient().sendResponse(new UserUpdateComposer(b.getRoomUnit(), b.getRoomUnit().getCurrentZ())));
        }

        habbo.getClient().sendResponse(new RoomEntryInfoMessageComposer(room, room.getRoomInfo().isRoomOwner(habbo)));
        habbo.getClient().sendResponse(new RoomVisualizationSettingsComposer(room));
        habbo.getClient().sendResponse(new GetGuestRoomResultComposer(room, habbo.getClient().getHabbo(), false, true));

        habbo.getClient().sendResponse(new ItemsComposer(room));

        final THashSet<RoomItem> floorItems = new THashSet<>();

        THashSet<RoomItem> allFloorItems = new THashSet<>(room.getRoomItemManager().getFloorItems().values());

        if (Emulator.getPluginManager().isRegistered(RoomFloorItemsLoadEvent.class, true)) {
            RoomFloorItemsLoadEvent roomFloorItemsLoadEvent = Emulator.getPluginManager().fireEvent(new RoomFloorItemsLoadEvent(habbo, allFloorItems));
            if (roomFloorItemsLoadEvent.hasChangedFloorItems()) {
                allFloorItems = roomFloorItemsLoadEvent.getFloorItems();
            }
        }

        allFloorItems.forEach(object -> {
            if (room.getRoomInfo().isHiddenWiredEnabled() && object instanceof InteractionWired)
                return true;

            floorItems.add(object);
            if (floorItems.size() == 250) {
                habbo.getClient().sendResponse(new ObjectsMessageComposer(room.getFurniOwnerNames(), floorItems));
                floorItems.clear();
            }

            return true;
        });

        habbo.getClient().sendResponse(new ObjectsMessageComposer(room.getFurniOwnerNames(), floorItems));
        floorItems.clear();

        if (!room.getRoomUnitManager().getRoomPetManager().getCurrentPets().isEmpty()) {
            habbo.getClient().sendResponse(new RoomPetComposer(room.getRoomUnitManager().getRoomPetManager().getCurrentPets()));
            room.getRoomUnitManager().getRoomPetManager().getCurrentPets().values().forEach(pet -> habbo.getClient().sendResponse(new UserUpdateComposer(pet.getRoomUnit())));
        }

        if (!habbo.getHabboStats().allowTalk()) {
            habbo.getHabboStats().setMutedBubbleTracker(true);
            int remainingMuteTime = habbo.getHabboStats().remainingMuteTime();
            habbo.getClient().sendResponse(new FloodControlMessageComposer(remainingMuteTime));
            habbo.getClient().sendResponse(new RemainingMutePeriodComposer(remainingMuteTime));
            room.sendComposer(new IgnoreResultMessageComposer(habbo, IgnoreResultMessageComposer.MUTED).compose());
        } else if (habbo.getHabboStats().isMutedBubbleTracker()) {
            habbo.getHabboStats().setMutedBubbleTracker(false);
        }

        THashMap<Integer, String> guildBadges = new THashMap<>();
        for (Habbo visibleHabbo : visibleHabbos) {
            if (visibleHabbo.getRoomUnit().getDanceType().getType() > 0) {
                habbo.getClient().sendResponse(new DanceMessageComposer(visibleHabbo.getRoomUnit()));
            }

            if (visibleHabbo.getRoomUnit().getHandItem() > 0) {
                habbo.getClient().sendResponse(new CarryObjectMessageComposer(visibleHabbo.getRoomUnit()));
            }

            if (visibleHabbo.getRoomUnit().getEffectId() > 0) {
                habbo.getClient().sendResponse(new AvatarEffectMessageComposer(visibleHabbo.getRoomUnit()));
            }

            if (visibleHabbo.getRoomUnit().isIdle()) {
                habbo.getClient().sendResponse(new SleepMessageComposer(visibleHabbo.getRoomUnit()));
            }

            if (visibleHabbo.getHabboStats().userIgnored(habbo.getHabboInfo().getId())) {
                visibleHabbo.getClient().sendResponse(new IgnoreResultMessageComposer(habbo, IgnoreResultMessageComposer.IGNORED));
            }

            if (!visibleHabbo.getHabboStats().allowTalk()) {
                habbo.getClient().sendResponse(new IgnoreResultMessageComposer(visibleHabbo, IgnoreResultMessageComposer.MUTED));
            } else if (habbo.getHabboStats().userIgnored(visibleHabbo.getHabboInfo().getId())) {
                habbo.getClient().sendResponse(new IgnoreResultMessageComposer(visibleHabbo, IgnoreResultMessageComposer.IGNORED));
            }

            if (visibleHabbo.getHabboStats().getGuild() != 0 && !guildBadges.containsKey(visibleHabbo.getHabboStats().getGuild())) {
                Guild guild = Emulator.getGameEnvironment().getGuildManager().getGuild(visibleHabbo.getHabboStats().getGuild());

                if (guild != null) {
                    guildBadges.put(visibleHabbo.getHabboStats().getGuild(), guild.getBadge());
                }
            }

            if (visibleHabbo.getRoomUnit().getRoomUnitType().equals(RoomUnitType.PET)) {
                try {
                    habbo.getClient().sendResponse(new UserRemoveMessageComposer(visibleHabbo.getRoomUnit()));
                    habbo.getClient().sendResponse(new RoomUserPetComposer(((PetData) visibleHabbo.getHabboStats().getCache().get("pet_type")).getType(), (Integer) visibleHabbo.getHabboStats().getCache().get("pet_race"), (String) visibleHabbo.getHabboStats().getCache().get("pet_color"), visibleHabbo));
                } catch (Exception ignored) {

                }
            }
        }

        habbo.getClient().sendResponse(new HabboGroupBadgesMessageComposer(guildBadges));

        if ((room.getRoomRightsManager().hasRights(habbo)
                || (room.getRoomInfo().hasGuild()
                && room.getGuildRightLevel(habbo).isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS)))
                && !room.getHabboQueue().isEmpty()) {
            for (Habbo waiting : room.getHabboQueue().valueCollection()) {
                habbo.getClient().sendResponse(new DoorbellMessageComposer(waiting.getHabboInfo().getUsername()));
            }
        }

        if (room.getRoomInfo().getPollId() > 0) {
            if (!PollManager.donePoll(habbo.getClient().getHabbo(), room.getRoomInfo().getPollId())) {
                Poll poll = Emulator.getGameEnvironment().getPollManager().getPoll(room.getRoomInfo().getPollId());

                if (poll != null) {
                    habbo.getClient().sendResponse(new PollOfferComposer(poll));
                }
            }
        }

        room.getRoomWordQuizManager().onHabboEntered(habbo);

        WiredHandler.handle(WiredTriggerType.ENTER_ROOM, habbo.getRoomUnit(), room, null);
        room.habboEntered(habbo);

        if (!habbo.getHabboStats().isNux()) {
            if (room.getRoomInfo().isRoomOwner(habbo) || room.getRoomInfo().isPublicRoom()) {
                NewUserExperienceScriptProceedEvent.handle(habbo);
            }
        }
    }


    void logEnter(Habbo habbo, Room room) {
        habbo.getHabboStats().roomEnterTimestamp = Emulator.getIntUnixTimestamp();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO room_enter_log (room_id, user_id, timestamp) VALUES(?, ?, ?)")) {
            statement.setInt(1, room.getRoomInfo().getId());
            statement.setInt(2, habbo.getHabboInfo().getId());
            statement.setInt(3, (int) (habbo.getHabboStats().getRoomEnterTimestamp()));
            statement.execute();

            if (!habbo.getHabboStats().visitedRoom(room.getRoomInfo().getId()))
                habbo.getHabboStats().addVisitRoom(room.getRoomInfo().getId());
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }
    }

    public void leaveRoom(Habbo habbo, Room room) {
        if (habbo.getRoomUnit().getRoom() != null && habbo.getRoomUnit().getRoom().equals(room)) {
            this.logExit(habbo);
            room.getRoomUnitManager().removeHabbo(habbo, true);

            habbo.getRoomUnit().setPreviousRoom(room);

            if (!room.getRoomInfo().isRoomOwner(habbo)) {
                AchievementManager.progressAchievement(room.getRoomInfo().getOwnerInfo().getId(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("RoomDecoHosting"), (int) Math.floor((Emulator.getIntUnixTimestamp() - habbo.getHabboStats().roomEnterTimestamp) / 60000.0));
            }

            habbo.getMessenger().connectionChanged(habbo, habbo.isOnline(), false);
        }
    }

    public void logExit(Habbo habbo) {
        Emulator.getPluginManager().fireEvent(new UserExitRoomEvent(habbo, UserExitRoomEvent.UserExitRoomReason.DOOR));
        if (habbo.getRoomUnit().getCacheable().containsKey("control")) {
            Habbo control = (Habbo) habbo.getRoomUnit().getCacheable().remove("control");
            control.getRoomUnit().getCacheable().remove("controller");
        }

        if (habbo.getRoomUnit().isRiding()) {
            if (habbo.getRoomUnit().getRidingPet().getRoomUnit() != null) {
                habbo.getRoomUnit().getRidingPet().getRoomUnit().walkTo(habbo.getRoomUnit().getRidingPet().getRoomUnit().getCurrentPosition());
            }
            habbo.getRoomUnit().getRidingPet().setTask(PetTasks.FREE);
            habbo.getRoomUnit().getRidingPet().setRider(null);
            habbo.getRoomUnit().setRidingPet(null);
        }

        Room room = habbo.getRoomUnit().getRoom();
        if (room != null) {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE room_enter_log SET exit_timestamp = ? WHERE user_id = ? AND room_id = ? ORDER BY timestamp DESC LIMIT 1")) {
                statement.setInt(1, Emulator.getIntUnixTimestamp());
                statement.setInt(2, habbo.getHabboInfo().getId());
                statement.setInt(3, room.getRoomInfo().getId());
                statement.execute();
            } catch (SQLException e) {
                log.error(CAUGHT_SQL_EXCEPTION, e);
            }
        }
    }

    public Set<String> getTags() {
        Map<String, Integer> tagCount = new HashMap<>();

        for (Room room : this.activeRooms.values()) {
            for (String s : room.getRoomInfo().getTags().split(";")) {
                int i = 0;
                if (tagCount.get(s) != null)
                    i++;

                tagCount.put(s, i++);
            }
        }
        return new TreeMap<>(tagCount).keySet();
    }

    public ArrayList<Room> getPublicRooms() {
        return this.activeRooms.values().stream().filter(room -> room.getRoomInfo().isPublicRoom()).sorted(RoomConfiguration.SORT_ID).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> getPopularRooms(int count) {
        return this.activeRooms.values().stream()
                .filter(room -> room.getRoomUnitManager().getRoomHabbosCount() > 0 && (!room.getRoomInfo().isPublicRoom() || RoomManager.SHOW_PUBLIC_IN_POPULAR_TAB))
                .sorted()
                .limit(count)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Room> getPopularRooms(int count, int category) {
        return this.activeRooms.values().stream()
                .filter(room -> !room.getRoomInfo().isPublicRoom() && room.getRoomInfo().getCategory().getId() == category)
                .sorted()
                .limit(count)
                .toList();
    }

    public Map<Integer, List<Room>> getPopularRoomsByCategory(int count) {
        Map<Integer, List<Room>> rooms = new HashMap<>();

        for (Room room : this.activeRooms.values()) {
            if (!room.getRoomInfo().isPublicRoom()) {
                if (room.getRoomInfo().getCategory() == null) {
                    continue;
                }
                if (!rooms.containsKey(room.getRoomInfo().getCategory().getId())) {
                    rooms.put(room.getRoomInfo().getCategory().getId(), new ArrayList<>());
                }

                rooms.get(room.getRoomInfo().getCategory().getId()).add(room);
            }
        }

        Map<Integer, List<Room>> result = new HashMap<>();

        for (Map.Entry<Integer, List<Room>> set : rooms.entrySet()) {
            if (set.getValue().isEmpty())
                continue;

            Collections.sort(set.getValue());

            result.put(set.getKey(), new ArrayList<>(set.getValue().subList(0, (Math.min(set.getValue().size(), count)))));
        }

        return result;
    }

    public List<Room> getRoomsWithName(String name) {
        List<Room> rooms = new ArrayList<>(activeRooms.values().stream().filter(room -> room.getRoomInfo().getName().equalsIgnoreCase(name)).toList());

        if (rooms.size() < 25) {
            rooms.addAll(this.getOfflineRoomsWithName(name));
        }

        Collections.sort(rooms);

        return rooms;
    }

    private ArrayList<Room> getOfflineRoomsWithName(String name) {
        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT users.username AS owner_name, rooms.* FROM rooms INNER JOIN users ON owner_id = users.id WHERE name LIKE ? ORDER BY id DESC LIMIT 25")) {
            statement.setString(1, "%" + name + "%");
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id")))
                        continue;

                    Room r = new Room(set);
                    rooms.add(r);
                    this.activeRooms.put(r.getRoomInfo().getId(), r);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public List<Room> getRoomsWithTag(String tag) {
        ArrayList<Room> rooms = new ArrayList<>();

        for (Room room : this.activeRooms.values()) {
            for (String s : room.getRoomInfo().getTags().split(";")) {
                if (s.equalsIgnoreCase(tag)) {
                    rooms.add(room);
                    break;
                }
            }
        }

        Collections.sort(rooms);

        return rooms;
    }

    public List<Room> getGroupRoomsWithName(String name) {
        ArrayList<Room> rooms = new ArrayList<>();

        for (Room room : this.activeRooms.values()) {
            if (room.getRoomInfo().getGuild() == null)
                continue;

            if (room.getRoomInfo().getName().toLowerCase().contains(name.toLowerCase()))
                rooms.add(room);
        }

        if (rooms.size() < 25) {
            rooms.addAll(this.getOfflineGroupRoomsWithName(name));
        }

        Collections.sort(rooms);

        return rooms;
    }

    private ArrayList<Room> getOfflineGroupRoomsWithName(String name) {
        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT users.username AS owner_name, rooms.* FROM rooms INNER JOIN users ON rooms.owner_id = users.id WHERE name LIKE ? AND guild_id != 0 ORDER BY id DESC LIMIT 25")) {
            statement.setString(1, "%" + name + "%");
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id")))
                        continue;

                    Room r = new Room(set);
                    rooms.add(r);

                    this.activeRooms.put(r.getRoomInfo().getId(), r);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public List<Room> getRoomsFriendsNow(Habbo habbo) {
        ArrayList<Room> rooms = new ArrayList<>();

        for (MessengerBuddy buddy : habbo.getMessenger().getFriends().values()) {
            if (buddy.getOnline() == 0)
                continue;

            Habbo friend = Emulator.getGameEnvironment().getHabboManager().getHabbo(buddy.getId());
            if (friend == null || friend.getRoomUnit().getRoom() == null)
                continue;

            rooms.add(friend.getRoomUnit().getRoom());
        }

        Collections.sort(rooms);

        return rooms;
    }

    public List<Room> getRoomsFriendsOwn(Habbo habbo) {
        ArrayList<Room> rooms = new ArrayList<>();

        for (MessengerBuddy buddy : habbo.getMessenger().getFriends().values()) {
            if (buddy.getOnline() == 0)
                continue;

            Habbo friend = Emulator.getGameEnvironment().getHabboManager().getHabbo(buddy.getId());

            if (friend == null)
                continue;

            rooms.addAll(this.getRoomsForHabbo(friend));
        }

        Collections.sort(rooms);

        return rooms;
    }

    public ArrayList<Room> getRoomsVisited(Habbo habbo, boolean includeSelf, int limit) {
        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT rooms.* FROM room_enter_log INNER JOIN rooms ON room_enter_log.room_id = rooms.id WHERE user_id = ? AND timestamp >= ? AND rooms.owner_id != ? GROUP BY rooms.id AND timestamp ORDER BY timestamp DESC LIMIT " + limit)) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            statement.setInt(2, Emulator.getIntUnixTimestamp() - 259200);
            statement.setInt(3, (includeSelf ? 0 : habbo.getHabboInfo().getId()));
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    Room room = this.activeRooms.get(set.getInt("id"));

                    if (room == null) {
                        room = new Room(set);

                        this.activeRooms.put(room.getRoomInfo().getId(), room);
                    }

                    rooms.add(room);
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        Collections.sort(rooms);

        return rooms;
    }

    public ArrayList<Room> getRoomsFavourite(Habbo habbo) {
        final ArrayList<Room> rooms = new ArrayList<>();

        habbo.getHabboStats().getFavoriteRooms().forEach(value -> {
            Room room = RoomManager.this.getActiveRoomById(value);

            if (room != null) {
                if (room.getRoomInfo().getState() == RoomState.INVISIBLE) {
                    room.loadData();
                    if (!room.getRoomRightsManager().hasRights(habbo)) return true;
                }
                rooms.add(room);
            }
            return true;
        });

        return rooms;
    }

    public List<Room> getGroupRooms(Habbo habbo, int limit) {
        final ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT rooms.* FROM rooms INNER JOIN guilds_members ON guilds_members.guild_id = rooms.guild_id WHERE guilds_members.user_id = ? AND level_id != 3")) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id"))) {
                        rooms.add(this.activeRooms.get(set.getInt("id")));
                    } else {
                        rooms.add(new Room(set));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        Collections.sort(rooms);

        return rooms.subList(0, (Math.min(rooms.size(), limit)));
    }

    public List<Room> getRoomsWithRights(Habbo habbo) {
        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT rooms.* FROM rooms INNER JOIN room_rights ON room_rights.room_id = rooms.id WHERE room_rights.user_id = ? ORDER BY rooms.id DESC LIMIT 30")) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id"))) {
                        rooms.add(this.activeRooms.get(set.getInt("id")));
                    } else {
                        rooms.add(new Room(set));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public ArrayList<Room> getRoomsWithFriendsIn(Habbo habbo, int limit) {
        final ArrayList<Room> rooms = new ArrayList<>();

        for (MessengerBuddy buddy : habbo.getMessenger().getFriends().values()) {
            Habbo friend = Emulator.getGameEnvironment().getHabboManager().getHabbo(buddy.getId());

            if (friend == null || friend.getHabboInfo() == null) continue;

            Room room = friend.getRoomUnit().getRoom();
            if (room != null && !rooms.contains(room) && room.getRoomRightsManager().hasRights(habbo)) rooms.add(room);

            if (rooms.size() >= limit) break;
        }

        Collections.sort(rooms);

        return rooms;
    }

    public ArrayList<Room> getTopRatedRooms(int limit) {
        final ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms ORDER BY score DESC LIMIT ?")) {
            statement.setInt(1, limit);

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id"))) {
                        rooms.add(this.activeRooms.get(set.getInt("id")));
                    } else {
                        rooms.add(new Room(set));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public List<Room> getRoomsWithAdminRights(Habbo habbo) {
        ArrayList<Room> rooms = new ArrayList<>();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM rooms INNER JOIN guilds_members ON guilds_members.guild_id = rooms.guild_id WHERE guilds_members.user_id = ? AND level_id = 0")) {
            statement.setInt(1, habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    if (this.activeRooms.containsKey(set.getInt("id"))) {
                        rooms.add(this.activeRooms.get(set.getInt("id")));
                    } else {
                        rooms.add(new Room(set));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return rooms;
    }

    public List<Room> getRoomsInGroup() {
        return new ArrayList<>();
    }

    public ArrayList<Room> getRoomsPromoted() {
        return activeRooms.values().stream().filter(r -> r.getRoomInfo().isPromoted()).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> getRoomsStaffPromoted() {
        return activeRooms.values().stream().filter(room -> room.getRoomInfo().isStaffPicked()).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> filterRoomsByOwner(List<Room> rooms, String filter) {
        return rooms.stream().filter(r -> r.getRoomInfo().getOwnerInfo().getUsername().equalsIgnoreCase(filter)).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> filterRoomsByName(List<Room> rooms, String filter) {
        return rooms.stream().filter(room -> room.getRoomInfo().getName().toLowerCase().contains(filter.toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> filterRoomsByNameAndDescription(List<Room> rooms, String filter) {
        return rooms.stream().filter(room -> room.getRoomInfo().getName().toLowerCase().contains(filter.toLowerCase()) || room.getRoomInfo().getDescription().toLowerCase().contains(filter.toLowerCase())).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Room> filterRoomsByTag(List<Room> rooms, String filter) {
        ArrayList<Room> r = new ArrayList<>();

        for (Room room : rooms) {
            if (room.getRoomInfo().getTags().split(";").length == 0)
                continue;

            for (String s : room.getRoomInfo().getTags().split(";")) {
                if (s.equalsIgnoreCase(filter))
                    r.add(room);
            }
        }

        return r;
    }

    public ArrayList<Room> filterRoomsByGroup(List<Room> rooms, String filter) {
        return rooms.stream().filter(room -> room.getRoomInfo().getGuild().getId() != 0)
                .filter(room -> Emulator.getGameEnvironment().getGuildManager().getGuild(room.getRoomInfo().getGuild().getId()).getName().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized void dispose() {
        for (Room room : this.activeRooms.values()) {
            room.dispose();
        }

        this.activeRooms.clear();

        log.info("Room Manager -> Disposed!");
    }

    public CustomRoomLayout insertCustomLayout(Room room, String map, int doorX, int doorY, int doorDirection) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO room_models_custom (id, name, door_x, door_y, door_dir, heightmap) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE door_x = ?, door_y = ?, door_dir = ?, heightmap = ?")) {
            statement.setInt(1, room.getRoomInfo().getId());
            statement.setString(2, "custom_" + room.getRoomInfo().getId());
            statement.setInt(3, doorX);
            statement.setInt(4, doorY);
            statement.setInt(5, doorDirection);
            statement.setString(6, map);
            statement.setInt(7, doorX);
            statement.setInt(8, doorY);
            statement.setInt(9, doorDirection);
            statement.setString(10, map);
            statement.execute();
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        }

        return this.loadCustomLayout(room);
    }

    public void banUserFromRoom(Habbo rights, int userId, int roomId, RoomBanTypes length) {
        Room room = this.getActiveRoomById(roomId);

        if (room == null)
            return;

        if (rights != null && !room.getRoomRightsManager().hasRights(rights)) return;

        String name = "";

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        if (habbo != null) {
            if (habbo.hasPermissionRight(Permission.ACC_UNKICKABLE)) {
                return;
            }

            name = habbo.getHabboInfo().getUsername();
        } else {
            HabboInfo info = Emulator.getGameEnvironment().getHabboManager().getOfflineHabboInfo(userId);

            if (info != null) {
                if (info.getPermissionGroup().hasPermissionRight(Permission.ACC_UNKICKABLE, false)) {
                    return;
                }
                name = info.getUsername();
            }
        }

        if (name.isEmpty()) {
            return;
        }

        RoomBan roomBan = new RoomBan(roomId, userId, name, Emulator.getIntUnixTimestamp() + length.duration);
        roomBan.insert();

        room.getRoomInfractionManager().addRoomBan(roomBan);

        if (habbo != null && habbo.getRoomUnit().getRoom() == room) {
            room.getRoomUnitManager().removeHabbo(habbo, true);
            habbo.getClient().sendResponse(new CantConnectMessageComposer(CantConnectMessageComposer.ROOM_ERROR_BANNED));
        }
    }

    public void handleSpawnLocation(RoomHabbo roomHabbo, RoomTile location, RoomRotation direction) {
        roomHabbo.setLocation(location);
        roomHabbo.setCurrentZ(location.getStackHeight());
        roomHabbo.setRotation(direction);
    }

    public void registerGameType(Class<? extends Game> gameClass) {
        gameTypes.add(gameClass);
    }

    public void unregisterGameType(Class<? extends Game> gameClass) {
        gameTypes.remove(gameClass);
    }

    @Getter
    @AllArgsConstructor
    public enum RoomBanTypes {
        RWUAM_BAN_USER_HOUR(60 * 60),
        RWUAM_BAN_USER_DAY(24 * 60 * 60),
        RWUAM_BAN_USER_PERM(10 * 365 * 24 * 60 * 60);

        private final int duration;
    }
}
