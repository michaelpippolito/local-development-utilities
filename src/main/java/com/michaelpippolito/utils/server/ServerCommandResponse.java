package com.michaelpippolito.utils.server;

import lombok.Getter;
import lombok.Setter;

public abstract class ServerCommandResponse {
    @Getter @Setter
    private ServerCommandStatus commandStatus;

    @Getter @Setter
    private ServerStatus serverStatus;

    @Getter @Setter
    private ServerType serverType;

    @Getter @Setter
    private String message;
}
