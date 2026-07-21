package ru.infotecs.test.util;

public class IpValidator {

    public static boolean isValidIPv4(String ip) {
        if (ip == null) return false;
        String regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(regex);
    }
}
