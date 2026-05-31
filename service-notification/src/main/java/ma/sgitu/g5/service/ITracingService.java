package ma.sgitu.g5.service;

/**
 * ITracingService - Service de traçabilité pour la validation des tokens JWT
 * 
 * Rôle:
 * - Envoyer les informations de traçabilité à G10 (Auth Service) pour validation asynchrone
 * - Logger les informations de traçabilité pour audit
 * - Permettre la validation différée des tokens émis par les autres groupes
 */
public interface ITracingService {

    /**
     * Envoie les informations de traçabilité à G10 pour validation
     * 
     * @param traceId Identifiant unique de traçabilité
     * @param token Token JWT reçu
     * @param sourceGroup Groupe source (G1-G10) depuis le header X-Source-Group
     * @param tokenSourceService Service source depuis le token JWT
     * @param userId ID utilisateur depuis le token
     * @param roles Rôles depuis le token
     * @param tokenValid Si le token a été parsé avec succès
     */
    void sendTracingInfo(String traceId, String token, String sourceGroup, 
                        String tokenSourceService, String userId, 
                        java.util.List<String> roles, boolean tokenValid);
}
