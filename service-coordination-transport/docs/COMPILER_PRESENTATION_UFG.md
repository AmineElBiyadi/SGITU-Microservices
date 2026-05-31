# Compiler la présentation (template Ufg)

## Fichier

`PRESENTATION_G4.tex` — thème **Ufg**, couleur principale **SGITUTeal** (vert-bleu, pas UFGBlue).

## Overleaf (recommandé)

1. Créer un projet Beamer sur Overleaf avec le thème **Ufg** (même template que votre cours).
2. Copier le contenu de `PRESENTATION_G4.tex`.
3. Si `\setBGColor{SGITUTeal}` échoue, remplacer par `\setBGColor{UFGGreen}` sur la slide Merci.
4. Compiler → télécharger **PRESENTATION_G4.pdf**.

## Changer encore la couleur

Dans le préambule, modifier :

```latex
\definecolor{SGITUTeal}{RGB}{0,95,115}   % actuel : teal
\setPrimaryColor{SGITUTeal}
```

Exemples alternatifs :

| Nom | RGB | Style |
|-----|-----|--------|
| Violet | `{75,0,130}` | sobre |
| Orange | `{200,80,20}` | énergique |
| Vert | `{0,120,60}` | classique |

Et slide finale : `\setBGColor{SGITUTeal}` (même nom).

## Contenu

14 sections de contenu + plan + page titre + Merci = **~15 slides** (exigence prof).
