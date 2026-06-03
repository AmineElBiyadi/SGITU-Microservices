package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation réelle du QR code via la bibliothèque ZXing ("Zebra Crossing").
 *
 * <p><b>Différence avec QRCodeImpl (simulation) :</b><br>
 * {@link QRCodeImpl} encode simplement le payload en Base64 dans une String.
 * Cette classe génère un vrai QR code binaire (image PNG) via ZXing et le
 * retourne encodé en Base64 dans les métadonnées — prêt à être affiché ou scanné.</p>
 *
 * <p><b>Architecture du token :</b></p>
 * <ul>
 *   <li>{@code tokenValue} — contenu structuré encodé dans le QR code.
 *       Format : {@code QR-EXT|{holderId}|{eventId}}.
 *       C'est cette valeur qui permet la vérification déterministe.</li>
 *   <li>{@code metadata.qr_image_base64} — image PNG du QR code encodée en Base64.
 *       À décoder et afficher côté client (mobile, web, impression).</li>
 * </ul>
 *
 * <p><b>Pour une vraie intégration :</b><br>
 * Remplacer le contenu du QR par un JWT signé (HMAC-SHA256) ou un token
 * chiffré contenant holderId + eventId + timestamp pour empêcher la falsification.</p>
 *
 * @see QRCodeImpl  La version simulée (Base64 simple)
 * @see IdentityMethod
 */
public class QRCodeExternalImpl implements IdentityMethod {

    /** Dimensions en pixels du QR code généré. */
    private static final int QR_SIZE = 300;

    /** Format de l'image exportée. */
    private static final String IMAGE_FORMAT = "PNG";

    /** Niveau de correction d'erreur : Q = ~25% de redondance (bon équilibre). */
    private static final ErrorCorrectionLevel ERROR_CORRECTION = ErrorCorrectionLevel.Q;

    // ──────────────────────────────────────────────────────────────────────────
    // generateToken
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Génère un token d'identité avec un vrai QR code PNG via ZXing.
     *
     * <p>Le {@code tokenValue} est le contenu textuel structuré qui sera
     * encodé dans le QR code, et qui servira lors de la vérification.
     * L'image PNG du QR code est disponible dans {@code metadata.qr_image_base64}.</p>
     *
     * @param ctx contexte contenant holderId et eventId
     * @return token avec image QR en Base64 dans les métadonnées
     * @throws RuntimeException si ZXing échoue à générer le QR code
     */
    @Override
    public IdentityToken generateToken(IdentityContext ctx) {

        // 1. Construire le contenu structuré qui sera encodé dans le QR
        String qrContent = buildQrContent(ctx);

        // 2. Générer l'image QR via ZXing et l'encoder en Base64
        String qrImageBase64 = generateQrImageBase64(qrContent);

        // 3. Construire les métadonnées
        Map<String, String> metadata = new HashMap<>();
        metadata.put("library",          "ZXing 3.5.3");
        metadata.put("qr_size",          QR_SIZE + "x" + QR_SIZE + "px");
        metadata.put("error_correction",  ERROR_CORRECTION.name());
        metadata.put("image_format",      IMAGE_FORMAT);
        metadata.put("qr_image_base64",   qrImageBase64);

        return IdentityToken.builder()
                .tokenValue(qrContent)                         // Contenu vérifiable
                .methodType(IdentityMethodType.QR_CODE_EXTERNAL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .metadata(metadata)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // verifyToken
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie le token en reconstruisant le contenu QR attendu et en le comparant
     * au {@code tokenValue} stocké.
     *
     * <p><b>Checks effectués :</b></p>
     * <ol>
     *   <li>Token non null et non vide</li>
     *   <li>Type de méthode correct ({@code QR_CODE_EXTERNAL})</li>
     *   <li>Token non expiré</li>
     *   <li>Contenu QR correspond au contexte fourni</li>
     * </ol>
     *
     * @param token   le token stocké dans le ticket
     * @param ctx     le contexte de la requête de validation (holderId, eventId)
     * @return {@code true} si le token est valide et non expiré
     */
    @Override
    public boolean verifyToken(IdentityToken token, IdentityContext ctx) {

        // 1. Null / vide
        if (token == null || token.getTokenValue() == null || token.getTokenValue().isBlank()) {
            return false;
        }

        // 2. Bonne méthode
        if (!IdentityMethodType.QR_CODE_EXTERNAL.equals(token.getMethodType())) {
            return false;
        }

        // 3. Expiration
        if (token.getExpiresAt() != null && Instant.now().isAfter(token.getExpiresAt())) {
            return false;
        }

        // 4. Contenu QR correspond au contexte
        String expectedContent = buildQrContent(ctx);
        return token.getTokenValue().equals(expectedContent);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Construit le contenu textuel structuré qui sera encodé dans le QR code.
     *
     * <p>Format actuel : {@code QR-EXT|{holderId}|{eventId}}</p>
     *
     * <p><b>TODO Production :</b> Remplacer par un JWT signé (HMAC-SHA256)
     * contenant holderId + eventId + issuedAt pour empêcher la falsification.</p>
     *
     * @param ctx contexte d'identité
     * @return contenu structuré à encoder dans le QR
     */
    private String buildQrContent(IdentityContext ctx) {
        return "QR-EXT|" + ctx.getHolderId() + "|" + ctx.getEventId();
    }

    /**
     * Utilise ZXing pour encoder le contenu en QR code PNG et retourner
     * l'image en Base64.
     *
     * <p>La chaîne Base64 retournée peut être utilisée directement dans une balise HTML :</p>
     * <pre>{@code
     * <img src="data:image/png;base64,{qrImageBase64}" />
     * }</pre>
     *
     * @param content texte à encoder dans le QR code
     * @return image PNG du QR code en Base64
     * @throws RuntimeException si ZXing ne peut pas générer l'image
     */
    private String generateQrImageBase64(String content) {
        try {
            // Configuration ZXing
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION);
            hints.put(EncodeHintType.MARGIN, 2);           // Zone blanche autour du QR
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            // Génération de la matrice binaire du QR code
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            // Conversion BitMatrix → image PNG → Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, IMAGE_FORMAT, outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (WriterException e) {
            throw new RuntimeException(
                    "ZXing : échec de l'encodage QR pour le contenu : " + content, e);
        } catch (IOException e) {
            throw new RuntimeException(
                    "ZXing : échec de l'écriture de l'image QR en mémoire", e);
        }
    }
}
