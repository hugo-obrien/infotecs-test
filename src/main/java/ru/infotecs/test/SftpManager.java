package ru.infotecs.test;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.Properties;

public class SftpManager {

    private Session session;
    private ChannelSftp channel;

    public void connect(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }

    public String readFile(String path) throws IOException, SftpException {
        try (InputStream is = channel.get(path)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (SftpException ex) {
            if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return "";
            }
            throw ex;
        }
    }

    public void writeFile(String path, String content) throws SftpException, IOException {
        try (OutputStream os = channel.put(path, ChannelSftp.OVERWRITE)) {
            os.write(content.getBytes());
        }
    }

    public void disconnect() {
        if (channel != null) channel.disconnect();
        if (session != null) session.disconnect();
    }
}
