package ru.infotecs.test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import ru.infotecs.test.model.Address;
import ru.infotecs.test.util.IpValidator;
import ru.infotecs.test.util.JsonUtil;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SftpClientApp {
    private static final String REMOTE_FILE = "addresses.json";
    private SftpManager sftpManager;
    private Scanner scanner;

    public static void main(String[] args) {
        new SftpClientApp().run();
    }

    public void run() {
        scanner = new Scanner(System.in);
        sftpManager = new SftpManager();

        System.out.println("Enter sftp-server connection details");
        System.out.println("Host: ");
        String host = scanner.nextLine();
        System.out.println("Port: ");
        int port = Integer.parseInt(scanner.nextLine());

        System.out.println("Login: ");
        String login = scanner.nextLine();
        System.out.println("Password: ");
        String password = scanner.nextLine();

        try {
            sftpManager.connect(host, port, login, password);
            System.out.println("Successfully connected to SFTP server");
            showMenu();
        } catch (Exception ex) {
            System.err.println("Connection failed: " + ex.getMessage());
        } finally {
            sftpManager.disconnect();
            scanner.close();
        }
    }

    private void showMenu() {
        boolean running = true;
        while (running) {
            System.out.println("Menu");
            System.out.println("1. Get domain-ip pairs");
            System.out.println("2. Get IP by domain name");
            System.out.println("3. Get domain by IP");
            System.out.println("4. Add new pair");
            System.out.println("5. Remove pair");
            System.out.println("6. Exit");
            System.out.println("Enter your choice: ");

            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "1":
                        listAddresses();
                        break;
                    case "2":
                        getIpByDomain();
                        break;
                    case "3":
                        getDomainByIp();
                        break;
                    case "4":
                        addAddress();
                        break;
                    case "5":
                        removeAddress();
                        break;
                    case "6":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            } catch (Exception ex) {
                System.out.println("Something went wrong: " + ex.getMessage());
            }
            System.out.println();
        }
    }

    private void listAddresses() throws SftpException, IOException {
        List<Address> list = loadAddresses();
        if (list.isEmpty()) {
            System.out.println("No addresses found");
        } else {
            System.out.println("Addresses:");
            for (Address address : list) {
                System.out.println(address);
            }
        }
    }

    private void getIpByDomain() throws SftpException, IOException {
        System.out.println("Enter domain: ");
        String domain = scanner.nextLine();

        get(address -> address.getDomain().equalsIgnoreCase(domain),
            address -> System.out.println("IP: " + address.getIp()),
            () -> System.out.println("No IP found"));
    }

    private void getDomainByIp() throws SftpException, IOException {
        System.out.println("Enter ip: ");
        String ip = scanner.nextLine();

        get(address -> address.getIp().equalsIgnoreCase(ip),
            address -> System.out.println("Domain: " + address.getDomain()),
            () -> System.out.println("No domain found"));
    }

    private void get(Predicate<Address> filter, Consumer<Address> resolve, Runnable reject) throws SftpException, IOException {
        List<Address> addresses = loadAddresses();
        Optional<Address> opt = addresses.stream()
            .filter(filter)
            .findFirst();

        if (opt.isPresent()) {
            resolve.accept(opt.get());
        } else {
            reject.run();
        }
    }

    private void addAddress() throws SftpException, IOException {
        System.out.println("Enter domain: ");
        String domain = scanner.nextLine();

        System.out.println("Enter ip: ");
        String ip = scanner.nextLine();

        if (!IpValidator.isValidIPv4(ip)) {
            System.out.println("Invalid IP");
            return;
        }

        List<Address> addresses = loadAddresses();
        boolean domainExists = addresses.stream().anyMatch(address -> address.getDomain().equalsIgnoreCase(domain));
        if (domainExists) {
            System.out.println("Domain already exists");
            return;
        }

        boolean ipExists = addresses.stream().anyMatch(address -> address.getIp().equalsIgnoreCase(ip));
        if (ipExists) {
            System.out.println("IP already exists");
            return;
        }

        addresses.add(new Address(domain, ip));
        saveAddresses(addresses);
        System.out.println("Successfully added address");
    }

    private void removeAddress() throws SftpException, IOException {
        System.out.println("Enter domain or IP to remove: ");
        String input = scanner.nextLine();

        List<Address> addresses = loadAddresses();
        boolean removed = addresses
            .removeIf(address -> address.getDomain().equalsIgnoreCase(input) || address.getIp().equalsIgnoreCase(input));

        if (removed) {
            System.out.println("Successfully removed address");
            saveAddresses(addresses);
        } else {
            System.out.println("Address not found");
        }
    }

    private void saveAddresses(List<Address> addresses) throws SftpException, IOException {
        sftpManager.writeFile(REMOTE_FILE, JsonUtil.serialize(addresses));
    }

    private List<Address> loadAddresses() throws SftpException, IOException {
        String json = sftpManager.readFile(REMOTE_FILE);
        List<Address> addresses = JsonUtil.parse(json);
        addresses.sort(Comparator.comparing(Address::getDomain));
        return addresses;
    }
}
