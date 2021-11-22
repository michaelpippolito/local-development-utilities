package com.michaelpippolito.utils.sftp;

import com.michaelpippolito.utils.server.ServerCommandResponse;
import com.michaelpippolito.utils.server.ServerCommandStatus;
import com.michaelpippolito.utils.sftp.request.StartSftpServerRequest;
import com.michaelpippolito.utils.sftp.request.StopSftpServerRequest;
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
    public ResponseEntity<ServerCommandResponse> sftpResponse(@PathVariable int port) {
        return sftpResponse(sftpHelper.startSftpServer(port));
    }

    @PostMapping("/sftp/start")
    public ResponseEntity<ServerCommandResponse> sftpResponse(@RequestBody StartSftpServerRequest request) {
        return sftpResponse(sftpHelper.startSftpServer(request));
    }

    @PostMapping("/sftp/stop/{port}")
    public ResponseEntity<ServerCommandResponse> stopSftpServer(@PathVariable int port) {
        return sftpResponse(sftpHelper.stopSftpServer(port));
    }

    @PostMapping("/sftp/stop")
    public ResponseEntity<ServerCommandResponse> stopSftpServer(@RequestBody StopSftpServerRequest request) {
        return sftpResponse(sftpHelper.stopSftpServer(request));
    }

    private ResponseEntity<ServerCommandResponse> sftpResponse(ServerCommandResponse response) {
        if (response.getCommandStatus().equals(ServerCommandStatus.SUCCESS)) {
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
