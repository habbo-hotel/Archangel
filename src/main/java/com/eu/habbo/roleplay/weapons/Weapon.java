package com.eu.habbo.roleplay.weapons;

import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class Weapon {

    @Getter
    private int id;

    @Getter
    @Setter
    private String displayName;
    @Getter
    private String uniqueName;
    @Getter
    @Setter
    private String type;
    @Getter
    @Setter
    private int minDamage;
    @Getter
    @Setter
    private int maxDamage;
    @Getter
    @Setter
    private int rangeInTiles;
    @Getter
    @Setter
    private double accuracy;
    @Getter
    @Setter
    private double reloadTime;
    @Setter
    private int ammoCapacity;
    @Getter
    @Setter
    private double weight;
    @Getter
    @Setter
    private int cooldownSeconds;
    private String specialAbilities;

    public String[] getSpecialAbilities() {
        return specialAbilities.split(",");
    }

    public void setSpecialAbilities(String[] newSpecialAbilities) {
        this.specialAbilities = Arrays.toString(newSpecialAbilities);
    }

    @Getter
    @Setter
    private int equipEffect;
    @Getter
    @Setter
    private String equipMessage;
    @Getter
    @Setter
    private String unequipMessage;
    @Getter
    @Setter
    private String attackMessage;

    public Weapon(ResultSet set) throws SQLException {
        this.load(set);
    }

    public void load(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.displayName = set.getString("display_name");
        this.uniqueName = set.getString("unique_name");
        this.type = set.getString("type");
        this.minDamage = set.getInt("min_damage");
        this.maxDamage = set.getInt("max_damage");
        this.rangeInTiles = set.getInt("range_in_tiles");
        this.accuracy = set.getInt("accuracy");
        this.reloadTime = set.getInt("reload_time");
        this.ammoCapacity = set.getInt("ammo_capacity");
        this.weight = set.getInt("weight");
        this.cooldownSeconds = set.getInt("cooldown_seconds");
        this.specialAbilities = set.getString("special_abilities");
        this.equipEffect = set.getInt("equip_effect");
        this.equipMessage = set.getString("equip_message");
        this.unequipMessage = set.getString("unequip_message");
        this.attackMessage = set.getString("attack_message");
    }




}