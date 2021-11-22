package com.michaelpippolito.utils.sftp.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class StopSftpServerRequest {
    @Getter @Setter
    private int port;
}
