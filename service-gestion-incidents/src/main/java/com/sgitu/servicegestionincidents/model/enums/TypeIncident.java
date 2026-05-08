package com.sgitu.servicegestionincidents.model.enums;

public enum TypeIncident {
    PANNE_VEHICULE(NiveauGravite.MOYEN),
    ACCIDENT(NiveauGravite.ELEVE),
    RETARD(NiveauGravite.FAIBLE),
    ENCOMBREMENT(NiveauGravite.MOYEN),
    SECURITE(NiveauGravite.ELEVE),
    INFRASTRUCTURE(NiveauGravite.MOYEN),
    AUTRE(NiveauGravite.FAIBLE);

    private final NiveauGravite graviteParDefaut;

    TypeIncident(NiveauGravite graviteParDefaut) {
        this.graviteParDefaut = graviteParDefaut;
    }

    public NiveauGravite getGraviteParDefaut() {
        return graviteParDefaut;
    }
}