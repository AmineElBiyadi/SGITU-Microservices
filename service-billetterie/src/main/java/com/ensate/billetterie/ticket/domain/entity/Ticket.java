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
import java.util.HashMap;
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

    @Field("parent_ticket_id")
    private String parentTicketId;

    @Field("transferred_from_user_id")
    private String transferredFromUserId;

    @Field("transfer_reason")
    private String transferReason;

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


    @Field("issued_at")
    private Instant issuedAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;


    public void transferPendingTo(String newHolderId, String reason) {
        this.transferHistory.add(
                new TransferRecord(holderId, newHolderId, Instant.now(), reason)
        );
        this.status   = TicketStatus.TRANSFER_PENDING;
    }
    public void transferTo(String newHolderId, String reason) {
        this.transferHistory.add(
                new TransferRecord(holderId, newHolderId, Instant.now(), reason)
        );
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


    public Map<String, Object> copyMetadata() {

        return this.getMetadata() != null
                ? new HashMap<>(this.getMetadata())
                : new HashMap<>();
    }


    public Ticket copyTo() {
        return Ticket.builder()
                .tripId(this.tripId)
                .price(this.price)
                .currency(this.currency)
                .ticketType(this.ticketType)
                .ticketClass(this.ticketClass)
                .holderId(this.holderId)
                .tokenValue(this.tokenValue)
                .identityMethod(this.identityMethod)
                .status(this.status)
                .transferHistory(new ArrayList<>(this.transferHistory))
                .metadata(this.metadata != null ? new HashMap<>(this.metadata) : null)
                .expiresAt(this.expiresAt)
                .flaggedAt(this.flaggedAt)
                .redeemedAt(this.redeemedAt)
                .refundedAt(this.refundedAt)
                .cancelledAt(this.cancelledAt)
                .deletedAt(this.deletedAt)
                .transferredAt(this.transferredAt)
                .build();
    }


    public Ticket copyAsNew() {
        return Ticket.builder()
                .tripId(this.tripId)
                .price(this.price)
                .currency(this.currency)
                .ticketType(this.ticketType)
                .ticketClass(this.ticketClass)
                .holderId(this.holderId)
                .identityMethod(this.identityMethod)
                .metadata(this.metadata != null ? new HashMap<>(this.metadata) : null)
                .expiresAt(this.expiresAt)
                // status resets to ISSUED (Builder.Default)
                // tokenValue excluded — caller must assign a fresh one
                // transferHistory excluded — starts fresh
                // all *_at timestamps excluded — set by lifecycle/auditing
                .build();
    }
}