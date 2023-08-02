package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.items.ICycleable;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.*;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameGate;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameScoreboard;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiSphere;
import com.eu.habbo.habbohotel.items.interactions.games.battlebanzai.InteractionBattleBanzaiTeleporter;
import com.eu.habbo.habbohotel.items.interactions.games.freeze.InteractionFreezeExitTile;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagField;
import com.eu.habbo.habbohotel.items.interactions.games.tag.InteractionTagPole;
import com.eu.habbo.habbohotel.items.interactions.pets.*;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredBlob;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.entities.items.RoomItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.inventory.FurniListInvalidateComposer;
import com.eu.habbo.messages.outgoing.inventory.UnseenItemsComposer;
import com.eu.habbo.messages.outgoing.rooms.HeightMapUpdateMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.items.*;
import com.eu.habbo.plugin.Event;
import com.eu.habbo.plugin.events.furniture.*;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.eu.habbo.database.DatabaseConstants.CAUGHT_SQL_EXCEPTION;

@Slf4j
@Getter
public class RoomItemManager {
    private final Room room;
    private final RoomWiredManager wiredManager;
    private final ConcurrentHashMap<Integer, RoomItem> currentItems;
    private final ConcurrentHashMap<Integer, RoomItem> floorItems;
    private final ConcurrentHashMap<Integer, RoomItem> wallItems;
    private final HashSet<ICycleable> cycleTasks;
    private final HashMap<Integer, InteractionNest> nests;
    private final HashMap<Integer, InteractionPetDrink> petDrinks;
    private final HashMap<Integer, InteractionPetFood> petFoods;
    private final HashMap<Integer, InteractionPetToy> petToys;
    private final HashMap<Integer, InteractionRoller> rollers;
    private final HashMap<Integer, InteractionGameScoreboard> gameScoreboards;
    private final HashMap<Integer, InteractionGameGate> gameGates;
    private final HashMap<Integer, InteractionGameTimer> gameTimers;
    private final HashMap<Integer, InteractionBattleBanzaiTeleporter> banzaiTeleporters;
    private final HashMap<Integer, InteractionFreezeExitTile> freezeExitTile;
    private final HashMap<Integer, RoomItem> undefinedSpecials;

    public RoomItemManager(Room room) {
        this.room = room;
        this.currentItems = new ConcurrentHashMap<>();
        this.wiredManager = new RoomWiredManager(room);

        this.floorItems = new ConcurrentHashMap<>();
        this.wallItems = new ConcurrentHashMap<>();

        this.cycleTasks = new HashSet<>(0);

        this.nests = new HashMap<>(0);
        this.petDrinks = new HashMap<>(0);
        this.petFoods = new HashMap<>(0);
        this.petToys = new HashMap<>(0);

        this.rollers = new HashMap<>(0);

        this.gameScoreboards = new HashMap<>(0);
        this.gameGates = new HashMap<>(0);
        this.gameTimers = new HashMap<>(0);

        this.banzaiTeleporters = new HashMap<>(0);
        this.freezeExitTile = new HashMap<>(0);

        this.undefinedSpecials = new HashMap<>(0);
    }

    public synchronized void load(Connection connection) {
        this.loadItems(connection);
    }

    private synchronized void loadItems(Connection connection) {
        this.currentItems.clear();
        this.wiredManager.clear();

        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM items WHERE room_id = ?")) {
            statement.setInt(1, this.room.getRoomInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    RoomItem item = Emulator.getGameEnvironment().getItemManager().loadHabboItem(set);

                    if (item != null) {
                        this.addRoomItem(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(CAUGHT_SQL_EXCEPTION, e);
        } catch (Exception e) {
            log.error("Caught Exception", e);
        }
    }

    public RoomItem getRoomItemById(int itemId) {
        return this.currentItems.get(itemId);
    }

    public void addRoomItem(RoomItem item) {
        if (this.currentItems.size() > Room.MAXIMUM_FURNI) {
            log.error("Room ID: {} has exceeded the furniture limit ({} > {}).", this.room.getRoomInfo().getId(), this.currentItems.size(), Room.MAXIMUM_FURNI);
        }

        synchronized (this.currentItems) {
            try {
                this.currentItems.put(item.getId(), item);
                this.sortItem(item);
                //Deprecated
                item.setRoomId(this.room.getRoomInfo().getId());
                item.setRoom(room);
            } catch (Exception ignored) {

            }
        }
    }

    public void removeRoomItem(RoomItem item) {
        if (item != null) {

            RoomItem i;
            synchronized (this.currentItems) {
                i = this.currentItems.remove(item.getId());
            }

            if (i != null) {
                if (item instanceof ICycleable) {
                    this.removeCycleTask((ICycleable) item);
                }

                if (item instanceof InteractionWired wired) {
                    this.wiredManager.removeWired(wired);
                } else if (item instanceof InteractionBattleBanzaiTeleporter) {
                    this.removeBanzaiTeleporter((InteractionBattleBanzaiTeleporter) item);
                } else if (item instanceof InteractionRoller) {
                    this.removeRoller((InteractionRoller) item);
                } else if (item instanceof InteractionGameScoreboard) {
                    this.removeScoreboard((InteractionGameScoreboard) item);
                } else if (item instanceof InteractionGameGate) {
                    this.removeGameGate((InteractionGameGate) item);
                } else if (item instanceof InteractionGameTimer) {
                    this.removeGameTimer((InteractionGameTimer) item);
                } else if (item instanceof InteractionFreezeExitTile) {
                    this.removeFreezeExitTile((InteractionFreezeExitTile) item);
                } else if (item instanceof InteractionNest) {
                    this.removeNest((InteractionNest) item);
                } else if (item instanceof InteractionPetDrink) {
                    this.removePetDrink((InteractionPetDrink) item);
                } else if (item instanceof InteractionPetFood) {
                    this.removePetFood((InteractionPetFood) item);
                } else if (item instanceof InteractionPetToy) {
                    this.removePetToy((InteractionPetToy) item);
                } else if (item instanceof InteractionPetTree) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionPetTrampoline) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionMoodLight) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionPyramid) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionMusicDisc) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionBattleBanzaiSphere) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionTalkingFurniture) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionWaterItem) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionWater) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionMuteArea) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionTagPole) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionTagField) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionJukeBox) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionPetBreedingNest) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionBlackHole) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionWiredHighscore) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionStickyPole) {
                    this.removeUndefined(item);
                } else if (item instanceof WiredBlob) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionTent) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionSnowboardSlope) {
                    this.removeUndefined(item);
                } else if (item instanceof InteractionBuildArea) {
                    this.removeUndefined(item);
                }
            }
        }
    }

    public HashSet<RoomItem> getItemsOfType(Class<? extends RoomItem> type) {
        return this.currentItems.values().stream()
                .filter(item -> type.equals(item.getClass()))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public FurnitureMovementError canPlaceFurnitureAt(RoomItem item, Habbo habbo, RoomTile tile, int rotation) {
        if (this.currentItems.size() >= Room.MAXIMUM_FURNI) {
            return FurnitureMovementError.MAX_ITEMS;
        } else if (item instanceof InteractionMoodLight && !this.getItemsOfType(InteractionMoodLight.class).isEmpty()) {
            return FurnitureMovementError.MAX_DIMMERS;
        } else if (item instanceof InteractionJukeBox && !this.getItemsOfType(InteractionJukeBox.class).isEmpty()) {
            return FurnitureMovementError.MAX_SOUNDFURNI;
        } else if (tile == null || tile.getState() == RoomTileState.INVALID) {
            return FurnitureMovementError.INVALID_MOVE;
        } else if (this.room.getRoomRightsManager().hasRights(habbo) || this.room.getGuildRightLevel(habbo).isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS) || habbo.hasPermissionRight(Permission.ACC_MOVEROTATE)) {
            return FurnitureMovementError.NONE;
        }

        rotation %= 8;

        if (habbo.getHabboStats().isRentingSpace()) {
            RoomItem rentSpace = this.currentItems.get(habbo.getHabboStats().getRentedItemId());

            if (rentSpace != null) {
                if (!RoomLayout.squareInSquare(RoomLayout.getRectangle(rentSpace.getCurrentPosition().getX(), rentSpace.getCurrentPosition().getY(), rentSpace.getBaseItem().getWidth(), rentSpace.getBaseItem().getLength(), rentSpace.getRotation()), RoomLayout.getRectangle(tile.getX(), tile.getY(), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation))) {
                    return FurnitureMovementError.NO_RIGHTS;
                } else {
                    return FurnitureMovementError.NONE;
                }
            }
        }

        //TODO CHECK THIS SITUATION
        for (RoomItem area : this.getItemsOfType(InteractionBuildArea.class)) {
            if (((InteractionBuildArea) area).inSquare(tile) && ((InteractionBuildArea) area).isBuilder(habbo.getHabboInfo().getUsername())) {
                return FurnitureMovementError.NONE;
            }
        }

        return FurnitureMovementError.NO_RIGHTS;
    }

    public FurnitureMovementError placeFloorItemAt(RoomItem item, RoomTile tile, int rotation, Habbo actor) {
        FurnitureMovementError error = this.canPlaceFurnitureAt(item, actor, tile, rotation);

        if(!error.equals(FurnitureMovementError.NONE)) {
            return error;
        }

        boolean pluginHelper = false;

        if (Emulator.getPluginManager().isRegistered(FurniturePlacedEvent.class, true)) {
            FurniturePlacedEvent event = Emulator.getPluginManager().fireEvent(new FurniturePlacedEvent(item, actor, tile));

            if (event.isCancelled()) {
                return FurnitureMovementError.CANCEL_PLUGIN_PLACE;
            }

            pluginHelper = event.hasPluginHelper();
        }

        THashSet<RoomTile> occupiedTiles = this.room.getLayout().getTilesAt(tile, item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation);

        FurnitureMovementError fits = this.furnitureFitsAt(tile, item, rotation, true);

        if (!fits.equals(FurnitureMovementError.NONE) && !pluginHelper) {
            return fits;
        }

        double height = tile.getStackHeight();

        for (RoomTile tile2 : occupiedTiles) {
            double sHeight = tile2.getStackHeight();
            if (sHeight > height) {
                height = sHeight;
            }
        }

        if (Emulator.getPluginManager().isRegistered(FurnitureBuildheightEvent.class, true)) {
            FurnitureBuildheightEvent event = Emulator.getPluginManager().fireEvent(new FurnitureBuildheightEvent(item, actor, 0.00, height));
            if (event.hasChangedHeight()) {
                height = event.getUpdatedHeight();
            }
        }

        item.setCurrentPosition(tile);
        item.setCurrentZ(height);
        item.setRotation(rotation);
        item.needsUpdate(true);

        this.addRoomItem(item);

        item.onPlace(this.room);
        this.room.updateTiles(occupiedTiles);

        this.room.sendComposer(new ObjectAddMessageComposer(item, this.room.getFurniOwnerName(item.getOwnerInfo().getId())).compose());

        for (RoomTile t : occupiedTiles) {
            this.room.updateHabbosAt(t);
            this.room.updateBotsAt(t);
        }

        Emulator.getThreading().run(item);
        return FurnitureMovementError.NONE;
    }

    public FurnitureMovementError placeWallItemAt(RoomItem item, String wallPosition, Habbo owner) {
        if (!(this.room.getRoomRightsManager().hasRights(owner) || this.room.getGuildRightLevel(owner).isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS))) {
            return FurnitureMovementError.NO_RIGHTS;
        }

        if (Emulator.getPluginManager().isRegistered(FurniturePlacedEvent.class, true)) {
            Event furniturePlacedEvent = new FurniturePlacedEvent(item, owner, null);
            Emulator.getPluginManager().fireEvent(furniturePlacedEvent);

            if (furniturePlacedEvent.isCancelled())
                return FurnitureMovementError.CANCEL_PLUGIN_PLACE;
        }

        item.setWallPosition(wallPosition);
        if (!this.room.getFurniOwnerNames().containsKey(item.getOwnerInfo().getId()) && owner != null) {
            this.room.getFurniOwnerNames().put(item.getOwnerInfo().getId(), owner.getHabboInfo().getUsername());
        }
        this.room.sendComposer(new ItemAddMessageComposer(item, this.room.getFurniOwnerName(item.getOwnerInfo().getId())).compose());
        item.needsUpdate(true);
        this.addRoomItem(item);
        //Deprecated
        item.setRoomId(this.room.getRoomInfo().getId());
        item.setRoom(this.room);
        item.onPlace(this.room);
        Emulator.getThreading().run(item);
        return FurnitureMovementError.NONE;
    }

    public FurnitureMovementError moveItemTo(RoomItem item, RoomTile targetTile, int rotation, Habbo actor) {
        return moveItemTo(item, targetTile, rotation, actor, true, true);
    }

    public FurnitureMovementError moveItemTo(RoomItem item, RoomTile targetTile, int rotation, Habbo actor, boolean sendUpdates, boolean checkForUnits) {
        FurnitureMovementError error = this.canPlaceFurnitureAt(item, actor, targetTile, rotation);

        if(!error.equals(FurnitureMovementError.NONE)) {
            return error;
        }

        RoomTile oldLocation = this.room.getLayout().getTile(item.getCurrentPosition().getX(), item.getCurrentPosition().getY());

        boolean pluginHelper = false;
        if (Emulator.getPluginManager().isRegistered(FurnitureMovedEvent.class, true)) {
            FurnitureMovedEvent event = Emulator.getPluginManager().fireEvent(new FurnitureMovedEvent(item, actor, oldLocation, targetTile));
            if (event.isCancelled()) {
                return FurnitureMovementError.CANCEL_PLUGIN_MOVE;
            }
            pluginHelper = event.hasPluginHelper();
        }

        boolean magicTile = item instanceof InteractionStackHelper;

        Optional<RoomItem> stackHelper = this.getItemsAt(targetTile).stream().filter(InteractionStackHelper.class::isInstance).findAny();

        //Check if can be placed at new position
        THashSet<RoomTile> occupiedTiles = this.room.getLayout().getTilesAt(targetTile, item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation);
        THashSet<RoomTile> newOccupiedTiles = this.room.getLayout().getTilesAt(targetTile, item.getBaseItem().getWidth(), item.getBaseItem().getLength(), rotation);

        RoomItem topItem = this.getTopItemAt(occupiedTiles, null);

        if ((stackHelper.isEmpty() && !pluginHelper) || item.getBaseItem().getInteractionType().getType() == InteractionWater.class) {
            if (oldLocation != targetTile) {
                for (RoomTile t : occupiedTiles) {
                    RoomItem tileTopItem = this.getTopItemAt(t.getX(), t.getY());
                    if (!magicTile && (tileTopItem != null && tileTopItem != item ? (t.getState().equals(RoomTileState.INVALID) || !t.getAllowStack() || !tileTopItem.getBaseItem().allowStack() ||
                            (tileTopItem.getBaseItem().getInteractionType().getType() == InteractionWater.class && (item.getBaseItem().getInteractionType().getType() != InteractionWaterItem.class || item.getBaseItem().getInteractionType().getType() == InteractionWater.class))) : this.room.calculateTileState(t, item).equals(RoomTileState.INVALID)) || stackHelper.isPresent() && item.getBaseItem().getInteractionType().getType() == InteractionWater.class) {
                        return FurnitureMovementError.CANT_STACK;
                    }

                    if (!Emulator.getConfig().getBoolean("wired.place.under", false) || (Emulator.getConfig().getBoolean("wired.place.under", false) && !item.isWalkable() && !item.getBaseItem().allowSit())) {
                        if (checkForUnits && !magicTile) {
                            if (this.room.getRoomUnitManager().hasHabbosAt(t))
                                return FurnitureMovementError.TILE_HAS_HABBOS;
                            if (!this.room.getRoomUnitManager().getBotsAt(t).isEmpty())
                                return FurnitureMovementError.TILE_HAS_BOTS;
                            if (this.room.getRoomUnitManager().hasPetsAt(t))
                                return FurnitureMovementError.TILE_HAS_PETS;
                        }
                    }
                }
            }

            List<Pair<RoomTile, THashSet<RoomItem>>> tileFurniList = new ArrayList<>();

            for (RoomTile t : occupiedTiles) {
                tileFurniList.add(Pair.create(t, this.getItemsAt(t)));
            }

            if (!magicTile && !item.canStackAt(tileFurniList)) {
                return FurnitureMovementError.CANT_STACK;
            }
        }

        THashSet<RoomTile> oldOccupiedTiles = this.room.getLayout().getTilesAt(this.room.getLayout().getTile(item.getCurrentPosition().getX(), item.getCurrentPosition().getY()), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), item.getRotation());

        int oldRotation = item.getRotation();

        if (oldRotation != rotation) {
            item.setRotation(rotation);

            if (Emulator.getPluginManager().isRegistered(FurnitureRotatedEvent.class, true)) {
                Event furnitureRotatedEvent = new FurnitureRotatedEvent(item, actor, oldRotation);
                Emulator.getPluginManager().fireEvent(furnitureRotatedEvent);

                if (furnitureRotatedEvent.isCancelled()) {
                    item.setRotation(oldRotation);
                    return FurnitureMovementError.CANCEL_PLUGIN_ROTATE;
                }
            }

            if ((stackHelper.isEmpty() && topItem != null && topItem != item && !topItem.getBaseItem().allowStack()) || (topItem != null && topItem != item && topItem.getCurrentZ() + Item.getCurrentHeight(topItem) + Item.getCurrentHeight(item) > Room.MAXIMUM_FURNI_HEIGHT)) {
                item.setRotation(oldRotation);
                return FurnitureMovementError.CANT_STACK;
            }

            // )
        }
        //Place at new position

        double height;

        if (stackHelper.isPresent()) {
            height = stackHelper.get().getExtraData().isEmpty() ? Double.parseDouble("0.0") : (Double.parseDouble(stackHelper.get().getExtraData()) / 100);
        } else if (item == topItem) {
            height = item.getCurrentZ();
        } else {
            height = this.room.getStackHeight(targetTile.getX(), targetTile.getY(), false, item);
            for (RoomTile til : occupiedTiles) {
                double sHeight = this.room.getStackHeight(til.getX(), til.getY(), false, item);
                if (sHeight > height) {
                    height = sHeight;
                }
            }
        }

        if (height > Room.MAXIMUM_FURNI_HEIGHT) {
            return FurnitureMovementError.CANT_STACK;
        }

        if (height < this.room.getLayout().getHeightAtSquare(targetTile.getX(), targetTile.getY())) {
            return FurnitureMovementError.CANT_STACK; //prevent furni going under the floor
        }

        if (Emulator.getPluginManager().isRegistered(FurnitureBuildheightEvent.class, true)) {
            FurnitureBuildheightEvent event = Emulator.getPluginManager().fireEvent(new FurnitureBuildheightEvent(item, actor, 0.00, height));
            if (event.hasChangedHeight()) {
                height = event.getUpdatedHeight();
            }
        }

        if (height > Room.MAXIMUM_FURNI_HEIGHT) {
            return FurnitureMovementError.CANT_STACK;
        }

        if (height < this.room.getLayout().getHeightAtSquare(targetTile.getX(), targetTile.getY())) {
            return FurnitureMovementError.CANT_STACK; //prevent furni going under the floor
        }

        item.setCurrentPosition(targetTile);
        item.setCurrentZ(height);

        if (magicTile) {
            item.setCurrentZ(targetTile.getZ());
            item.setExtraData(String.valueOf(item.getCurrentZ() * 100));
        }

        if (item.getCurrentZ() > Room.MAXIMUM_FURNI_HEIGHT) {
            item.setCurrentZ(Room.MAXIMUM_FURNI_HEIGHT);
        }

        //Update Furniture
        item.onMove(this.room, oldLocation, targetTile);
        item.needsUpdate(true);

        Emulator.getThreading().run(item);

        if (sendUpdates) {
            this.room.sendComposer(new ObjectUpdateMessageComposer(item).compose());
        }

        //Update old & new tiles
        occupiedTiles.removeAll(oldOccupiedTiles);
        occupiedTiles.addAll(oldOccupiedTiles);

        this.room.updateTiles(occupiedTiles);

        //Update Habbos at old position
        for (RoomTile t : occupiedTiles) {
            this.room.updateHabbosAt(t);
            this.room.updateBotsAt(t);
        }

        if (Emulator.getConfig().getBoolean("wired.place.under", false)) {
            for (RoomTile t : newOccupiedTiles) {
                for (Habbo h : this.room.getRoomUnitManager().getHabbosAt(t)) {
                    try {
                        item.onWalkOn(h.getRoomUnit(), this.room, null);
                    } catch (Exception ignored) {

                    }
                }
            }
        }

        return FurnitureMovementError.NONE;
    }

    public void pickUpItem(RoomItem item, Habbo picker) {
        if (item == null)
            return;

        if (Emulator.getPluginManager().isRegistered(FurniturePickedUpEvent.class, true)) {
            Event furniturePickedUpEvent = new FurniturePickedUpEvent(item, picker);
            Emulator.getPluginManager().fireEvent(furniturePickedUpEvent);

            if (furniturePickedUpEvent.isCancelled())
                return;
        }

        this.removeRoomItem(item);
        item.onPickUp(this.room);
        //Deprecated
        item.setRoomId(0);
        item.setRoom(null);
        item.needsUpdate(true);

        if (item.getBaseItem().getType() == FurnitureType.FLOOR) {
            this.room.sendComposer(new RemoveFloorItemComposer(item).compose());

            THashSet<RoomTile> updatedTiles = new THashSet<>();
            Rectangle rectangle = RoomLayout.getRectangle(item.getCurrentPosition().getX(), item.getCurrentPosition().getY(), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), item.getRotation());

            for (short x = (short) rectangle.x; x < rectangle.x + rectangle.getWidth(); x++) {
                for (short y = (short) rectangle.y; y < rectangle.y + rectangle.getHeight(); y++) {
                    double stackHeight = this.room.getStackHeight(x, y, false);
                    RoomTile tile = this.room.getLayout().getTile(x, y);

                    if (tile != null) {
                        tile.setStackHeight(stackHeight);
                        updatedTiles.add(tile);
                    }
                }
            }
            this.room.sendComposer(new HeightMapUpdateMessageComposer(this.room, updatedTiles).compose());
            this.room.updateTiles(updatedTiles);
            updatedTiles.forEach(tile -> {
                this.room.updateHabbosAt(tile);
                this.room.updateBotsAt(tile.getX(), tile.getY());
            });
        } else if (item.getBaseItem().getType() == FurnitureType.WALL) {
            this.room.sendComposer(new ItemRemoveMessageComposer(item).compose());
        }

        Habbo habbo;
        habbo = picker != null && picker.getHabboInfo().getId() == item.getId() ? (picker) : (Emulator.getGameServer().getGameClientManager().getHabbo(item.getOwnerInfo().getId()));
        if (habbo != null) {
            habbo.getInventory().getItemsComponent().addItem(item);
            habbo.getClient().sendResponse(new UnseenItemsComposer(item));
            habbo.getClient().sendResponse(new FurniListInvalidateComposer());
        }
        Emulator.getThreading().run(item);
    }

    public void ejectUserItem(RoomItem item) {
        this.pickUpItem(item, null);
    }

    public void ejectUserFurni(int userId) {
        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(userId);
        HashSet<RoomItem> userItems = this.currentItems.values().stream().filter(item -> item.getOwnerInfo().getId() == userId).collect(Collectors.toCollection(HashSet::new));

        if (habbo != null) {
            habbo.getInventory().getItemsComponent().addItems(userItems);
            habbo.getClient().sendResponse(new UnseenItemsComposer(userItems));
        }

        for (RoomItem i : userItems) {
            this.pickUpItem(i, null);
        }
    }

    public void ejectAllFurni() {
        this.ejectAllFurni(null);
    }

    public void ejectAllFurni(Habbo habbo) {
        ConcurrentHashMap<Integer, HashSet<RoomItem>> userItemsMap = new ConcurrentHashMap<>();

        for (RoomItem item : this.currentItems.values()) {
            if ((habbo != null && item.getOwnerInfo().getId() == habbo.getHabboInfo().getId()) || item instanceof InteractionPostIt) {
                continue;
            }

            userItemsMap.computeIfAbsent(item.getOwnerInfo().getId(), k -> new HashSet<>()).add(item);
        }

        for (Map.Entry<Integer, HashSet<RoomItem>> entrySet : userItemsMap.entrySet()) {
            for (RoomItem i : entrySet.getValue()) {
                this.pickUpItem(i, null);
            }

            Habbo user = Emulator.getGameEnvironment().getHabboManager().getHabbo(entrySet.getKey());

            if (user != null) {
                user.getInventory().getItemsComponent().addItems(entrySet.getValue());
                user.getClient().sendResponse(new UnseenItemsComposer(entrySet.getValue()));
            }
        }
    }

    public FurnitureMovementError furnitureFitsAt(RoomTile targetTile, RoomItem item, int rotation, boolean checkForUnits) {
        RoomLayout layout = this.room.getLayout();
        boolean wiredPlaceUnder = Emulator.getConfig().getBoolean("wired.place.under", false);
        Item baseItem = item.getBaseItem();

        if (!layout.fitsOnMap(targetTile, baseItem.getWidth(), baseItem.getLength(), rotation)) {
            return FurnitureMovementError.INVALID_MOVE;
        }

        if (item instanceof InteractionStackHelper) {
            return FurnitureMovementError.NONE;
        }

        THashSet<RoomTile> occupiedTiles = this.room.getLayout().getTilesAt(targetTile, baseItem.getWidth(), baseItem.getLength(), rotation);

        for (RoomTile occupiedTile : occupiedTiles) {
            if (occupiedTile.getState() == RoomTileState.INVALID) {
                return FurnitureMovementError.INVALID_MOVE;
            }

            if(!Emulator.getConfig().getBoolean("wired.place.under", false) || (Emulator.getConfig().getBoolean("wired.place.under", false) && !item.isWalkable() && !item.getBaseItem().allowSit() && !item.getBaseItem().allowLay())) {
                if (checkForUnits && this.room.getRoomUnitManager().hasHabbosAt(occupiedTile)) return FurnitureMovementError.TILE_HAS_HABBOS;
                if (checkForUnits && this.room.getRoomUnitManager().hasBotsAt(occupiedTile)) return FurnitureMovementError.TILE_HAS_BOTS;
                if (checkForUnits && this.room.getRoomUnitManager().hasPetsAt(occupiedTile)) return FurnitureMovementError.TILE_HAS_PETS;
            }
        }

        Optional<RoomItem> stackHelper = this.getItemsAt(targetTile).stream().filter(InteractionStackHelper.class::isInstance).findAny();

        List<Pair<RoomTile, THashSet<RoomItem>>> tileFurniList = new ArrayList<>();

        for (RoomTile t : occupiedTiles) {
            tileFurniList.add(Pair.create(t, this.getItemsAt(t)));

            RoomItem topItem = this.getTopItemAt(t.getX(), t.getY(), item);
            if (topItem != null && !topItem.getBaseItem().allowStack() && !t.getAllowStack()) {
                return FurnitureMovementError.CANT_STACK;
            }

            if ((stackHelper.isPresent() && baseItem.getInteractionType().getType() == InteractionWater.class) || topItem != null && (topItem.getBaseItem().getInteractionType().getType() == InteractionWater.class && (baseItem.getInteractionType().getType() == InteractionWater.class || baseItem.getInteractionType().getType() != InteractionWaterItem.class))) {
                return FurnitureMovementError.CANT_STACK;
            }
        }

        if (!item.canStackAt(tileFurniList)) {
            return FurnitureMovementError.CANT_STACK;
        }

        return FurnitureMovementError.NONE;
    }

    @Deprecated
    public THashSet<RoomItem> getItemsAt(int x, int y) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);

        if (tile != null) {
            return this.getItemsAt(tile);
        }

        return new THashSet<>(0);
    }

    public THashSet<RoomItem> getItemsAt(RoomTile tile) {
        return getItemsAt(tile, false);
    }

    public THashSet<RoomItem> getItemsAt(RoomTile tile, boolean returnOnFirst) {
        THashSet<RoomItem> items = new THashSet<>(0);

        if (tile == null) {
            return items;
        }

        if (this.room.isLoaded()) {
            THashSet<RoomItem> cachedItems = this.room.tileCache.get(tile);
            if (cachedItems != null)
                return cachedItems;
        }

        Iterator<RoomItem> iterator = this.currentItems.values().iterator();

        for (int i = this.currentItems.size(); i-- > 0; ) {
            RoomItem item;
            try {
                item = iterator.next();
            } catch (Exception e) {
                break;
            }

            if (item == null || item.getBaseItem().getType() != FurnitureType.FLOOR) {
                continue;
            }

            int width, length;

            if (item.getRotation() != 2 && item.getRotation() != 6) {
                width = item.getBaseItem().getWidth() > 0 ? item.getBaseItem().getWidth() : 1;
                length = item.getBaseItem().getLength() > 0 ? item.getBaseItem().getLength() : 1;
            } else {
                width = item.getBaseItem().getLength() > 0 ? item.getBaseItem().getLength() : 1;
                length = item.getBaseItem().getWidth() > 0 ? item.getBaseItem().getWidth() : 1;
            }

            if (!(tile.getX() >= item.getCurrentPosition().getX() && tile.getX() <= item.getCurrentPosition().getX() + width - 1 && tile.getY() >= item.getCurrentPosition().getY() && tile.getY() <= item.getCurrentPosition().getY() + length - 1))
                continue;

            items.add(item);

            if (returnOnFirst) {
                return items;
            }
        }

        if (this.room.isLoaded()) {
            this.room.tileCache.put(tile, items);
        }

        return items;
    }

    public THashSet<RoomItem> getItemsAt(int x, int y, double minZ) {
        THashSet<RoomItem> items = new THashSet<>();

        for (RoomItem item : this.getItemsAt(x, y)) {
            if (item.getCurrentZ() < minZ)
                continue;

            items.add(item);
        }
        return items;
    }

    public THashSet<RoomItem> getItemsAt(Class<? extends RoomItem> type, int x, int y) {
        THashSet<RoomItem> items = new THashSet<>();

        for (RoomItem item : this.getItemsAt(x, y)) {
            if (!item.getClass().equals(type))
                continue;

            items.add(item);
        }
        return items;
    }

    public boolean hasItemsAt(int x, int y) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);

        if (tile == null)
            return false;

        return !this.getItemsAt(tile, true).isEmpty();
    }

    public RoomItem getTopItemAt(RoomTile tile) {
        if(tile == null) {
            return null;
        }

        return this.getTopItemAt(tile.getX(), tile.getY(), null);
    }

    public RoomItem getTopItemAt(int x, int y) {
        return this.getTopItemAt(x, y, null);
    }

    public RoomItem getTopItemAt(int x, int y, RoomItem exclude) {
        RoomTile tile = this.room.getLayout().getTile((short) x, (short) y);

        if (tile == null)
            return null;

        RoomItem highestItem = null;

        for (RoomItem item : this.getItemsAt(x, y)) {
            if (exclude != null && exclude == item)
                continue;

            if (highestItem != null) {
                if (highestItem.getCurrentZ() + Item.getCurrentHeight(highestItem) > item.getCurrentZ() + Item.getCurrentHeight(item))
                    continue;
            }

            highestItem = item;
        }

        return highestItem;
    }

    public RoomItem getTopItemAt(THashSet<RoomTile> tiles, RoomItem exclude) {
        RoomItem highestItem = null;
        for (RoomTile tile : tiles) {

            if (tile == null)
                continue;

            for (RoomItem item : this.getItemsAt(tile.getX(), tile.getY())) {
                if (exclude != null && exclude == item)
                    continue;

                if (highestItem != null) {
                    if (highestItem.getCurrentZ() + Item.getCurrentHeight(highestItem) > item.getCurrentZ() + Item.getCurrentHeight(item))
                        continue;
                }

                highestItem = item;
            }
        }

        return highestItem;
    }

    public double getTopHeightAt(int x, int y) {
        RoomItem item = this.getTopItemAt(x, y);
        if (item != null) {
            return (item.getCurrentZ() + Item.getCurrentHeight(item) - (item.getBaseItem().allowSit() ? 1 : 0));
        } else {
            return this.room.getLayout().getHeightAtSquare(x, y);
        }
    }

    public RoomItem getLowestChair(RoomTile tile) {
        RoomItem lowestChair = null;

        THashSet<RoomItem> items = this.getItemsAt(tile);
        if (items != null && !items.isEmpty()) {
            for (RoomItem item : items) {

                if (!item.getBaseItem().allowSit())
                    continue;

                if (lowestChair != null) {
                    if (lowestChair.getCurrentZ() < item.getCurrentZ()) continue;
                }

                lowestChair = item;
            }
        }

        return lowestChair;
    }

    public RoomItem getTallestChair(RoomTile tile) {
        RoomItem lowestChair = null;

        THashSet<RoomItem> items = this.getItemsAt(tile);
        if (items != null && !items.isEmpty()) {
            for (RoomItem item : items) {

                if (!item.getBaseItem().allowSit())
                    continue;

                if (lowestChair != null) {
                    if (lowestChair.getCurrentZ() + Item.getCurrentHeight(lowestChair) > item.getCurrentZ() + Item.getCurrentHeight(item))
                        continue;
                }

                lowestChair = item;
            }
        }

        return lowestChair;
    }

    public void addCycleTask(ICycleable task) {
        this.cycleTasks.add(task);
    }

    public void removeCycleTask(ICycleable task) {
        this.cycleTasks.remove(task);
    }

    public void addBanzaiTeleporter(InteractionBattleBanzaiTeleporter item) {
        this.banzaiTeleporters.put(item.getId(), item);
    }

    public void removeBanzaiTeleporter(InteractionBattleBanzaiTeleporter item) {
        this.banzaiTeleporters.remove(item.getId());
    }

    public void addRoller(InteractionRoller item) {
        synchronized (this.rollers) {
            this.rollers.put(item.getId(), item);
        }
    }

    public void removeRoller(InteractionRoller roller) {
        synchronized (this.rollers) {
            this.rollers.remove(roller.getId());
        }
    }

    public void addGameScoreboard(InteractionGameScoreboard scoreboard) {
        this.gameScoreboards.put(scoreboard.getId(), scoreboard);
    }

    public void removeScoreboard(InteractionGameScoreboard scoreboard) {
        this.gameScoreboards.remove(scoreboard.getId());
    }

    public void addGameGate(InteractionGameGate gameGate) {
        this.gameGates.put(gameGate.getId(), gameGate);
    }

    public void removeGameGate(InteractionGameGate gameGate) {
        this.gameGates.remove(gameGate.getId());
    }

    public void addGameTimer(InteractionGameTimer gameTimer) {
        this.gameTimers.put(gameTimer.getId(), gameTimer);
    }

    public void removeGameTimer(InteractionGameTimer gameTimer) {
        this.gameTimers.remove(gameTimer.getId());
    }

    public void addFreezeExitTile(InteractionFreezeExitTile freezeExitTile) {
        this.freezeExitTile.put(freezeExitTile.getId(), freezeExitTile);
    }

    public void removeFreezeExitTile(InteractionFreezeExitTile freezeExitTile) {
        this.freezeExitTile.remove(freezeExitTile.getId());
    }

    public void addNest(InteractionNest item) {
        this.nests.put(item.getId(), item);
    }

    public void removeNest(InteractionNest item) {
        this.nests.remove(item.getId());
    }

    public void addPetDrink(InteractionPetDrink item) {
        this.petDrinks.put(item.getId(), item);
    }

    public void removePetDrink(InteractionPetDrink item) {
        this.petDrinks.remove(item.getId());
    }

    public void addPetFood(InteractionPetFood item) {
        this.petFoods.put(item.getId(), item);
    }

    public void removePetFood(InteractionPetFood petFood) {
        this.petFoods.remove(petFood.getId());
    }

    public InteractionPetToy getPetToy(int itemId) {
        return this.petToys.get(itemId);
    }

    public void addPetToy(InteractionPetToy item) {
        this.petToys.put(item.getId(), item);
    }

    public void removePetToy(InteractionPetToy petToy) {
        this.petToys.remove(petToy.getId());
    }

    public void addUndefined(RoomItem item) {
        synchronized (this.undefinedSpecials) {
            this.undefinedSpecials.put(item.getId(), item);
        }
    }

    public void removeUndefined(RoomItem item) {
        synchronized (this.undefinedSpecials) {
            this.undefinedSpecials.remove(item.getId());
        }
    }

    private void sortItem(RoomItem item) {
        if(item.getBaseItem().getType().equals(FurnitureType.FLOOR)) {
            this.floorItems.put(item.getId(), item);
            this.sortFloorItem(item);
        } else if(item.getBaseItem().getType().equals(FurnitureType.WALL)) {
            this.wallItems.put(item.getId(), item);
        }
    }

    private void sortFloorItem(RoomItem item) {
        if (item instanceof ICycleable) {
            this.addCycleTask((ICycleable) item);
        }

        if(item instanceof InteractionWired wired) {
            this.wiredManager.addWired(wired);
        } else if (item instanceof InteractionBattleBanzaiTeleporter interactionBattleBanzaiTeleporter) {
            this.addBanzaiTeleporter(interactionBattleBanzaiTeleporter);
        } else if (item instanceof InteractionRoller interactionRoller) {
            this.addRoller(interactionRoller);
        } else if (item instanceof InteractionGameScoreboard interactionGameScoreboard) {
            this.addGameScoreboard(interactionGameScoreboard);
        } else if (item instanceof InteractionGameGate interactionGameGate) {
            this.addGameGate(interactionGameGate);
        } else if (item instanceof InteractionGameTimer interactionGameTimer) {
            this.addGameTimer(interactionGameTimer);
        } else if (item instanceof InteractionFreezeExitTile interactionFreezeExitTile) {
            this.addFreezeExitTile(interactionFreezeExitTile);
        } else if (item instanceof InteractionNest interactionNest) {
            this.addNest(interactionNest);
        } else if (item instanceof InteractionPetDrink interactionPetDrink) {
            this.addPetDrink(interactionPetDrink);
        } else if (item instanceof InteractionPetFood interactionPetFood) {
            this.addPetFood(interactionPetFood);
        } else if (item instanceof InteractionPetToy interactionPetToy) {
            this.addPetToy(interactionPetToy);
        } else if (item instanceof InteractionPetTree) {
            this.addUndefined(item);
        } else if (item instanceof InteractionPetTrampoline) {
            this.addUndefined(item);
        } else if (item instanceof InteractionMoodLight) {
            this.addUndefined(item);
        } else if (item instanceof InteractionPyramid) {
            this.addUndefined(item);
        } else if (item instanceof InteractionMusicDisc) {
            this.addUndefined(item);
        } else if (item instanceof InteractionBattleBanzaiSphere) {
            this.addUndefined(item);
        } else if (item instanceof InteractionTalkingFurniture) {
            this.addUndefined(item);
        } else if (item instanceof InteractionWater) {
            this.addUndefined(item);
        } else if (item instanceof InteractionWaterItem) {
            this.addUndefined(item);
        } else if (item instanceof InteractionMuteArea) {
            this.addUndefined(item);
        } else if (item instanceof InteractionBuildArea) {
            this.addUndefined(item);
        } else if (item instanceof InteractionTagPole) {
            this.addUndefined(item);
        } else if (item instanceof InteractionTagField) {
            this.addUndefined(item);
        } else if (item instanceof InteractionJukeBox) {
            this.addUndefined(item);
        } else if (item instanceof InteractionPetBreedingNest) {
            this.addUndefined(item);
        } else if (item instanceof InteractionBlackHole) {
            this.addUndefined(item);
        } else if (item instanceof InteractionWiredHighscore) {
            this.addUndefined(item);
        } else if (item instanceof InteractionStickyPole) {
            this.addUndefined(item);
        } else if (item instanceof WiredBlob) {
            this.addUndefined(item);
        } else if (item instanceof InteractionTent) {
            this.addUndefined(item);
        } else if (item instanceof InteractionSnowboardSlope) {
            this.addUndefined(item);
        } else if (item instanceof InteractionFireworks) {
            this.addUndefined(item);
        }
    }

    public void dispose() {
        this.currentItems.values().parallelStream()
                .filter(RoomItem::needsUpdate)
                .forEach(roomItem -> {
                    roomItem.run();
                    this.currentItems.remove(roomItem.getId());
                });

        this.currentItems.clear();
    }
}
