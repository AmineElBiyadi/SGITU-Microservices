package com.ensate.billetterie.ticket.repository;

import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket,String> {

    Optional<Ticket> findByTokenValue(String tokenValue);
    List<Ticket> findByHolderId(String holderId);
//    List<Ticket>     findByEventId(String eventId);
//    List<Ticket>     findByEventIdAndStatus(String eventId, TicketStatus status);


    List<Ticket>     findByStatusInAndExpiresAtBefore(List<TicketStatus> statuses, Instant cutoff);
}
