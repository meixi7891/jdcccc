package com.jingdianbao.service.impl;

import com.jingdianbao.entity.LoginAccount;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class AccountService {

    @Value("${dmp.account.file.path}")
    private String accountFilePath;

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountService.class);

    public LoginAccount loadRandomDmpAccount() {
        try {
            List<String> list = FileUtils.readLines(new File(accountFilePath), "utf-8");
            String line = list.get(new Random().nextInt(list.size()));
            String[] ss = line.split("\\s+");
            LoginAccount loginAccount = new LoginAccount(ss[0], ss[1]);
            return loginAccount;
        } catch (IOException e) {
            LOGGER.error("", e);
        }
        return null;
    }

    public List<LoginAccount> allAccounts() {
        List<LoginAccount> accountList = new ArrayList<>();
        try {
            FileUtils.readLines(new File(accountFilePath), "utf-8").stream().filter(s -> !s.trim().isEmpty()).map(s -> s.split("\\s+")).forEach(ss -> accountList.add(new LoginAccount(ss[0], ss[1])));
        } catch (IOException e) {
            LOGGER.error("", e);
        }
        return accountList;
    }


    public int accountCount() {
        try {
            List<String> list = FileUtils.readLines(new File(accountFilePath), "utf-8");
            return list.size();
        } catch (IOException e) {
            LOGGER.error("", e);
            return 0;
        }

    }
}
