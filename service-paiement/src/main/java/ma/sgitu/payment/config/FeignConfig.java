package ma.sgitu.payment.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration OpenFeign pour G6 Paiement
 *
 * Fonctionnalités :
 * 1. Ajoute automatiquement le JWT aux appels sortants vers G5
 * 2. Gère les erreurs Feign (retry, logging)
 */
@Configuration
public class FeignConfig {

    private static final Logger logger = LoggerFactory.getLogger(FeignConfig.class);

    /**
     * Intercepteur de requêtes Feign
     * Ajoute automatiquement le header Authorization avec JWT
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Récupère l'authentification du contexte Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Si un JWT est présent, on l'ajoute au header Authorization
            if (authentication != null && authentication.getCredentials() != null) {
                String token = authentication.getCredentials().toString();
                requestTemplate.header("Authorization", "Bearer " + token);
                logger.debug("JWT ajouté au header Feign pour appel G5");
            } else {
                logger.warn("Aucun JWT trouvé dans le contexte - Appel G5 sans authentification");
            }
        };
    }

    /**
     * Gestion personnalisée des erreurs Feign
     * Log les erreurs d'appel vers G5
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            logger.error("Erreur Feign - Méthode: {} - Status: {} - Raison: {}",
                    methodKey, response.status(), response.reason());
            return new ErrorDecoder.Default().decode(methodKey, response);
        };
    }
}