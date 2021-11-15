package com.michaelpippolito.utils.sftp.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
public class StartSftpServerRequest {
    @Getter @Setter
    private int port;

    @Getter @Setter
    private List<String> directories;
}
