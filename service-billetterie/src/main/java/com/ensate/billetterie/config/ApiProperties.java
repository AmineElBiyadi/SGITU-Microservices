package com.ensate.billetterie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    private Info info;
    private Contact contact;
    private License license;

    @Data
    public static class Info {
        private String title;
        private String description;
        private String version;
        private String termsOfService;
    }

    @Data
    public static class Contact {
        private String name;
        private String email;
        private String url;
    }

    @Data
    public static class License {
        private String name;
        private String url;
    }
}
