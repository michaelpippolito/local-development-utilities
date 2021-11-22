import com.jcraft.jsch.*;
import com.michaelpippolito.utils.LocalUtiltiesApplication;
import com.michaelpippolito.utils.server.ServerCommandResponse;
import com.michaelpippolito.utils.server.ServerCommandStatus;
import com.michaelpippolito.utils.server.ServerStatus;
import com.michaelpippolito.utils.server.ServerType;
import com.michaelpippolito.utils.sftp.SftpConfig;
import com.michaelpippolito.utils.sftp.SftpHelper;
import com.michaelpippolito.utils.sftp.request.StartSftpServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LocalUtiltiesApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class SftpTests {

    @LocalServerPort
    private int port;

    @Value("${sftp.localDir}")
    private String localSftpDirectory;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SftpHelper sftpHelper;

    @Autowired
    private SftpConfig sftpConfig;

    @After
    public void cleanupTests() throws IOException {
        sftpHelper.stopAllSftpServers();
        FileUtils.cleanDirectory(new File(sftpConfig.getLocalDir()));
    }

    @Test
    public void startServerUrlParamTests() {
        String url = "http://localhost:" + port + "/sftp/start";

        /*
            Case 1: Verify when no servers are running and a valid port is sent that an SFTP Server is started and a
            successful response is received
         */
        log.info("Case 1: Verify when no servers are running and a valid port is sent that an SFTP Server is started and a successful response is received");
        int case1Port = SocketUtils.findAvailableTcpPort();
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case1Port, null, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );
        verifySftpServer(case1Port);

        /*
            Case 2: Verify when a port already occupied by an SFTP Server is sent that a successful response is
            received
         */
        log.info("Case 2: Verify when a port already occupied by an SFTP Server is sent that a successful response is received");
        int case2Port = case1Port;
        ServerCommandResponse case2Response = restTemplate.postForObject(url + "/" + case2Port, null, ServerCommandResponse.class);
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case2Port, null, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );

        /*
            Case 3: Verify the application is able to start multiple SFTP Servers on multiple ports
         */
        log.info("Case 3:Verify the application is able to start multiple SFTP Servers on multiple ports");
        int case3Port = SocketUtils.findAvailableTcpPort();
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case3Port, null, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );
        verifySftpServer(case3Port);

        /*
            Case 4: Verify when a port occupied by something other than an SFTP Server is sent that a failure response
            is received indicating the port's current use
         */
        log.info("Case 4: Verify when a port occupied by something other than an SFTP Server is sent that a failure response is received indicating the port's current use");
        int case4Port = port;
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case4Port, null, ServerCommandResponse.class),
                ServerCommandStatus.PORT_ALREADY_IN_USE,
                ServerStatus.UP,
                ServerType.APPLICATION
        );
    }

    @Test
    public void startServerRequestBodyTests() {
        String url = "http://localhost:" + port + "/sftp/start";

        /*
            Case 1: Verify when no servers are running and a valid port is sent without any directories that an SFTP
            Server is started and a successful response is received
         */
        log.info("Case 1: Verify when no servers are running and a valid port is sent without any directories specified that an SFTP Server is started and a successful response is received");
        int case1Port = SocketUtils.findAvailableTcpPort();
        StartSftpServerRequest case1Request = new StartSftpServerRequest(case1Port, null);
        verifyServerCommandResponse(
                restTemplate.postForObject(url, case1Request, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );
        verifySftpServer(case1Port, case1Request.getDirectories());

        /*
            Case 2: Verify a valid port is sent with an empty directories list specified that an SFTP Server is started
            and a successful response is received
         */
        log.info("Case 2: Verify when a valid port is sent with an empty directories list specified that an SFTP Server is started and a successful response is received");
        int case2Port = SocketUtils.findAvailableTcpPort();
        StartSftpServerRequest case2Request = new StartSftpServerRequest(case2Port, Collections.emptyList());
        verifyServerCommandResponse(
                restTemplate.postForObject(url, case2Request, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );
        verifySftpServer(case2Port, case2Request.getDirectories());

        /*
            Case 3: Verify when a valid port is sent with a directories list specified that an SFTP Server is started, a
            successful response is received, and all directories are created
         */
        log.info("Case 3: Verify when a valid port is sent with a directories list specified that an SFTP Server is started, a successful response is received, and all directories are created");
        int case3Port = SocketUtils.findAvailableTcpPort();
        StartSftpServerRequest case3Request = new StartSftpServerRequest(case3Port, Arrays.asList("1", "2/2_1"));
        verifyServerCommandResponse(
                restTemplate.postForObject(url, case3Request, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );
        verifySftpServer(case3Port, case3Request.getDirectories());

        /*
            Case 4: Verify when a port already occupied by an SFTP Server is sent that a successful response is
            received
         */
        log.info("Case 4: Verify when a port already occupied by an SFTP Server is sent that a successful response is received");
        int case4Port = case1Port;
        ServerCommandResponse case4Response = restTemplate.postForObject(url + "/" + case4Port, null, ServerCommandResponse.class);
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case4Port, null, ServerCommandResponse.class),
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP
        );

        /*
            Case 5: Verify when a port occupied by something other than an SFTP Server is sent that a failure response
            is received indicating the port's current use
         */
        log.info("Case 5: Verify when a port occupied by something other than an SFTP Server is sent that a failure response is received indicating the port's current use");
        int case5Port = port;
        verifyServerCommandResponse(
                restTemplate.postForObject(url + "/" + case5Port, null, ServerCommandResponse.class),
                ServerCommandStatus.PORT_ALREADY_IN_USE,
                ServerStatus.UP,
                ServerType.APPLICATION
        );
    }

    private void verifyServerCommandResponse(
            ServerCommandResponse actual,
            ServerCommandStatus expectedCommandStatus,
            ServerStatus expectedServerStatus,
            ServerType expectedServerType
    ) {
        assertThat(actual.getCommandStatus()).isEqualTo(expectedCommandStatus);
        assertThat(actual.getServerStatus()).isEqualTo(expectedServerStatus);
        assertThat(actual.getServerType()).isEqualTo(expectedServerType);
    }

    private void verifySftpServer(int port, List<String> directories) {
        /*
            Verify that a connection can be established to the SFTP Server
         */
        JSch jsch = new JSch();
        ChannelSftp channelSftp = null;
        Session session = null;
        try {
            session = jsch.getSession("test", "localhost", port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            assertThat(session.isConnected()).isTrue();
            assertThat(session.getPort()).isEqualTo(port);

            channelSftp = (ChannelSftp)session.openChannel("sftp");
            channelSftp.connect();
            assertThat(channelSftp.isConnected()).isTrue();
        } catch (JSchException e) {
            log.error("Failed testing SFTP Server connectivity -- {}", ExceptionUtils.getStackTrace(e));
            Assertions.fail();
        }

        /*
            If directory list is provided, verify each directory exists
         */
        if (directories != null && !directories.isEmpty()) {
            try {
                for (String directory : directories) {
                    SftpATTRS attrs = channelSftp.stat(directory);
                    if (attrs != null) {
                        assertThat(attrs.isDir()).isTrue();
                    } else {
                        log.error("Directory {} does not exist or is not a directory!", directory);
                        Assertions.fail();
                    }
                }
            } catch (SftpException e) {
                log.error("Failed testing the creation of directories -- {}", ExceptionUtils.getStackTrace(e));
                Assertions.fail();
            }
        }

        /*
            Verify file write, read, & delete
         */
        try {
            String testFileName = "test_file.txt";
            String testFilePath = this.getClass().getResource(testFileName).toURI().getPath();
            String testFileContents = readFromInputStream(this.getClass().getResourceAsStream(testFileName));

            // File write
            File localFile = new File(testFilePath);
            channelSftp.put(localFile.getAbsolutePath(), testFileName);
            Vector remoteFiles = channelSftp.ls("*." + FilenameUtils.getExtension(testFileName));
            assertThat(remoteFiles.size()).isEqualTo(1);
            ChannelSftp.LsEntry remoteFileEntry = (ChannelSftp.LsEntry) remoteFiles.get(0);
            assertThat(remoteFileEntry.getFilename()).isEqualTo(testFileName);

            // File download
            String downloadedFileName = "test_download" + FilenameUtils.getExtension(testFileName);
            channelSftp.get(testFileName, localFile.getAbsolutePath().replace(testFileName, downloadedFileName));
            File downloadedFile = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(downloadedFileName)).getFile());
            assertThat(readFromInputStream(new FileInputStream(downloadedFile))).isEqualTo(testFileContents);
            if (!downloadedFile.delete()) {
                log.warn("{} not deleted!", downloadedFile.getAbsolutePath());
            }

            // File read
            assertThat(readFromInputStream(channelSftp.get(testFileName))).isEqualTo(testFileContents);

            // File delete
            channelSftp.rm(testFileName);
            remoteFiles = channelSftp.ls("*." + FilenameUtils.getExtension(testFileName));
            assertThat(remoteFiles.size()).isEqualTo(0);

            channelSftp.exit();
            session.disconnect();
        } catch (SftpException e) {
            log.error("Failed testing SFTP File operations -- {}", ExceptionUtils.getStackTrace(e));
            Assertions.fail();
        } catch (IOException e) {
            log.error("Failed testing SFTP File Read/Download Operations -- {}", ExceptionUtils.getStackTrace(e));
            Assertions.fail();
        } catch (URISyntaxException e) {
            log.error("Failed opening test file -- {}", ExceptionUtils.getStackTrace(e));
            Assertions.fail();
        }
    }

    private void verifySftpServer(int port) {
        verifySftpServer(port, Collections.emptyList());
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
