package com.sgitu.servicegestionincidents.util;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Component
public class ReferenceGenerator {

    public String generate() {
        String year = String.valueOf(Year.now().getValue());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INC-" + year + "-" + uuid;
    }
}
