# Compilation LaTeX — G4 (versions optimisées)

## Fichiers à utiliser

| Document | Fichier LaTeX |
|----------|----------------|
| **Rapport final** | `RAPPORT_FINAL_G4_SGITU.tex` |
| **Soutenance (~15 min)** | `PRESENTATION_G4_SGITU.tex` |

Anciens fichiers (`RAPPORT_G4_COORDINATION.tex`, `PRESENTATION_G4.tex`) : conservés pour historique.

## Overleaf / local

1. Créer un projet avec **tout le dossier** `service-coordination-transport/` (thème `beamerthemeuae.sty` + dossier `figures/`).
2. Compiler :
   - Rapport : `pdflatex RAPPORT_FINAL_G4_SGITU.tex` (×2 si sommaire)
   - Slides : `pdflatex PRESENTATION_G4_SGITU.tex` (×2)

## Figures manquantes

Les commandes `\figimg` / `\figslide` affichent un cadre bleu si le PNG n'existe pas. Générer les UML :

```powershell
powershell -ExecutionPolicy Bypass -File scripts/generate-figures-mermaid.ps1
```

Captures Swagger/Docker/Postman : déposer dans `figures/` (voir `figures/README_FIGURES.md`).

## Présentation — timing oral

| Section | Slides | ~min |
|---------|--------|------|
| Cadrage | 2 | 2 |
| Sécurité G3→G10→G4 | 2 | 3 |
| Conception | 2 | 3 |
| Architecture + Docker | 2 | 3 |
| Démo + Chaos + Piliers | 3 | 3 |
| Conclusion | 2 | 1 |

**Total : ~15 min** (+ questions)
