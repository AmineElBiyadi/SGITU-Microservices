package com.sgitu.servicegestionincidents.model.enums;

public enum NiveauGravite {
    FAIBLE(48, 1),
    MOYEN(24, 2),
    ELEVE(8, 3),
    CRITIQUE(2, 4);

    private final int delaiMaxTraitement; // en heures
    private final int niveauPriorite;

    NiveauGravite(int delaiMaxTraitement, int niveauPriorite) {
        this.delaiMaxTraitement = delaiMaxTraitement;
        this.niveauPriorite = niveauPriorite;
    }

    public int getDelaiMaxTraitement() {
        return delaiMaxTraitement;
    }

    public int getNiveauPriorite() {
        return niveauPriorite;
    }
}
