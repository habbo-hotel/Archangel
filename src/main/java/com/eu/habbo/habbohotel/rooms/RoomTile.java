package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.entities.RoomEntity;
import com.eu.habbo.habbohotel.rooms.entities.units.RoomUnit;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RoomTile {
    @Getter
    private final short x;
    @Getter
    private final short y;
    @Getter
    private final short z;
    private final HashSet<RoomEntity> units;
    @Setter
    @Getter
    private RoomTileState state;
    private double stackHeight;
    private boolean allowStack = true;
    @Getter
    @Setter
    private RoomTile previous = null;
    private boolean diagonally;
    @Getter
    private short gCosts;
    private short hCosts;


    public RoomTile(short x, short y, short z, RoomTileState state, boolean allowStack) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stackHeight = z;
        this.state = state;
        this.setAllowStack(allowStack);
        this.units = new HashSet<>();
    }

    public RoomTile(RoomTile tile) {
        this.x = tile.x;
        this.y = tile.y;
        this.z = tile.z;
        this.stackHeight = tile.stackHeight;
        this.state = tile.state;
        this.allowStack = tile.allowStack;
        this.diagonally = tile.diagonally;
        this.gCosts = tile.gCosts;
        this.hCosts = tile.hCosts;

        if (this.state == RoomTileState.INVALID) {
            this.allowStack = false;
        }
        this.units = tile.units;
    }

    public RoomTile() {
        x = 0;
        y = 0;
        z = 0;
        this.stackHeight = 0;
        this.state = RoomTileState.INVALID;
        this.allowStack = false;
        this.diagonally = false;
        this.gCosts = 0;
        this.hCosts = 0;
        this.units = null;
    }

    public double getStackHeight() {
        return this.stackHeight;
    }

    public void setStackHeight(double stackHeight) {
        if (this.state == RoomTileState.INVALID) {
            this.stackHeight = Short.MAX_VALUE;
            this.allowStack = false;
            return;
        }

        if (stackHeight >= 0 && stackHeight != Short.MAX_VALUE) {
            this.stackHeight = stackHeight;
            this.allowStack = true;
        } else {
            this.allowStack = false;
            this.stackHeight = this.z;
        }
    }

    public boolean getAllowStack() {
        if (this.state == RoomTileState.INVALID) {
            return false;
        }

        return this.allowStack;
    }

    public void setAllowStack(boolean allowStack) {
        this.allowStack = allowStack;
    }

    public short relativeHeight() {
        if (this.state == RoomTileState.INVALID) {
            return Short.MAX_VALUE;
        } else if (!this.allowStack && (this.state == RoomTileState.BLOCKED || this.state == RoomTileState.SIT)) {
            return 64 * 256;
        }

        return this.allowStack ? (short) (this.getStackHeight() * 256.0) : 64 * 256;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RoomTile &&
                ((RoomTile) o).x == this.x &&
                ((RoomTile) o).y == this.y;
    }

    public RoomTile copy() {
        return new RoomTile(this);
    }

    public double distance(RoomTile roomTile) {
        double x = (double) this.x - roomTile.x;
        double y = (double) this.y - roomTile.y;
        return Math.sqrt(x * x + y * y);
    }

    public void isDiagonally(boolean isDiagonally) {
        this.diagonally = isDiagonally;
    }

    public int getfCosts() {
        return this.gCosts + this.hCosts;
    }

    public void setgCosts(RoomTile previousRoomTile) {
        this.setgCosts(previousRoomTile, this.diagonally ? RoomLayout.DIAGONALMOVEMENTCOST : RoomLayout.BASICMOVEMENTCOST);
    }

    private void setgCosts(short gCosts) {
        this.gCosts = gCosts;
    }

    void setgCosts(RoomTile previousRoomTile, int basicCost) {
        this.setgCosts((short) (previousRoomTile.getGCosts() + basicCost));
    }

    public int calculategCosts(RoomTile previousRoomTile) {
        if (this.diagonally) {
            return previousRoomTile.getGCosts() + 14;
        }

        return previousRoomTile.getGCosts() + 10;
    }

    public void sethCosts(RoomTile parent) {
        this.hCosts = (short) ((Math.abs(this.x - parent.x) + Math.abs(this.y - parent.y)) * (parent.diagonally ? RoomLayout.DIAGONALMOVEMENTCOST : RoomLayout.BASICMOVEMENTCOST));
    }

    public String toString() {
        return "RoomTile (" + this.x + ", " + this.y + ", " + this.z + "): h: " + this.hCosts + " g: " + this.gCosts + " f: " + this.getfCosts();
    }

    public boolean isWalkable() {
        return this.state == RoomTileState.OPEN;
    }

    public boolean is(short x, short y) {
        return this.x == x && this.y == y;
    }

    public List<RoomEntity> getEntities() {
        synchronized (this.units) {
            return new ArrayList<>(this.units);
        }
    }

    public void addUnit(RoomEntity entity) {
        synchronized (this.units) {
            if (!this.units.contains(entity)) {
                this.units.add(entity);
            }
        }
    }

    public void removeUnit(RoomEntity entity) {
        synchronized (this.units) {
            this.units.remove(entity);
        }
    }

    public boolean hasUnits() {
        synchronized (this.units) {
            return this.units.size() > 0;
        }
    }

    public boolean unitIsOnFurniOnTile(RoomUnit unit, Item item) {
        if ((unit.getCurrentPosition().getX() < this.x || unit.getCurrentPosition().getX() >= this.x + item.getLength()))
            return false;
        if (unit.getCurrentPosition().getY() < this.y) return false;
        return unit.getCurrentPosition().getY() < this.y + item.getWidth();
    }
}