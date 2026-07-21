package ru.infotecs.test;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import ru.infotecs.test.model.Address;
import ru.infotecs.test.util.IpValidator;
import ru.infotecs.test.util.JsonUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SftpClientTest {

    private static final String TEST_DIR = System.getProperty("java.io.tmpdir") + "/sftp_test";
    private static final int PORT = 2222;
    private static final String USER = "testuser";
    private static final String PASS = "testpass";
    private static final String TEST_FILE = TEST_DIR + "/addresses.json";

    private static SshServer sshd;

    @BeforeSuite
    public void startSftpServer() throws IOException {
        new File(TEST_DIR).mkdirs();

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(PORT);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(TEST_DIR, "hostkey.ser")));
        sshd.setPasswordAuthenticator((u, p, s) -> USER.equals(u) && PASS.equals(p));
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

        // Настраиваем виртуальную файловую систему: корень SFTP = TEST_DIR
        VirtualFileSystemFactory fsFactory = new VirtualFileSystemFactory();
        fsFactory.setDefaultHomeDir(Paths.get(TEST_DIR));
        sshd.setFileSystemFactory(fsFactory);

        sshd.start();
    }

    @AfterSuite
    public void stopSftpServer() throws IOException {
        if (sshd != null) {
            sshd.stop();
        }
    }

    @BeforeMethod
    public void setUpFile() throws IOException {
        File localFile = Paths.get(TEST_DIR, "addresses.json").toFile();
        try (FileWriter fw = new FileWriter(localFile)) {
            fw.write("{\"addresses\": []}");
        }
    }

    @Test
    public void testPositiveConnection() throws Exception {
        SftpManager manager = new SftpManager();
        manager.connect("localhost", PORT, USER, PASS);
        manager.disconnect();
    }

    @Test(expectedExceptions = Exception.class)
    public void testNegativeConnection() throws Exception {
        SftpManager manager = new SftpManager();
        manager.connect("localhost", PORT, "wrong_user", "wrong_pass");
    }

    @Test
    public void testAddAndListSorted() throws Exception {
        SftpManager manager = new SftpManager();
        manager.connect("localhost", PORT, USER, PASS);

        // Используем относительный путь для SFTP
        String remoteFileName = "addresses.json";

        String json = manager.readFile(remoteFileName);
        List<Address> list = JsonUtil.parse(json);
        list.add(new Address("first.domain", "192.168.0.1"));
        list.add(new Address("second.domain", "192.168.0.2"));
        list.add(new Address("third.domain", "192.168.0.3"));
        list.add(new Address("fourth.domain", "192.168.0.4"));
        list.sort(java.util.Comparator.comparing(Address::getDomain));

        manager.writeFile(remoteFileName, JsonUtil.serialize(list));

        String savedJson = manager.readFile(remoteFileName);
        List<Address> savedList = JsonUtil.parse(savedJson);

        Assert.assertEquals(savedList.size(), 4, "List must contains 4 elements");
        Assert.assertEquals(savedList.get(0).getDomain(), "first.domain", "first.domain must be first");
        Assert.assertEquals(savedList.get(1).getDomain(), "fourth.domain", "second.domain must be second");
        Assert.assertEquals(savedList.get(2).getDomain(), "second.domain", "second.domain must be third");
        Assert.assertEquals(savedList.get(3).getDomain(), "third.domain", "second.domain must be fourth");

        manager.disconnect();
    }

    @Test
    public void testIpValidation() {
        Assert.assertTrue(IpValidator.isValidIPv4("192.168.0.1"));
        Assert.assertTrue(IpValidator.isValidIPv4("0.0.0.0"));
        Assert.assertTrue(IpValidator.isValidIPv4("255.255.255.255"));

        Assert.assertFalse(IpValidator.isValidIPv4("256.1.1.1"));
        Assert.assertFalse(IpValidator.isValidIPv4("192.168.1"));
        Assert.assertFalse(IpValidator.isValidIPv4("abc.def.ghi.jkl"));
        Assert.assertFalse(IpValidator.isValidIPv4("192.168.0.1.1"));
    }

    @Test
    public void testUniquenessLogic() {
        List<Address> list = Arrays.asList(
            new Address("test.com", "1.1.1.1"),
            new Address("test2.com", "2.2.2.2")
        );

        boolean domainExists = list.stream().anyMatch(a -> a.getDomain().equalsIgnoreCase("test.com"));
        boolean ipExists = list.stream().anyMatch(a -> a.getIp().equals("1.1.1.1"));

        Assert.assertTrue(domainExists, "Домен должен быть найден");
        Assert.assertTrue(ipExists, "IP должен быть найден");
    }
}
