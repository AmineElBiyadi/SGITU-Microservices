# Captures Postman pour le rapport G10

Enregistrer ici les captures Postman utilisees par `docs/G10_Rapport_Avancement.tex`.

Noms attendus :

- `postman_01_g10_health.png`
- `postman_02_login_g3_jwt.png`
- `postman_03_jwt_invalide_401.png`
- `postman_04_rbac_passenger_403.png`
- `postman_05_ingestion_g8_ticket.png`
- `postman_06_analytics_operator_200.png`
- `postman_07_ml_peak_hours_200.png`
- `postman_08_update_roles_admin.png`
- `logs_01_g10_gateway_request_response.png`
- `logs_02_g3_user_service.png`
- `logs_03_g8_analytics_ingestion.png`

Chaque capture doit montrer :

- l'URL appelee via `http://localhost:8080` ;
- le header `Authorization: Bearer <JWT>` si la route est protegee ;
- le header `X-Correlation-Id` si le test l'utilise ;
- le code HTTP ;
- le body JSON de reponse.

Pour les captures de logs, montrer si possible :

- le nom du conteneur ;
- la route appelee ;
- le code HTTP ou le resultat de traitement ;
- le meme `X-Correlation-Id` que dans Postman.

Scenario conseille pour les captures de logs :

1. Executer `01.4 - 200 login G3 via G10` pour produire les logs G10 + G3.
2. Executer `03.2 - 201/207 ingestion tickets` pour produire les logs G10 + G8.
3. Garder les trois terminaux ouverts pendant les deux requetes :
   - `docker compose logs -f api-gateway`
   - `docker compose logs -f user-service`
   - `docker compose logs -f g8-analytics`
