package com.michaelpippolito.utils.sftp;

import com.michaelpippolito.utils.server.*;
import com.michaelpippolito.utils.sftp.request.StartSftpServerRequest;
import com.michaelpippolito.utils.sftp.request.StopSftpServerRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.io.nio2.Nio2ServiceFactoryFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.SubsystemFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@Component
@Slf4j
public class SftpHelper {

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private SftpConfig config;

    private Map<Integer, SshServer> sftpServers = new HashMap<Integer, SshServer>();

    @PostConstruct
    public void init() {
        if (config.isAutoStartup()) {
            if (!config.getDefaultServers().isEmpty()) {
                for (int port : config.getDefaultServers().keySet()) {
                    List<String> directories = config.getDefaultServers().get(port);
                    startSftpServer(new StartSftpServerRequest(port, directories));
                }
            }
        }
    }

    public ServerCommandResponse startSftpServer(StartSftpServerRequest request) {
        log.info("Starting SFTP Server on port " + request.getPort() + "...");

        SshServer server;
        if (serverManager.isPortInUse(request.getPort())) {
            if (serverManager.isServerOfType(request.getPort(), ServerType.SFTP)) {
                if (sftpServers.containsKey(request.getPort())) {
                    server = sftpServers.get(request.getPort());
                } else {
                    /*
                        This should never happen, but including it in case something goes horribly wrong
                     */
                    serverManager.abandonPort(request.getPort());
                    server = createSftpServer(request.getPort());
                }
            } else {
                ServerType existingServerType = serverManager.getServerType(request.getPort());
                String errorMessage = "Failed to start SFTP Server on port " + request.getPort() + " -- port already in use by " + existingServerType;

                log.error(errorMessage);
                return new ServerCommandResponse(
                        ServerCommandStatus.PORT_ALREADY_IN_USE,
                        ServerStatus.UP,
                        existingServerType,
                        errorMessage
                );
            }
        } else {
            server = createSftpServer(request.getPort());
        }

        ServerCommandResponse response = initializeSftpServer(server);
        if (response.getServerStatus().equals(ServerStatus.UP) && response.getServerType().equals(ServerType.SFTP)) {
            if (request.getDirectories() != null) {
                if (!createDirectories(request.getDirectories())) {
                    response.setMessage(
                            response.getMessage() + " -- Failed to create directories! Please create manually!"
                    );
                }
            }
        }
        return response;
    }

    public ServerCommandResponse startSftpServer(int port) {
        return startSftpServer(new StartSftpServerRequest(port, Collections.emptyList()));
    }

    public ServerCommandResponse stopSftpServer(StopSftpServerRequest request) {
        log.info("Stopping SFTP Server on port " + request.getPort() + "...");

        if (serverManager.isPortInUse(request.getPort())) {
            if (sftpServers.containsKey(request.getPort())) {
                SshServer server = sftpServers.get(request.getPort());
                return shutdownSftpServer(server);
            } else {
                serverManager.abandonPort(request.getPort());
            }
        }

        /*
            This should never happen, but including it in case something goes horribly wrong
        */
        if (sftpServers.containsKey(request.getPort())) {
            SshServer server = sftpServers.get(request.getPort());
            return shutdownSftpServer(server);
        } else {
            String successMessage = "Stopped SFTP Server on port " + request.getPort() + "!";
            log.info(successMessage);
            return new ServerCommandResponse(
                    ServerCommandStatus.SUCCESS,
                    ServerStatus.DOWN,
                    ServerType.SFTP,
                    successMessage
            );
        }
    }

    public ServerCommandResponse stopSftpServer(int port) {
        return stopSftpServer(new StopSftpServerRequest(port));
    }

    public void stopAllSftpServers() {
        HashSet<Integer> sftpPorts = new HashSet<Integer>(sftpServers.keySet());
        for (int port : sftpPorts) {
            stopSftpServer(new StopSftpServerRequest(port));
        }
    }

    private SshServer createSftpServer(int port) {
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(port);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.setIoServiceFactoryFactory(new Nio2ServiceFactoryFactory());
        server.setSubsystemFactories(Collections.<SubsystemFactory>singletonList(new SftpSubsystemFactory()));
        server.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException {
                return true;
            }
        });
        if (!StringUtils.isEmpty(config.getLocalDir())) {
            server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(config.getLocalDir())));
        }
        return server;
    }

    private ServerCommandResponse initializeSftpServer(SshServer server) {
        if (serverManager.occupyPort(server.getPort(), ServerType.SFTP)) {
            if (!server.isStarted()) {
                try {
                    server.start();
                } catch (IOException e) {
                    String errorMessage = "Failed starting SFTP Server on port " + server.getPort() + " -- " + ExceptionUtils.getStackTrace(e);
                    log.error(errorMessage);
                    return new ServerCommandResponse(
                            ServerCommandStatus.FAILED,
                            ServerStatus.DOWN,
                            ServerType.SFTP,
                            errorMessage
                    );
                }
            }

            if (!server.isOpen()) {
                try {
                    server.open();
                } catch (IOException e) {
                    String errorMessage = "Failed opening SFTP Server on port " + server.getPort() + " -- " + ExceptionUtils.getStackTrace(e);
                    log.error(errorMessage);
                    return new ServerCommandResponse(
                            ServerCommandStatus.FAILED,
                            ServerStatus.DOWN,
                            ServerType.SFTP,
                            errorMessage
                    );
                }
            }

            String successMessage = "SFTP Server running on port " + server.getPort() + "!";
            log.info(successMessage);

            sftpServers.put(server.getPort(), server);
            return new ServerCommandResponse(
                    ServerCommandStatus.SUCCESS,
                    ServerStatus.UP,
                    ServerType.SFTP,
                    successMessage
            );
        } else {
            String errorMessage = "Failed occupying port " + server.getPort() + " for SFTP Server!";
            log.error(errorMessage);
            return new ServerCommandResponse(
                    ServerCommandStatus.FAILED,
                    ServerStatus.DOWN,
                    ServerType.SFTP,
                    errorMessage
            );
        }
    }

    private boolean createDirectories(List<String> directories) {
        for (String directory : directories) {
            log.info("Creating directory {}...", directory);
            if (!new File(config.getLocalDir() + File.separator + directory).mkdirs()) {
                log.error("Failed creating directories -- {}", directory);
                return false;
            }
            log.info("Created directory {}!", directory);
        }
        return true;
    }

    private ServerCommandResponse shutdownSftpServer(SshServer server) {
        if (server.isOpen()) {
            try {
                server.close();
            } catch (IOException e) {
                String errorMessage = "Failed closing SFTP Server on port " + server.getPort() + " -- " + ExceptionUtils.getStackTrace(e);
                log.error(errorMessage);
                return new ServerCommandResponse(
                        ServerCommandStatus.FAILED,
                        ServerStatus.UP,
                        ServerType.SFTP,
                        errorMessage
                );
            }
        }

        if (server.isStarted()) {
            try {
                server.stop();
            } catch (IOException e) {
                String errorMessage = "Failed stopping SFTP Server on port " + server.getPort() + " -- " + ExceptionUtils.getStackTrace(e);
                log.error(errorMessage);
                return new ServerCommandResponse(
                        ServerCommandStatus.FAILED,
                        ServerStatus.UP,
                        ServerType.SFTP,
                        errorMessage
                );
            }
        }

        String successMessage = "Stopped SFTP Server on port " + server.getPort() + "!";
        log.info(successMessage);
        sftpServers.remove(server.getPort());
        serverManager.abandonPort(server.getPort());
        return new ServerCommandResponse(
                ServerCommandStatus.SUCCESS,
                ServerStatus.DOWN,
                ServerType.SFTP,
                successMessage
        );
    }
}
