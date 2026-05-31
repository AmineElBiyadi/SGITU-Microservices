package ma.sgitu.g5.service;

import ma.sgitu.g5.dto.request.MetadataDTO;

public interface ITemplateService {

    /**
     * Hydrate le template correspondant à l'eventType avec les données metadata
     * 
     * @param eventType le type d'événement (ex: PAYMENT_SUCCESS)
     * @param metadata  les données brutes du groupe
     * @return le message final formaté
     */
    String hydrateMessage(String eventType, MetadataDTO metadata);

    /**
     * Retourne le sujet (subject) pour les emails
     */
    String hydrateSubject(String eventType, MetadataDTO metadata);
}