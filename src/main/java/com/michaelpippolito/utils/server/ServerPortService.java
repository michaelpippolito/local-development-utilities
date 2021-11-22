package com.michaelpippolito.utils.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ServerPortService {

    @Autowired
    private ServerManager serverManager;

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        serverManager.occupyPort(event.getWebServer().getPort(), ServerType.APPLICATION);
    }
}
