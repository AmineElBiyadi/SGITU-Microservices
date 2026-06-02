package com.sgitu.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecipientsResponseDTO {
    private List<NotificationRecipientDTO> items;
    private int page;
    private int size;
    private long total;
}
