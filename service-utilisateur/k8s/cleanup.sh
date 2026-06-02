#!/bin/bash

# Cleanup User Service from Kubernetes
# Compatible with Git Bash on Windows

echo "=========================================="
echo "🗑️  Nettoyage du Service Utilisateur"
echo "=========================================="
echo ""

read -p "⚠️  Voulez-vous vraiment supprimer tous les déploiements ? (y/N) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  Suppression du namespace sgitu..."
    kubectl delete namespace sgitu
    
    echo ""
    echo "✅ Nettoyage terminé !"
    echo ""
    echo "Pour redéployer :"
    echo "  bash deploy.sh"
    echo "  ou"
    echo "  powershell.exe -File deploy.ps1"
else
    echo "❌ Opération annulée"
fi
echo ""
