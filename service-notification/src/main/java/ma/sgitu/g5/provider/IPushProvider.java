package ma.sgitu.g5.provider;

import ma.sgitu.g5.dto.response.SendResultDTO;

public interface IPushProvider {
    SendResultDTO send(String deviceToken, String message);
}
