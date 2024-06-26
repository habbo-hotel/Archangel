package com.eu.habbo.roleplay.commands.bank;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.roleplay.corp.Corp;
import com.eu.habbo.roleplay.corp.CorpManager;
import com.eu.habbo.roleplay.database.HabboBankAccountRepository;
import com.eu.habbo.roleplay.users.HabboBankAccount;

public class BankAccountWithdrawCommand extends Command  {

    public BankAccountWithdrawCommand() {
        super("cmd_bank_withdraw");
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        if (params == null) {
            return true;
        }

        if (params.length != 3) {
            return true;
        }

        int corpID = Integer.parseInt(params[1]);
        Corp bankCorp = CorpManager.getInstance().getCorpByID(corpID);

        int withdrawAmount =Integer.parseInt(params[2]);

        HabboBankAccount bankAccount = HabboBankAccountRepository.getInstance().getByUserAndCorpID(gameClient.getHabbo().getHabboInfo().getId(), corpID);

        if (bankCorp == null || bankAccount == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("roleplay.bank.not_found"));
            return true;
        }

        if (bankAccount.getCheckingBalance() < withdrawAmount) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("roleplay.bank.balance.not_enough"));
            return true;
        }

        bankAccount.setCheckingBalance(bankAccount.getCheckingBalance() - withdrawAmount);
        HabboBankAccountRepository.getInstance().update(bankAccount);

        gameClient.getHabbo().shout(Emulator.getTexts()
                .getValue("roleplay.bank.balance.withdraw")
                .replace(":credits", String.valueOf(withdrawAmount))
        );

        return true;
    }
}
