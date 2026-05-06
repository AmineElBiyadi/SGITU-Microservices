package com.ensate.billetterie.ticket.domain.entity;


import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.ticket.domain.enums.TicketClass;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import com.ensate.billetterie.ticket.domain.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String id;

    @Indexed
    @Field("tripId")
    private String tripId;

    @Indexed
    @Field
    private double price;

    @Indexed
    @Field
    private String currency;

    @Indexed
    @Field("ticket_type")
    private TicketType ticketType;

    @Indexed
    @Field("ticket_class")
    private TicketClass ticketClass;

    @Indexed
    @Field("holder_id")
    private String holderId;

    @Indexed(unique = true)
    @Field("token_value")
    private String tokenValue;

    @Field("identity_method")
    private IdentityMethodType identityMethod;

    @Field("status")
    @Builder.Default
    private TicketStatus status = TicketStatus.ISSUED;

    @Field("transfer_history")
    @Builder.Default
    private List<TransferRecord> transferHistory = new ArrayList<>();

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("expires_at")
    private Instant expiresAt;

    @Field("flagged_at")
    private Instant flaggedAt;

    @Field("redeemed_at")
    private Instant redeemedAt;

    @Field("refunded_at")
    private Instant refundedAt;

    @Field("cancelled_at")
    private Instant cancelledAt;

    @Field("deleted_at")
    private Instant deletedAt;


    @Field("transferred_at")
    private Instant transferredAt;

    @CreatedDate
    @Field("issued_at")
    private Instant issuedAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;



    public void transferTo(String newHolderId, String reason) {
        this.transferHistory.add(
                new TransferRecord(id, newHolderId, Instant.now(), reason)
        );
        this.holderId = newHolderId;
        this.status   = TicketStatus.TRANSFERRED;
    }

    public void markRedeemed()    { this.status = TicketStatus.REDEEMED; }
    public void cancel()      { this.status = TicketStatus.CANCELLED; }
    public void markExpired() { this.status = TicketStatus.EXPIRED; }

    public boolean isExpired() { return Instant.now().isAfter(this.expiresAt); }
    public boolean isActive()  {
        return (status == TicketStatus.ISSUED || status == TicketStatus.TRANSFERRED)
                && !isExpired();
    }
}