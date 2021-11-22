package com.michaelpippolito.utils.sftp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpConfig {
    @Getter @Setter
    private boolean autoStartup;

    @Getter @Setter
    private String localDir;

    @Getter @Setter
    private Map<Integer, List<String>> defaultServers;
}
