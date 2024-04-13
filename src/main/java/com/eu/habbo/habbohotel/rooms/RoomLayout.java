package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.constants.RoomTileState;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import com.eu.habbo.habbohotel.rooms.entities.units.types.RoomAvatar;
import com.eu.habbo.habbohotel.rooms.bots.entities.RoomBot;
import gnu.trove.set.hash.THashSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.sql.ResultSet;
import java.util.List;
import java.util.*;

@Slf4j
public class RoomLayout {
    protected static final int BASICMOVEMENTCOST = 10;
    protected static final int DIAGONALMOVEMENTCOST = 14;
    public static double MAXIMUM_STEP_HEIGHT = 1.1;
    public static boolean ALLOW_FALLING = true;
    public boolean CANMOVEDIAGONALY = true;
    @Getter
    private String name;
    @Getter
    @Setter
    private short doorX;
    @Getter
    @Setter
    private short doorY;
    @Getter
    private short doorZ;
    @Getter
    @Setter
    private int doorDirection;
    @Getter
    @Setter
    private String heightmap;
    @Getter
    private int mapSize;
    @Getter
    private int mapSizeX;
    @Getter
    private int mapSizeY;
    private RoomTile[][] roomTiles;
    @Getter
    private RoomTile doorTile;
    private final Room room;

    public RoomLayout(ResultSet set, Room room) {
        this.room = room;
        try {
            this.name = set.getString("name");
            this.doorX = set.getShort("door_x");
            this.doorY = set.getShort("door_y");

            this.doorDirection = set.getInt("door_dir");
            this.heightmap = set.getString("heightmap");

            this.parse();
        } catch (Exception e) {
            log.error("Caught exception", e);
        }
    }

    public static boolean squareInSquare(Rectangle outerSquare, Rectangle innerSquare) {
        if (outerSquare.x > innerSquare.x)
            return false;

        if (outerSquare.y > innerSquare.y)
            return false;

        if (outerSquare.x + outerSquare.width < innerSquare.x + innerSquare.width)
            return false;

        return outerSquare.y + outerSquare.height >= innerSquare.y + innerSquare.height;
    }

    public static boolean tileInSquare(Rectangle square, RoomTile tile) {
        return (square.contains(tile.getX(), tile.getY()));
    }

    public static boolean pointInSquare(int x1, int y1, int x2, int y2, int pointX, int pointY) {
        return (pointX >= x1 && pointY >= y1) && (pointX <= x2 && pointY <= y2);
    }

    public static boolean tilesAdjecent(RoomTile one, RoomTile two) {
        return !(one == null || two == null) && !(Math.abs(one.getX() - two.getX()) > 1) && !(Math.abs(one.getY() - two.getY()) > 1);
    }

    public static Rectangle getRectangle(int x, int y, int width, int length, int rotation) {
        rotation = (rotation % 8);

        if (rotation == 2 || rotation == 6) {
            return new Rectangle(x, y, length, width);
        }

        return new Rectangle(x, y, width, length);
    }

    public static boolean tilesAdjecent(RoomTile tile, RoomTile comparator, int width, int length, int rotation) {
        Rectangle rectangle = getRectangle(comparator.getX(), comparator.getY(), width, length, rotation);
        rectangle = new Rectangle(rectangle.x - 1, rectangle.y - 1, rectangle.width + 2, rectangle.height + 2);

        return rectangle.contains(tile.getX(), tile.getY());
    }

    public void parse() {
        String[] modelTemp = this.heightmap.replace("\n", "").split(Character.toString('\r'));

        this.mapSize = 0;
        this.mapSizeX = modelTemp[0].length();
        this.mapSizeY = modelTemp.length;
        this.roomTiles = new RoomTile[this.mapSizeX][this.mapSizeY];

        for (short y = 0; y < this.mapSizeY; y++) {
            if (modelTemp[y].isEmpty() || modelTemp[y].equalsIgnoreCase("\r")) {
                continue;
            }

            for (short x = 0; x < this.mapSizeX; x++) {
                if (modelTemp[y].length() != this.mapSizeX) {
                    break;
                }

                String square = modelTemp[y].substring(x, x + 1).trim().toLowerCase();
                RoomTileState state = RoomTileState.OPEN;
                short height = 0;
                if (square.equalsIgnoreCase("x")) {
                    state = RoomTileState.INVALID;
                } else {
                    if (square.isEmpty()) {
                        height = 0;
                    } else if (Emulator.isNumeric(square)) {
                        height = Short.parseShort(square);
                    } else {
                        height = (short) (10 + "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(square.toUpperCase()));
                    }
                }
                this.mapSize += 1;

                this.roomTiles[x][y] = new RoomTile(x, y, height, state, true);
            }
        }

        this.doorTile = this.roomTiles[this.doorX][this.doorY];

        if (this.doorTile != null) {
            this.doorTile.setAllowStack(false);
            RoomTile doorFrontTile = this.getTileInFront(this.doorTile, this.doorDirection);

            if (doorFrontTile != null && this.tileExists(doorFrontTile.getX(), doorFrontTile.getY())) {
                if (this.roomTiles[doorFrontTile.getX()][doorFrontTile.getY()].getState() != RoomTileState.INVALID) {
                    if (this.doorZ != this.roomTiles[doorFrontTile.getX()][doorFrontTile.getY()].getZ() || this.roomTiles[this.doorX][this.doorY].getState() != this.roomTiles[doorFrontTile.getX()][doorFrontTile.getY()].getState()) {
                        this.doorZ = this.roomTiles[doorFrontTile.getX()][doorFrontTile.getY()].getZ();
                        this.roomTiles[this.doorX][this.doorY].setState(RoomTileState.OPEN);
                    }
                }
            }
        }
    }

    public short getHeightAtSquare(int x, int y) {
        if (x < 0 ||
                y < 0 ||
                x >= this.getMapSizeX() ||
                y >= this.getMapSizeY())
            return 0;

        return this.roomTiles[x][y].getZ();
    }

    public double getStackHeightAtSquare(int x, int y) {
        if (x < 0 ||
                y < 0 ||
                x >= this.getMapSizeX() ||
                y >= this.getMapSizeY())
            return 0;

        return this.roomTiles[x][y].getStackHeight();
    }

    public double getRelativeHeightAtSquare(int x, int y) {
        if (x < 0 ||
                y < 0 ||
                x >= this.getMapSizeX() ||
                y >= this.getMapSizeY())
            return 0;

        return this.roomTiles[x][y].relativeHeight();
    }

    public RoomTile getTile(short x, short y) {
        if (this.tileExists(x, y)) {
            return this.roomTiles[x][y];
        }

        return null;
    }

    public boolean tileExists(short x, short y) {
        return !(x < 0 || y < 0 || x >= this.getMapSizeX() || y >= this.getMapSizeY());
    }

    public boolean tileWalkable(RoomTile tile) {
        return this.tileWalkable(tile.getX(), tile.getY());
    }

    public boolean tileWalkable(short x, short y) {
        boolean walkable = false;

        if(this.tileExists(x, y)) {
            RoomTile tile = this.getTile(x, y);
            walkable = tile.isWalkable() && (this.room.getRoomUnitManager().areRoomUnitsAt(tile) && !this.room.getRoomInfo().isAllowWalkthrough());
        }

        return walkable;
    }

    public boolean isVoidTile(short x, short y) {
        if (!this.tileExists(x, y)) return true;
        return this.roomTiles[x][y].getState() == RoomTileState.INVALID;
    }

    public String getRelativeMap() {
        return this.heightmap.replace("\r\n", "\r");
    }//re

    public final Deque<RoomTile> findPath(RoomTile oldTile, RoomTile newTile, RoomTile goalLocation, RoomUnit roomUnit) {
        return this.findPath(oldTile, newTile, goalLocation, roomUnit, false);
    }

    /// Pathfinder Reworked By Quadral, thanks buddy!! You Saved Morningstar <3
    public final Deque<RoomTile> findPath(RoomTile oldTile, RoomTile newTile, RoomTile goalLocation, RoomUnit roomUnit, boolean isWalktroughRetry) {
        if (this.room == null || !this.room.isLoaded() || oldTile == null || newTile == null || oldTile.equals(newTile) || newTile.getState() == RoomTileState.INVALID)
            return new LinkedList<>();

        LinkedList<RoomTile> openList = new LinkedList<>();
        List<RoomTile> closedList = new LinkedList<>();
        openList.add(oldTile.copy());

        RoomTile doorTile = this.room.getLayout().getDoorTile();

        while (!openList.isEmpty()) {
            RoomTile current = this.lowestFInOpen(openList);
            if (current.getX() == newTile.getX() && current.getY() == newTile.getY()) {
                return this.calcPath(this.findTile(openList, oldTile.getX(), oldTile.getY()), current);
            }

            closedList.add(current);
            openList.remove(current);

            List<RoomTile> adjacentNodes = this.getAdjacent(openList, current, newTile, roomUnit);
            for (RoomTile currentAdj : adjacentNodes) {
                if (closedList.contains(currentAdj)) continue;

                if (roomUnit.canOverrideTile(currentAdj)) {
                    currentAdj.setPrevious(current);
                    currentAdj.sethCosts(this.findTile(openList, newTile.getX(), newTile.getY()));
                    currentAdj.setgCosts(current);
                    openList.add(currentAdj);
                    continue;
                }

                if (currentAdj.getState() == RoomTileState.BLOCKED || ((currentAdj.getState() == RoomTileState.SIT || currentAdj.getState() == RoomTileState.LAY) && !currentAdj.equals(goalLocation))) {
                    closedList.add(currentAdj);
                    openList.remove(currentAdj);
                    continue;
                }

                double height = currentAdj.getStackHeight() - current.getStackHeight();

                if ((!ALLOW_FALLING && height < -MAXIMUM_STEP_HEIGHT) || (currentAdj.getState() == RoomTileState.OPEN && height > MAXIMUM_STEP_HEIGHT)) {
                    closedList.add(currentAdj);
                    openList.remove(currentAdj);
                    continue;
                }

                RoomUnit exception = null;

                if(roomUnit instanceof RoomAvatar roomAvatar && roomAvatar.isRiding()) {
                    exception = roomAvatar.getRidingPet().getRoomUnit();
                }

                if (this.room.getRoomUnitManager().areRoomUnitsAt(currentAdj, exception) && doorTile.distance(currentAdj) > 2 && (!isWalktroughRetry || !this.room.getRoomInfo().isAllowWalkthrough() || currentAdj.equals(goalLocation))) {
                    closedList.add(currentAdj);
                    openList.remove(currentAdj);
                    continue;
                }

                if (!openList.contains(currentAdj)) {
                    currentAdj.setPrevious(current);
                    currentAdj.sethCosts(this.findTile(openList, newTile.getX(), newTile.getY()));
                    currentAdj.setgCosts(current);
                    openList.add(currentAdj);
                } else if (currentAdj.getGCosts() > currentAdj.calculategCosts(current)) {
                    currentAdj.setPrevious(current);
                    currentAdj.setgCosts(current);
                }
            }
        }

        if (this.room.getRoomInfo().isAllowWalkthrough() && !isWalktroughRetry) {
            return this.findPath(oldTile, newTile, goalLocation, roomUnit, true);
        }

        return null;
    }

    public List<RoomTile> getAdjacentTiles(short x, short y) {
        List<RoomTile> adjacentTiles = new ArrayList<>();
        adjacentTiles.add(this.getTile(x, (short) (y - 1))); // Above
        adjacentTiles.add(this.getTile(x, (short) (y + 1))); // Below
        adjacentTiles.add(this.getTile((short) (x - 1), y)); // Left
        adjacentTiles.add(this.getTile((short) (x + 1), y)); // Right

        return adjacentTiles;
    }

    public RoomTile getClosestWalkableTile(short x, short y) {
        RoomTile startTile = getTile(x, y);
        if (startTile == null || startTile.isWalkable()) {
            return startTile; // Return the start tile if it's walkable or null
        }

        Queue<RoomTile> queue = new ArrayDeque<>();
        boolean[][] visited = new boolean[this.getMapSizeX()][this.getMapSizeY()]; // Assuming width and height are the dimensions of the room layout

        queue.add(startTile);
        visited[x][y] = true;

        while (!queue.isEmpty()) {
            for (RoomTile adjacentTile : this.getAdjacentTiles(x, y)) {
                if (adjacentTile == null) {
                    continue;
                }
                int adjX = adjacentTile.getX();
                int adjY = adjacentTile.getY();

                visited[adjX][adjY] = true;
                if (adjacentTile.isWalkable()) {
                    return adjacentTile; // Return the walkable tile
                }
                queue.add(adjacentTile);
            }
        }
        return null;
    }

    private RoomTile findTile(List<RoomTile> tiles, short x, short y) {
        for (RoomTile tile : tiles) {
            if (x == tile.getX() && y == tile.getY()) {
                return tile;
            }
        }

        RoomTile tile = this.getTile(x, y);

        if (tile != null) {
            return tile.copy();
        }
        return null;
    }

    public Deque<RoomTile> calcPath(RoomTile start, RoomTile goal) {
        LinkedList<RoomTile> path = new LinkedList<>();
        if (start == null)
            return path;

        RoomTile curr = goal;
        while (curr != null) {
            path.addFirst(this.getTile(curr.getX(), curr.getY()));
            curr = curr.getPrevious();
            if ((curr != null) && (curr.equals(start))) {
                return path;
            }
        }
        return path;
    }

    private RoomTile lowestFInOpen(List<RoomTile> openList) {
        if (openList == null)
            return null;

        RoomTile cheapest = openList.get(0);
        for (RoomTile anOpenList : openList) {
            if (anOpenList.getfCosts() < cheapest.getfCosts()) {
                cheapest = anOpenList;
            }
        }
        return cheapest;
    }

    private List<RoomTile> getAdjacent(List<RoomTile> openList, RoomTile node, RoomTile nextTile, RoomUnit unit) {
        short x = node.getX();
        short y = node.getY();
        List<RoomTile> adj = new LinkedList<>();
        if (x > 0) {
            RoomTile temp = this.findTile(openList, (short) (x - 1), y);
            if (this.canWalkOn(temp, unit)) {
                if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                    temp.isDiagonally(false);
                        adj.add(temp);
                }
            }
        }
        if (x < this.mapSizeX) {
            RoomTile temp = this.findTile(openList, (short) (x + 1), y);
            if (this.canWalkOn(temp, unit)) {
                if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                    temp.isDiagonally(false);
                    if (!adj.contains(temp))
                        adj.add(temp);
                }
            }
        }
        if (y > 0) {
            RoomTile temp = this.findTile(openList, x, (short) (y - 1));
            if (this.canWalkOn(temp, unit)) {
                if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                    temp.isDiagonally(false);
                    if (!adj.contains(temp))
                        adj.add(temp);
                }
            }
        }
        if (y < this.mapSizeY) {
            RoomTile temp = this.findTile(openList, x, (short) (y + 1));
            if (this.canWalkOn(temp, unit)) {
                if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                    temp.isDiagonally(false);
                    if (!adj.contains(temp))
                        adj.add(temp);
                }
            }
        }
        if (this.CANMOVEDIAGONALY) {
            if ((x < this.mapSizeX) && (y < this.mapSizeY)) {
                RoomTile offX = this.findTile(openList, (short) (x + 1), y);
                RoomTile offY = this.findTile(openList, x, (short) (y + 1));
                if (offX != null && offY != null && (offX.isWalkable() || offY.isWalkable())) {
                    RoomTile temp = this.findTile(openList, (short) (x + 1), (short) (y + 1));
                    if (this.canWalkOn(temp, unit)) {
                        if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                            temp.isDiagonally(true);
                            if (!adj.contains(temp))
                                adj.add(temp);
                        }
                    }
                }
            }
            if ((x > 0) && (y > 0)) {
                RoomTile offX = this.findTile(openList, (short) (x - 1), y);
                RoomTile offY = this.findTile(openList, x, (short) (y - 1));
                if (offX != null && offY != null && (offX.isWalkable() || offY.isWalkable())) {
                    RoomTile temp = this.findTile(openList, (short) (x - 1), (short) (y - 1));
                    if (this.canWalkOn(temp, unit)) {
                        if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                            temp.isDiagonally(true);
                            if (!adj.contains(temp))
                                adj.add(temp);
                        }
                    }
                }
            }
            if ((x > 0) && (y < this.mapSizeY)) {
                RoomTile offX = this.findTile(openList, (short) (x - 1), y);
                RoomTile offY = this.findTile(openList, x, (short) (y + 1));
                if (offX != null && offY != null && (offX.isWalkable() || offY.isWalkable())) {
                    RoomTile temp = this.findTile(openList, (short) (x - 1), (short) (y + 1));
                    if (this.canWalkOn(temp, unit)) {
                        if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                            temp.isDiagonally(true);
                            if (!adj.contains(temp))
                                adj.add(temp);
                        }
                    }
                }
            }
            if ((x < this.mapSizeX) && (y > 0)) {
                RoomTile offX = this.findTile(openList, (short) (x + 1), y);
                RoomTile offY = this.findTile(openList, x, (short) (y - 1));
                if (offX != null && offY != null && (offX.isWalkable() || offY.isWalkable())) {
                    RoomTile temp = this.findTile(openList, (short) (x + 1), (short) (y - 1));
                    if (this.canWalkOn(temp, unit)) {
                        if (temp.getState() != RoomTileState.SIT || nextTile.getStackHeight() - node.getStackHeight() <= 2.0) {
                            temp.isDiagonally(true);
                            if (!adj.contains(temp))
                                adj.add(temp);
                        }
                    }
                }
            }
        }
        return adj;
    }

    private boolean canWalkOn(RoomTile tile, RoomUnit unit) {
        return tile != null && (unit.canOverrideTile(tile) || (tile.getState() != RoomTileState.BLOCKED && tile.getState() != RoomTileState.INVALID));
    }

    public void moveDiagonally(boolean value) {
        this.CANMOVEDIAGONALY = value;
    }

    public RoomTile getTileInFront(RoomTile tile, int rotation) {
        return this.getTileInFront(tile, rotation, 0);
    }

    public RoomTile getTileInFront(RoomTile tile, int rotation, int offset) {
        int offsetX = 0;
        int offsetY = 0;

        rotation = rotation % 8;
        switch (rotation) {
            case 0 -> offsetY--;
            case 1 -> {
                offsetX++;
                offsetY--;
            }
            case 2 -> offsetX++;
            case 3 -> {
                offsetX++;
                offsetY++;
            }
            case 4 -> offsetY++;
            case 5 -> {
                offsetX--;
                offsetY++;
            }
            case 6 -> offsetX--;
            case 7 -> {
                offsetX--;
                offsetY--;
            }
        }

        short x = tile.getX();
        short y = tile.getY();

        for (int i = 0; i <= offset; i++) {
            x += offsetX;
            y += offsetY;
        }

        return this.getTile(x, y);
    }

    public List<RoomTile> getTilesInFront(RoomTile tile, int rotation, int amount) {
        List<RoomTile> tiles = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            RoomTile t = this.getTileInFront(tile, rotation, i);

            if (t != null) {
                tiles.add(t);
            } else {
                break;
            }
        }

        return tiles;
    }

    public List<RoomTile> getTilesAround(RoomTile tile) {
        return getTilesAround(tile, 0);
    }

    public List<RoomTile> getTilesAround(RoomTile tile, int directionOffset) {
        return getTilesAround(tile, directionOffset, true);
    }

    public List<RoomTile> getTilesAround(RoomTile tile, int directionOffset, boolean diagonal) {
        List<RoomTile> tiles = new ArrayList<>(diagonal ? 8 : 4);

        if (tile != null) {
            for (int i = 0; i < 8; i += (diagonal ? 1 : 2)) {
                RoomTile t = this.getTileInFront(tile, (i + directionOffset) % 8);
                if (t != null) {
                    tiles.add(t);
                }
            }
        }

        return tiles;
    }

    public List<RoomTile> getWalkableTilesAround(RoomTile tile) {
        return getWalkableTilesAround(tile, 0);
    }

    public List<RoomTile> getWalkableTilesAround(RoomTile tile, int directionOffset) {
        List<RoomTile> availableTiles = new ArrayList<>(this.getTilesAround(tile, directionOffset));

        List<RoomTile> toRemove = new ArrayList<>();

        for (RoomTile t : availableTiles) {
            if (t == null || t.getState() != RoomTileState.OPEN || !t.isWalkable()) {
                toRemove.add(t);
            }
        }

        for (RoomTile t : toRemove) {
            availableTiles.remove(t);
        }

        return availableTiles;
    }

    public RoomTile getRandomWalkableTilesAround(RoomBot roomBot, RoomTile tile, int radius) {
         if(tile == null || !this.tileExists(tile.getX(), tile.getY()) || tile.getState().equals(RoomTileState.INVALID)) {
             roomBot.setCanWalk(false);
             return null;
         }

        List<RoomTile> newTiles = new ArrayList<>();

        int minX = Math.max(0, tile.getX() - radius);
        int minY = Math.max(0, tile.getY() - radius);
        int maxX = Math.min(this.room.getLayout().getMapSizeX(), tile.getX() + radius);
        int maxY = Math.min(this.room.getLayout().getMapSizeY(), tile.getY() + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                RoomTile tile2 = this.room.getLayout().getTile((short) x, (short) y);
                if (tile2 != null && tile2.getState() != RoomTileState.BLOCKED && tile2.getState() != RoomTileState.INVALID) {
                    newTiles.add(tile2);
                }
            }
        }

        if(newTiles.isEmpty()) {
            log.debug("No Random tiles found.");
            return null;
        }

        Collections.shuffle(newTiles);
        return newTiles.get(0);
    }

    public boolean fitsOnMap(RoomTile tile, int width, int length, int rotation) {
        if (tile != null) {
            if (rotation == 0 || rotation == 4) {
                for (short i = tile.getX(); i <= (tile.getX() + (width - 1)); i++) {
                    for (short j = tile.getY(); j <= (tile.getY() + (length - 1)); j++) {
                        RoomTile t = this.getTile(i, j);

                        if (t == null || t.getState() == RoomTileState.INVALID) {
                            return false;
                        }
                    }
                }
            } else if (rotation == 2 || rotation == 6) {
                for (short i = tile.getX(); i <= (tile.getX() + (length - 1)); i++) {
                    for (short j = tile.getY(); j <= (tile.getY() + (width - 1)); j++) {
                        RoomTile t = this.getTile(i, j);

                        if (t == null || t.getState() == RoomTileState.INVALID) {
                            return false;
                        }
                    }
                }
            } else if (rotation == 1 || rotation == 3 || rotation == 5 || rotation == 7) {
                RoomTile t = this.getTile(tile.getX(), tile.getY());
                if (t == null || t.getState() == RoomTileState.INVALID) {
                    return false;
                }
            }
        }

        return true;
    }

    public THashSet<RoomTile> getTilesAt(RoomTile tile, int width, int length, int rotation) {
        THashSet<RoomTile> pointList = new THashSet<>(width * length, 0.1f);

        if (tile != null) {
            if (rotation == 0 || rotation == 4) {
                for (short i = tile.getX(); i <= (tile.getX() + (width - 1)); i++) {
                    for (short j = tile.getY(); j <= (tile.getY() + (length - 1)); j++) {
                        RoomTile t = this.getTile(i, j);

                        if (t != null) {
                            pointList.add(t);
                        }
                    }
                }
            } else if (rotation == 2 || rotation == 6) {
                for (short i = tile.getX(); i <= (tile.getX() + (length - 1)); i++) {
                    for (short j = tile.getY(); j <= (tile.getY() + (width - 1)); j++) {
                        RoomTile t = this.getTile(i, j);

                        if (t != null) {
                            pointList.add(t);
                        }
                    }
                }
            } else if (rotation == 1 || rotation == 3 || rotation == 5 || rotation == 7) {
                RoomTile t = this.getTile(tile.getX(), tile.getY());
                if (t != null) {
                    pointList.add(t);
                }
            }
        }
        return pointList;
    }
}
