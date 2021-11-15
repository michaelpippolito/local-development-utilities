package com.michaelpippolito.utils.sftp;

import com.michaelpippolito.utils.server.ServerCommandStatus;
import com.michaelpippolito.utils.server.ServerManager;
import com.michaelpippolito.utils.server.ServerType;
import com.michaelpippolito.utils.server.ServerStatus;
import com.michaelpippolito.utils.sftp.request.StartSftpServerRequest;
import com.michaelpippolito.utils.sftp.response.StartSftpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SftpHelper {

    @Autowired
    private ServerManager serverManager;

    private Map<Integer, SshServer> sftpServers = new HashMap<Integer, SshServer> ();

    public StartSftpServerResponse startSftpServer(StartSftpServerRequest request) {
        log.info("Starting SFTP Server on port " + request.getPort() + "...");

        SshServer server;
        if (serverManager.isPortInUse(request.getPort())) {
            if (serverManager.isServerOfType(request.getPort(), ServerType.SFTP)) {
                if (sftpServers.containsKey(request.getPort())) {
                    server = sftpServers.get(request.getPort());
                } else {
                    serverManager.abandonPort(request.getPort());
                    server = createSftpServer(request.getPort());
                }
            } else {
                ServerType existingServerType = serverManager.getServerType(request.getPort());
                String errorMessage = "Failed to start SFTP Server on port " + request.getPort() + " -- port already in use by " + existingServerType;

                log.error(errorMessage);
                return new StartSftpServerResponse(
                        ServerCommandStatus.PORT_ALREADY_IN_USE,
                        ServerStatus.UP,
                        existingServerType,
                        errorMessage
                );
            }
        } else {
            server = createSftpServer(request.getPort());
        }

        if (serverManager.occupyPort(request.getPort(), ServerType.SFTP)) {
            return initializeSftpServer(server);
        } else {
            String errorMessage = "Failed occupying port " + request.getPort() + " for SFTP Server!";
            return new StartSftpServerResponse(
                    ServerCommandStatus.FAILED,
                    ServerStatus.DOWN,
                    ServerType.SFTP,
                    errorMessage
            );
        }
    }

    public StartSftpServerResponse startSftpServer(int port) {
        return startSftpServer(new StartSftpServerRequest(port, Collections.emptyList()));
    }

    private SshServer createSftpServer(int port) {
        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(port);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        server.setSubsystemFactories(Collections.<SubsystemFactory>singletonList(new SftpSubsystemFactory()));
        server.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException {
                return true;
            }
        });
        return server;
    }

    private StartSftpServerResponse initializeSftpServer(SshServer server) {
        if (!server.isStarted()) {
            try {
                server.start();
            } catch (IOException e) {
                String errorMessage = "Failed starting SFTP Server on port " + server.getPort() + " -- " + ExceptionUtils.getStackTrace(e);
                log.error(errorMessage);
                return new StartSftpServerResponse(
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
                return new StartSftpServerResponse(
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
        return new StartSftpServerResponse(
                ServerCommandStatus.SUCCESS,
                ServerStatus.UP,
                ServerType.SFTP,
                successMessage
        );
    }
}
