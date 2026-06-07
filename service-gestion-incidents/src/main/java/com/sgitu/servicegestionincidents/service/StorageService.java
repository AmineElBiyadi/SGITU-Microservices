package com.sgitu.servicegestionincidents.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

public interface StorageService {
    /**
     * Téléverse un fichier directement vers MinIO depuis le serveur (utilisé par /signaler).
     * Retourne la stockageKey générée.
     */
    String uploadFile(MultipartFile file, String objectKey);

    /**
     * Génère une URL pré-signée pour téléverser un fichier directement vers MinIO.
     */
    String generatePreSignedUploadUrl(String objectKey, String contentType, Duration expiration);

    /**
     * Génère une URL pré-signée pour consulter/télécharger un fichier privé.
     */
    String generatePreSignedDownloadUrl(String objectKey, Duration expiration);

    /**
     * Supprime un objet du stockage.
     */
    void deleteObject(String objectKey);
}
