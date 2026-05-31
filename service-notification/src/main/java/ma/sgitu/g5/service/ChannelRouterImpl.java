package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.provider.IEmailProvider;
import ma.sgitu.g5.provider.ILogProvider;
import ma.sgitu.g5.provider.IPushProvider;
import ma.sgitu.g5.provider.ISMSProvider;
import org.springframework.stereotype.Service;

/**
 * ChannelRouterImpl — Route chaque notification vers le bon provider selon le canal.
 *
 * Canaux supportés :
 *   EMAIL → SendGridEmailAdapter (ou JavaMailSender en dev/MailHog)
 *   SMS   → TwilioSMSAdapter
 *   PUSH  → FCMPushAdapter (Firebase Cloud Messaging)
 *   LOG   → LogFileAdapter (écriture fichier log structuré — niveau admin)
 *
 * Le canal LOG est utilisé pour les notifications admin qui ne doivent pas
 * être envoyées à un utilisateur final mais tracées dans un fichier log
 * (ex: alertes sécurité, événements critiques G7, audits inter-groupes).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelRouterImpl implements IChannelRouter {

    private final IEmailProvider emailProvider;
    private final ISMSProvider   smsProvider;
    private final IPushProvider  pushProvider;
    private final ILogProvider   logProvider;

    @Override
    public SendResultDTO route(NotificationRequestDTO dto, String subject, String message) {
        String channel = dto.getChannel();
        log.info("[G5-ROUTER] Canal={} | notificationId={} | source={}",
                channel, dto.getNotificationId(), dto.getSourceService());

        return switch (channel) {
            case "EMAIL" -> emailProvider.send(
                    dto.getRecipient().getEmail(), subject, message);

            case "SMS"   -> smsProvider.send(
                    dto.getRecipient().getPhone(), message);

            case "PUSH"  -> pushProvider.send(
                    dto.getRecipient().getDeviceToken(), message);

            // Canal LOG : notification admin → fichier log structuré
            case "LOG"   -> logProvider.log(
                    subject,
                    message,
                    dto.getSourceService(),
                    dto.getRecipient() != null ? dto.getRecipient().getUserId() : "system");

            default -> throw new IllegalArgumentException(
                    "[G5-ROUTER] Canal non supporté : " + channel
                    + ". Valeurs acceptées : EMAIL, SMS, PUSH, LOG");
        };
    }
}
