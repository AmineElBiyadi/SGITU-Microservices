package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.request.RecipientDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.provider.IEmailProvider;
import ma.sgitu.g5.provider.ILogProvider;
import ma.sgitu.g5.provider.IPushProvider;
import ma.sgitu.g5.provider.ISMSProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — ChannelRouterImpl")
class ChannelRouterImplTest {

    @Mock private IEmailProvider emailProvider;
    @Mock private ISMSProvider smsProvider;
    @Mock private IPushProvider pushProvider;
    @Mock private ILogProvider logProvider;

    @InjectMocks
    private ChannelRouterImpl channelRouter;

    private NotificationRequestDTO dto;

    @BeforeEach
    void setUp() {
        RecipientDTO recipient = new RecipientDTO();
        recipient.setUserId("user-1");
        recipient.setEmail("a@sgitu.ma");
        recipient.setPhone("+212600000000");
        recipient.setDeviceToken("fcm-token");

        dto = new NotificationRequestDTO();
        dto.setNotificationId("n-1");
        dto.setSourceService("G4_COORDINATION");
        dto.setRecipient(recipient);
    }

    @Test
    @DisplayName("route EMAIL → emailProvider.send")
    void route_email() {
        SendResultDTO ok = success("SMTP");
        when(emailProvider.send(eq("a@sgitu.ma"), eq("Sujet"), eq("Message"))).thenReturn(ok);

        dto.setChannel("EMAIL");
        SendResultDTO result = channelRouter.route(dto, "Sujet", "Message");

        assertThat(result.isSuccess()).isTrue();
        verify(emailProvider).send("a@sgitu.ma", "Sujet", "Message");
    }

    @Test
    @DisplayName("route SMS → smsProvider.send")
    void route_sms() {
        SendResultDTO ok = success("TWILIO");
        when(smsProvider.send("+212600000000", "SMS body")).thenReturn(ok);

        dto.setChannel("SMS");
        SendResultDTO result = channelRouter.route(dto, "ignored", "SMS body");

        assertThat(result.getProvider()).isEqualTo("TWILIO");
        verify(smsProvider).send("+212600000000", "SMS body");
    }

    @Test
    @DisplayName("route PUSH → pushProvider.send")
    void route_push() {
        SendResultDTO ok = success("FCM");
        when(pushProvider.send("fcm-token", "Push body")).thenReturn(ok);

        dto.setChannel("PUSH");
        channelRouter.route(dto, "ignored", "Push body");

        verify(pushProvider).send("fcm-token", "Push body");
    }

    @Test
    @DisplayName("route LOG → logProvider.log (canal admin G7)")
    void route_log() {
        SendResultDTO ok = success("LOG_FILE");
        when(logProvider.log(anyString(), anyString(), anyString(), anyString())).thenReturn(ok);

        dto.setChannel("LOG");
        dto.setSourceService("G7_SUIVI_VEHICULES");
        channelRouter.route(dto, "ALERTE", "Message admin");

        verify(logProvider).log("ALERTE", "Message admin", "G7_SUIVI_VEHICULES", "user-1");
    }

    @Test
    @DisplayName("canal inconnu → IllegalArgumentException")
    void route_unknownChannel() {
        dto.setChannel("FAX");
        assertThatThrownBy(() -> channelRouter.route(dto, "s", "m"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Canal non supporté");
    }

    private static SendResultDTO success(String provider) {
        SendResultDTO dto = new SendResultDTO();
        dto.setSuccess(true);
        dto.setProvider(provider);
        return dto;
    }
}
