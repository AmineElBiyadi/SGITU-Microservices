package ma.sgitu.g5.provider;

import ma.sgitu.g5.dto.response.SendResultDTO;

public interface IEmailProvider {
    SendResultDTO send(String to, String subject, String body);
}
