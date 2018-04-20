package com.jingdianbao.service.impl;

import com.jingdianbao.entity.LoginAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LoginTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginTask.class);

    @Autowired
    private LoginService loginService;

    private CopyOnWriteArrayList<LoginAccount> loginAccountList;

    @PostConstruct
    private void init() {
        loginAccountList = new CopyOnWriteArrayList<>();
        new Thread(() -> {
            while (true) {
                if (loginAccountList.isEmpty()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {

                    }
                } else {
                    try {
                        LoginAccount loginAccount = loginAccountList.get(0);
                        boolean result = loginService.doLoginDmp(loginAccount.getUserName(), loginAccount.getPassword());
                        loginAccountList.remove(0);
                        if (!result) {
                            loginAccountList.add(loginAccount);
                        }
                    } catch (Throwable e) {
                        LOGGER.error("", e);
                    }
                }
            }
        }).start();
    }

    public void addLoginTask(LoginAccount loginAccount) {
        if (!loginAccountList.contains(loginAccount)) {
            loginAccountList.add(loginAccount);
        }
    }

    public int getTaskSize() {
        return loginAccountList.size();
    }
}
