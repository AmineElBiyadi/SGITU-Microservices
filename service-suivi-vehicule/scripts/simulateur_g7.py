import requests
import time
import random
import json
from datetime import datetime

# Configuration
BASE_URL = "http://localhost:8087/api/suivi-vehicules/simulateur"
VEHICLE_ID = "53c31262-591a-44d4-8872-51e84611ac5e"
HEADERS = {
    "X-User-Id": "admin",
    "X-User-Email": "admin@sgitu.ma",
    "X-Roles": "ROLE_ADMIN",
    "Content-Type": "application/json"
}

def simulate_movement():
    print(f"--- Démarrage de la simulation pour le véhicule {VEHICLE_ID} ---")
    lat, lon = 33.5731, -7.5898  # Casablanca
    
    try:
        while True:
            # 1. Simuler une position GPS
            vitesse = random.uniform(30, 140)  # Vitesse variable pour déclencher des alertes
            lat += random.uniform(-0.001, 0.001)
            lon += random.uniform(-0.001, 0.001)
            
            payload_gps = {
                "vehiculeId": VEHICLE_ID,
                "latitude": lat,
                "longitude": lon,
                "vitesse": vitesse,
                "cap": random.uniform(0, 360)
            }
            
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Envoi Position: {vitesse:.2f} km/h...", end=" ")
            try:
                res = requests.post(f"{BASE_URL}/sensor-data", headers=HEADERS, json=payload_gps, timeout=5)
                print(f"Status: {res.status_code}")
            except Exception as e:
                print(f"Erreur: {e}")

            # 2. Simuler aléatoirement un log d'alerte Admin (toutes les 5 itérations environ)
            if random.random() < 0.2:
                levels = ["WARN", "ERROR", "FATAL"]
                level = random.choice(levels)
                payload_log = {
                    "level": level,
                    "message": f"SIMULATION : Alerte système automatique de niveau {level}"
                }
                print(f"[{datetime.now().strftime('%H:%M:%S')}] !!! Envoi Log Admin ({level}) !!!", end=" ")
                try:
                    res = requests.post(f"{BASE_URL}/trigger-log-alert", headers=HEADERS, json=payload_log, timeout=5)
                    print(f"Status: {res.status_code}")
                except Exception as e:
                    print(f"Erreur: {e}")

            time.sleep(3)  # Attendre 3 secondes entre chaque envoi

    except KeyboardInterrupt:
        print("\nSimulation arrêtée par l'utilisateur.")

if __name__ == "__main__":
    # Vérifier si le service est disponible
    try:
        requests.get("http://localhost:8087/actuator/health", timeout=2)
        simulate_movement()
    except:
        print("ERREUR : Le microservice G7 n'est pas accessible sur http://localhost:8087")
        print("Assurez-vous de l'avoir lancé avec './mvnw.cmd spring-boot:run'")
