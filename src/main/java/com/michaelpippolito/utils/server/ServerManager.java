package com.michaelpippolito.utils.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ServerManager {
    @Getter @Setter
    private Map<Integer, ServerType> servers = new HashMap<>();

    public boolean isPortInUse(int port) {
        return servers.containsKey(port);
    }

    public boolean isServerOfType(int port, ServerType type) {
        if (servers.containsKey(port)) {
            return servers.get(port).equals(type);
        }
        return false;
    }

    public boolean occupyPort(int port, ServerType type) {
        if (!servers.containsKey(port)) {
            servers.put(port, type);
            return true;
        } else return isServerOfType(port, type);
    }

    public void abandonPort(int port) {
        servers.remove(port);
    }

    public ServerType getServerType(int port) {
        return servers.get(port);
    }
}
