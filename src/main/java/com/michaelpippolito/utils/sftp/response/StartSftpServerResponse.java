package com.michaelpippolito.utils.sftp.response;

import com.michaelpippolito.utils.server.ServerCommandResponse;
import com.michaelpippolito.utils.server.ServerCommandStatus;
import com.michaelpippolito.utils.server.ServerStatus;
import com.michaelpippolito.utils.server.ServerType;

public class StartSftpServerResponse extends ServerCommandResponse {
    public StartSftpServerResponse(ServerCommandStatus commandStatus, ServerStatus serverStatus, ServerType serverType, String message) {
        this.setCommandStatus(commandStatus);
        this.setServerStatus(serverStatus);
        this.setServerType(serverType);
        this.setMessage(message);
    }
}
