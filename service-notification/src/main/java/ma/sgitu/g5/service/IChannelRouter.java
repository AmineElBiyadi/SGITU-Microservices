package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;

public interface IChannelRouter {
    SendResultDTO route(NotificationRequestDTO dto, String subject, String message);
}
