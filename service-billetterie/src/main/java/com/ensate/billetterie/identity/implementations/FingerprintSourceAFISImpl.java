package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;
import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintImageOptions;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation réelle de la vérification d'empreintes digitales en utilisant SourceAFIS.
 * 
 * <p><b>Fonctionnement :</b></p>
 * <ul>
 *   <li>Lors de l'émission, on extrait l'image de l'empreinte depuis le payload,
 *       on génère un {@link FingerprintTemplate} (minuties) et on stocke sa forme sérialisée.</li>
 *   <li>Lors de la vérification, on génère le template de la nouvelle image présentée
 *       et on utilise un {@link FingerprintMatcher} pour comparer avec le template stocké.</li>
 * </ul>
 */
public class FingerprintSourceAFISImpl implements IdentityMethod {

    // Seuil de score pour que l'empreinte soit considérée comme correspondante
    private static final double MATCHING_THRESHOLD = 40.0;

    @Override
    public IdentityToken generateToken(IdentityContext identityContext) {
        // 1. Récupérer l'image encodée en Base64 depuis le contexte (envoyée par le scanner/mobile)
        String base64Image = extractBase64Image(identityContext.getRawPayload(), "fingerprint_image_base64");
        
        if (base64Image == null) {
            throw new IllegalArgumentException("Payload doit contenir 'fingerprint_image_base64'");
        }

        // 2. Décoder l'image et créer un FingerprintTemplate
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        FingerprintImage image = new FingerprintImage(imageBytes, new FingerprintImageOptions());
        FingerprintTemplate template = new FingerprintTemplate(image);
        
        // 3. Sérialiser le template pour le stocker (on ne stocke jamais l'image brute)
        byte[] serializedTemplate = template.toByteArray();
        String tokenValue = Base64.getEncoder().encodeToString(serializedTemplate);

        // 4. Préparer les métadonnées
        Map<String, String> metadata = new HashMap<>();
        metadata.put("algorithm", "SourceAFIS 3.18.1");
        metadata.put("template_size", String.valueOf(serializedTemplate.length));

        return IdentityToken.builder()
                .tokenValue(tokenValue)
                .methodType(IdentityMethodType.FINGERPRINT_EXTERNAL)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .metadata(metadata)
                .build();
    }

    @Override
    public boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext) {
        if (identityToken == null || identityToken.getTokenValue() == null) {
            return false;
        }
        
        if (!IdentityMethodType.FINGERPRINT_EXTERNAL.equals(identityToken.getMethodType())) {
            return false;
        }

        try {
            // 1. Reconstruire le template de référence depuis le token stocké
            byte[] serializedTemplate = Base64.getDecoder().decode(identityToken.getTokenValue());
            FingerprintTemplate referenceTemplate = new FingerprintTemplate(serializedTemplate);
            
            // 2. Récupérer la NOUVELLE image présentée lors du contrôle d'accès
            String newBase64Image = extractBase64Image(identityContext.getRawPayload(), "fingerprint_image_base64");
            if (newBase64Image == null) return false;
            
            byte[] newImageBytes = Base64.getDecoder().decode(newBase64Image);
            FingerprintImage newImage = new FingerprintImage(newImageBytes, new FingerprintImageOptions());
            FingerprintTemplate candidateTemplate = new FingerprintTemplate(newImage);

            // 3. Effectuer le matching
            FingerprintMatcher matcher = new FingerprintMatcher(referenceTemplate);
            double score = matcher.match(candidateTemplate);
            
            // 4. Vérifier si le score dépasse le seuil (Matching threshold)
            return score >= MATCHING_THRESHOLD;
            
        } catch (Exception e) {
            // En cas d'erreur de décodage ou de format d'image non supporté
            e.printStackTrace();
            return false;
        }
    }
    
    private String extractBase64Image(Map<String, Object> payload, String key) {
        if (payload != null && payload.containsKey(key)) {
            Object val = payload.get(key);
            if (val instanceof String) {
                return (String) val;
            }
        }
        return null;
    }
}
