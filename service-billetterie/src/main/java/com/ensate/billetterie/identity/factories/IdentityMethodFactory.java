package com.ensate.billetterie.identity.factories;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.implementations.FaceIDImpl;
import com.ensate.billetterie.identity.implementations.FingerprintImpl;
import com.ensate.billetterie.identity.implementations.FingerprintSourceAFISImpl;
import com.ensate.billetterie.identity.implementations.QRCodeImpl;
import com.ensate.billetterie.identity.implementations.QRCodeExternalImpl;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

import java.util.Map;

/**
 * Factory de sélection dynamique des stratégies d'identification.
 *
 * <p>Utilise {@link Map#ofEntries} (plutôt que {@link Map#of}) pour rester
 * extensible au-delà de 10 entrées sans modifier la signature.</p>
 *
 * <p><b>Pour ajouter une nouvelle implémentation :</b> créer la classe qui
 * implémente {@link com.ensate.billetterie.identity.interfaces.IdentityMethod},
 * ajouter la valeur dans {@link IdentityMethodType}, puis ajouter une entrée
 * ici. Aucun autre fichier n'a besoin d'être modifié.</p>
 */
public class IdentityMethodFactory {

    private static final Map<IdentityMethodType, IdentityMethod> REGISTRY = Map.ofEntries(
            // ── Simulation architecturale (sans dépendance externe) ──────────
            Map.entry(IdentityMethodType.QR_CODE,          new QRCodeImpl()),
            Map.entry(IdentityMethodType.FINGERPRINT,      new FingerprintImpl()),
            Map.entry(IdentityMethodType.FACE_ID,          new FaceIDImpl()),

            // ── Implémentations réelles (bibliothèques / SDKs externes) ──────
            Map.entry(IdentityMethodType.QR_CODE_EXTERNAL,     new QRCodeExternalImpl()),
            Map.entry(IdentityMethodType.FINGERPRINT_EXTERNAL, new FingerprintSourceAFISImpl())
    );

    /**
     * Retourne la stratégie d'identification correspondant au type demandé.
     *
     * @param type type de méthode d'identification
     * @return l'implémentation {@link IdentityMethod} associée
     * @throws IllegalArgumentException si aucune implémentation n'est enregistrée pour ce type
     */
    public static IdentityMethod create(IdentityMethodType type) {
        IdentityMethod method = REGISTRY.get(type);
        if (method == null) {
            throw new IllegalArgumentException(
                    "Aucune implémentation enregistrée pour le type : " + type +
                    ". Types disponibles : " + REGISTRY.keySet());
        }
        return method;
    }

    private IdentityMethodFactory() {}
}

