package com.cibicom.data.internal;

public class Api {
    private static final String hostName = "localhost";
    private static final int port = 8080;

    public static String getHostName() {
        return hostName;
    }

    public static int getPort() {
        return port;
    }

    public static boolean isIdValid(String id) {
        String[] appIdGroups = id.split("-");
        return appIdGroups[0].length() == 8 && appIdGroups[1].length() == 4 &&
                appIdGroups[2].length() == 4 && appIdGroups[3].length() == 4 &&
                appIdGroups[4].length() == 12;
    }

}
