package ma.sgitu.g5.provider;

import ma.sgitu.g5.dto.response.SendResultDTO;

public interface ISMSProvider {
    SendResultDTO send(String phone, String message);
}
