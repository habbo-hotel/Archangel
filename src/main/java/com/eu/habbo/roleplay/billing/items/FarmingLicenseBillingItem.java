package com.eu.habbo.roleplay.billing.items;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.roleplay.government.GovernmentManager;
import com.eu.habbo.roleplay.government.LicenseType;

public class FarmingLicenseBillingItem implements BillingItem{

    private  int userID;
    private int chargedByUserID;

    public FarmingLicenseBillingItem(int userID, int chargedByUserID) {
        this.userID = userID;
        this.chargedByUserID = chargedByUserID;
    }

    @Override
    public BillType getType() {
        return BillType.FARMING_LICENSE;
    }

    @Override
    public String getTitle() {
        return "Farming License";
    }

    @Override
    public String getDescription() {
        return "Fee for processing";
    }

    @Override
    public int getAmountOwed() {
        return 150;
    }

    @Override
    public int getAmountPaid() {
        return 0;
    }

    @Override
    public int getChargedByCorpID() {
        return GovernmentManager.getInstance().getPoliceCorp().getGuild().getId();
    }

    @Override
    public int getChargedByUserID() {
        return this.chargedByUserID;
    }

    @Override
    public int getUserID() {
        return this.userID;
    }

    @Override
    public void onBillPaid(Habbo habbo) {
        habbo.getInventory().getLicensesComponent().createLicense(LicenseType.FARMING);
        habbo.shout(Emulator.getTexts().getValue("roleplay.license.received").replace(":license", this.getTitle()));
    }
}