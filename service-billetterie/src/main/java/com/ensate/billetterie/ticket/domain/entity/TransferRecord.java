package com.ensate.billetterie.ticket.domain.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRecord {

    @Field("from_holder")
    private String fromHolder;

    @Field("to_holder")
    private String toHolder;

    @Field("transferred_at")
    private Instant transferredAt;

    @Field("reason")
    private String reason;
}
