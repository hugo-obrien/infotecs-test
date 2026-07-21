package ru.infotecs.test.util;

import ru.infotecs.test.model.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {
    private static final String regex = "\\{\\s*\"domain\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"ip\"\\s*:\\s*\"([^\"]+)\"\\s*\\}";

    public static List<Address> parse(String json) {
        List<Address> addresses = new ArrayList<>();

        if (json == null || json.isEmpty()) {
            return addresses;
        }

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(json);
        while (m.find()) {
            addresses.add(new Address(m.group(1), m.group(2)));
        }
        return addresses;
    }

    public static String serialize(List<Address> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"addresses\": [\n");
        for (int i = 0; i < list.size(); i++) {
            Address a = list.get(i);
            sb.append("    {\"domain\":\"").append(a.getDomain()).append("\", \"ip\":\"").append(a.getIp()).append("\"}");
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
