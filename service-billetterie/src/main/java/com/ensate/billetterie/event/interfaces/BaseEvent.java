package com.ensate.billetterie.event.interfaces;

import java.time.Instant;

public interface BaseEvent {
    String getEventType();
    Instant getTimestamp();
}
