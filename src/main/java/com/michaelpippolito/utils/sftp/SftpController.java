package com.michaelpippolito.utils.sftp;

import com.michaelpippolito.utils.server.ServerCommandStatus;
import com.michaelpippolito.utils.sftp.request.StartSftpServerRequest;
import com.michaelpippolito.utils.sftp.response.StartSftpServerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SftpController {

    @Autowired
    private SftpHelper sftpHelper;

    @PostMapping("/sftp/start/{port}")
    public ResponseEntity<StartSftpServerResponse> startSftpServer(@PathVariable int port) {
        return startSftpServer(sftpHelper.startSftpServer(port));
    }

    @PostMapping("/sftp/start")
    public ResponseEntity<StartSftpServerResponse> startSftpServer(@RequestBody StartSftpServerRequest request) {
        return startSftpServer(sftpHelper.startSftpServer(request));
    }

    private ResponseEntity<StartSftpServerResponse> startSftpServer(StartSftpServerResponse response) {
        if (response.getCommandStatus().equals(ServerCommandStatus.SUCCESS)) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
