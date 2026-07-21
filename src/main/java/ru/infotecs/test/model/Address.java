package ru.infotecs.test.model;

public class Address {
    private final String domain;
    private final String ip;

    public Address(String domain, String ip) {
        this.domain = domain;
        this.ip = ip;
    }

    public String getDomain() {
        return domain;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "Domain: " + domain + ", IP: " + ip;
    }
}
