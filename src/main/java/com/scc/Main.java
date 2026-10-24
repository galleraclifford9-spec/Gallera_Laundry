package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        DatabaseInitializer.initialize();
        UserDao userDao = new UserDao();
        Scanner sc = new Scanner(System.in);
        new Authentication(userDao, sc).start();

        sc.close();
    }
}