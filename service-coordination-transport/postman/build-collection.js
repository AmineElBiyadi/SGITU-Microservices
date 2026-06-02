const fs = require('fs');

/** UUID exemple aligné G7 — remplacer par un id réel après POST /api/suivi-vehicules/vehicules (G7). */
const EXAMPLE_VEHICLE_UUID = '00000000-0000-4000-8000-000000000001';

const saveId = (v) => ({
	listen: 'test',
	script: {
		type: 'text/javascript',
		exec: [
			'if ([200, 201].includes(pm.response.code)) {',
			'  const j = pm.response.json();',
			`  if (j && j.id != null) pm.collectionVariables.set('${v}', String(j.id));`,
			'}',
		],
	},
});

const loginEvent = {
	listen: 'test',
	script: {
		type: 'text/javascript',
		exec: [
			'if (pm.response.code === 200) {',
			"  pm.collectionVariables.set('accessToken', pm.response.json().token);",
			'}',
		],
	},
};

const expectStatus = (...codes) => ({
	listen: 'test',
	script: {
		type: 'text/javascript',
		exec: [
			`pm.test('HTTP ${codes.join('|')}', function () {`,
			`  pm.expect(pm.response.code).to.be.oneOf([${codes.join(',')}]);`,
			'});',
		],
	},
});

const notificationBody = () =>
	'{\n' +
	'  "notificationId": "G4-POSTMAN-001",\n' +
	'  "sourceService": "COORDINATION",\n' +
	'  "eventType": "DELAY_ALERT",\n' +
	'  "channel": "EMAIL",\n' +
	'  "recipient": { "userId": "{{recipientUserId}}", "email": "{{recipientEmail}}" },\n' +
	'  "metadata": { "lineId": "L12", "reason": "RETARD_SIGNIFICATIF", "variables": { "vehiculeId": "{{vehiculeId}}", "valeur": "12", "arret": "Gare Sud" } }\n' +
	'}';

const jsonHdr = () => [{ key: 'Content-Type', value: 'application/json' }];

const req = (name, method, url, opts = {}) => {
	const r = { name, request: { method, header: jsonHdr(), url } };
	if (opts.noauth) r.request.auth = { type: 'noauth' };
	if (opts.body) r.request.body = { mode: 'raw', raw: opts.body };
	if (opts.desc) r.request.description = opts.desc;
	if (opts.events) r.event = opts.events;
	return r;
};

const folder = (name, desc, items) => ({
	name,
	...(desc ? { description: desc } : {}),
	item: items,
});

const collection = {
	info: {
		_postman_id: 'g4-coord-2026-ordered',
		name: 'SGITU G4 — Tests ordonnés',
		description:
			'ORDRE IMPORTANT\n\n' +
			'1) Démarrer G4 : cd service-coordination-transport puis docker compose up -d\n' +
			'2) Attendre health UP : GET 00 — Démarrage\n' +
			'3) Parcours rapide : dossier GUIDE (requêtes 1→13 dans l\'ordre)\n' +
			'4) Login : gestionnaire.reseau = réseau | gestionnaire.flotte = missions | admin.technique = admin\n\n' +
			'Flow G7 : créer véhicule chez G7 → variable vehiculeId (UUID) → sync ou Kafka → affectation ACTIF → mission.\n\n' +
			'Les POST « créer » enregistrent automatiquement ligneId, arretId, missionId…',
		schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
	},
	variable: [
		{ key: 'baseUrl', value: 'http://localhost:8084' },
		{ key: 'accessToken', value: '' },
		{ key: 'ligneId', value: '1' },
		{ key: 'arretId', value: '1' },
		{ key: 'arretId2', value: '2' },
		{ key: 'trajetId', value: '1' },
		{ key: 'horaireId', value: '1' },
		{ key: 'affectationId', value: '1' },
		{ key: 'missionId', value: '1' },
		{ key: 'eventId', value: '1' },
		{ key: 'impactId', value: '1' },
		{ key: 'vehiculeId', value: EXAMPLE_VEHICLE_UUID },
		{ key: 'recipientUserId', value: '1' },
		{ key: 'recipientEmail', value: 'test@sgitu.local' },
		{ key: 'g7BaseUrl', value: 'http://localhost:8087' },
		{ key: 'g3BaseUrl', value: 'http://localhost:8083' },
		{ key: 'g3AccessToken', value: '' },
	],
	auth: {
		type: 'bearer',
		bearer: [{ key: 'token', value: '{{accessToken}}', type: 'string' }],
	},
	item: [],
};

collection.item.push(
	folder('00 — Démarrage', 'Sans JWT — faire en premier', [
		req('GET health (public)', 'GET', '{{baseUrl}}/api/g4/health', {
			noauth: true,
			desc: 'Réponse 200 avec status UP',
		}),
	])
);

collection.item.push(
	folder('01 — Connexion', 'Choisir UN login selon le test', [
		{
			...req('Login gestionnaire.reseau (G4_OPERATOR)', 'POST', '{{baseUrl}}/api/auth/login', {
				noauth: true,
				body: '{\n  "username": "gestionnaire.reseau",\n  "password": "password"\n}',
				desc: 'Réseau : lignes, arrêts, trajets, horaires',
			}),
			event: [loginEvent],
		},
		{
			...req('Login gestionnaire.flotte (DISPATCHER)', 'POST', '{{baseUrl}}/api/auth/login', {
				noauth: true,
				body: '{\n  "username": "gestionnaire.flotte",\n  "password": "password"\n}',
				desc: 'Flotte : missions, events, incident-impacts, notifications',
			}),
			event: [loginEvent],
		},
		{
			...req('Login admin.technique (G4_ADMIN)', 'POST', '{{baseUrl}}/api/auth/login', {
				noauth: true,
				body: '{\n  "username": "admin.technique",\n  "password": "password"\n}',
				desc: 'Admin : pending-notifications, operator/status',
			}),
			event: [loginEvent],
		},
	])
);

const guide = [
	{
		...req('1. Login réseau (G4_OPERATOR)', 'POST', '{{baseUrl}}/api/auth/login', {
			noauth: true,
			body: '{\n  "username": "gestionnaire.reseau",\n  "password": "password"\n}',
			desc: 'Étapes 2→6 : lignes, arrêts, trajets, horaires',
		}),
		event: [loginEvent, expectStatus(200)],
	},
	{
		...req('2. POST ligne', 'POST', '{{baseUrl}}/api/g4/lignes', {
			body: '{\n  "code": "L12-{{$timestamp}}",\n  "nom": "Ligne test Postman",\n  "description": "Parcours guidé",\n  "active": true\n}',
		}),
		event: [saveId('ligneId'), expectStatus(201)],
	},
	{
		...req('3. POST arrêt A', 'POST', '{{baseUrl}}/api/g4/arrets', {
			body: '{\n  "code": "AR-A",\n  "nom": "Arrêt A",\n  "latitude": 35.57,\n  "longitude": -5.37,\n  "ligneId": {{ligneId}}\n}',
		}),
		event: [saveId('arretId'), expectStatus(201)],
	},
	{
		...req('4. POST arrêt B', 'POST', '{{baseUrl}}/api/g4/arrets', {
			body: '{\n  "code": "AR-B",\n  "nom": "Arrêt B",\n  "latitude": 35.58,\n  "longitude": -5.36,\n  "ligneId": {{ligneId}}\n}',
		}),
		event: [saveId('arretId2'), expectStatus(201)],
	},
	{
		...req('5. POST trajet', 'POST', '{{baseUrl}}/api/g4/trajets', {
			body:
				'{\n  "ligneId": {{ligneId}},\n  "code": "T12-A",\n  "nom": "Sens aller",\n  "sens": "ALLER",\n  "actif": true,\n  "arretSequence": [\n    { "arretId": {{arretId}}, "sequenceOrder": 1 },\n    { "arretId": {{arretId2}}, "sequenceOrder": 2 }\n  ]\n}',
		}),
		event: [saveId('trajetId'), expectStatus(201)],
	},
	{
		...req('6. POST horaire', 'POST', '{{baseUrl}}/api/g4/horaires', {
			body:
				'{\n  "trajetId": {{trajetId}},\n  "arretId": {{arretId}},\n  "heurePassage": "07:15:00",\n  "jourSemaine": 1,\n  "validFrom": "2026-01-01",\n  "validTo": "2026-12-31",\n  "libelle": "Passage matin"\n}',
		}),
		event: [saveId('horaireId'), expectStatus(201)],
	},
	{
		...req('6b. Login flotte (DISPATCHER)', 'POST', '{{baseUrl}}/api/auth/login', {
			noauth: true,
			body: '{\n  "username": "gestionnaire.flotte",\n  "password": "password"\n}',
			desc: 'Étapes 7→11 : affectation, mission, events, G9, notification',
		}),
		event: [loginEvent, expectStatus(200)],
	},
	{
		...req('6c. POST sync véhicule G7 → G4 (optionnel)', 'POST', '{{baseUrl}}/api/g4/vehicules/sync-from-g7/{{vehiculeId}}', {
			desc: 'Si G7 UP : sync REST. Sinon exécuter run-postman-ordered.ps1 (seed référentiel).',
		}),
		event: [expectStatus(200, 400, 502)],
	},
	(() => {
		const r = req('6d. GET véhicules disponibles G4', 'GET', '{{baseUrl}}/api/g4/vehicules/disponibles');
		r.event = [expectStatus(200)];
		return r;
	})(),
	{
		...req('7. POST affectation', 'POST', '{{baseUrl}}/api/g4/affectations', {
			body:
				'{\n  "vehiculeId": "{{vehiculeId}}",\n  "ligneId": {{ligneId}},\n  "dateDebut": "2026-05-07T06:00:00Z",\n  "statut": "ACTIF",\n  "commentaire": "Test"\n}',
		}),
		event: [saveId('affectationId')],
	},
	{
		...req('8. POST mission', 'POST', '{{baseUrl}}/api/g4/missions', {
			body:
				'{\n  "vehiculeId": "{{vehiculeId}}",\n  "ligneId": {{ligneId}},\n  "trajetId": {{trajetId}},\n  "affectationId": {{affectationId}},\n  "statut": "PLANIFIEE",\n  "plannedStart": "2026-05-07T08:00:00Z",\n  "notes": "Mission test"\n}',
		}),
		event: [saveId('missionId')],
	},
	{
		...req('9. POST detect-delay', 'POST', '{{baseUrl}}/api/g4/events/detect-delay', {
			body: '{\n  "missionId": {{missionId}},\n  "retardMinutes": 10,\n  "cause": "Trafic"\n}',
		}),
		event: [saveId('eventId')],
	},
	{
		...req('10. POST incident-impact (G9)', 'POST', '{{baseUrl}}/api/g4/incident-impacts', {
			body:
				'{\n  "incidentReference": "INC-TEST-001",\n  "missionId": {{missionId}},\n  "vehiculeId": "{{vehiculeId}}",\n  "resume": "Test impact G9"\n}',
		}),
		event: [saveId('impactId')],
	},
	req('11. POST notification', 'POST', '{{baseUrl}}/api/notifications/send', {
		body: notificationBody(),
	}),
	req('12. GET missions actives', 'GET', '{{baseUrl}}/api/g4/missions/actives'),
	req('13. GET health', 'GET', '{{baseUrl}}/api/g4/health', { noauth: true }),
];

collection.item.push(
	folder(
		'GUIDE — Parcours complet (1→13)',
		'Exécuter dans l\'ordre, une requête à la fois. Ne pas sauter d\'étapes.',
		guide
	)
);

collection.item.push(
	folder('02 — Lignes', 'Login gestionnaire.reseau', [
		{
			...req('POST créer ligne', 'POST', '{{baseUrl}}/api/g4/lignes', {
				body: '{\n  "code": "L99",\n  "nom": "Ligne détaillée",\n  "active": true\n}',
			}),
			event: [saveId('ligneId')],
		},
		req('GET toutes', 'GET', '{{baseUrl}}/api/g4/lignes'),
		req('GET actives', 'GET', '{{baseUrl}}/api/g4/lignes/actives'),
		req('GET par id', 'GET', '{{baseUrl}}/api/g4/lignes/{{ligneId}}'),
		req('GET trajets de la ligne', 'GET', '{{baseUrl}}/api/g4/lignes/{{ligneId}}/trajets'),
		req('PUT modifier', 'PUT', '{{baseUrl}}/api/g4/lignes/{{ligneId}}', {
			body: '{\n  "code": "L99",\n  "nom": "Ligne maj",\n  "active": true\n}',
		}),
		req('DELETE (fin de test)', 'DELETE', '{{baseUrl}}/api/g4/lignes/{{ligneId}}', {
			desc: 'Optionnel',
		}),
	])
);

collection.item.push(
	folder('03 — Arrêts', 'Après une ligne (variables ligneId)', [
		{
			...req('POST arrêt 1', 'POST', '{{baseUrl}}/api/g4/arrets', {
				body:
					'{\n  "code": "AR1",\n  "nom": "Stop 1",\n  "latitude": 35.57,\n  "longitude": -5.37,\n  "ligneId": {{ligneId}}\n}',
			}),
			event: [saveId('arretId')],
		},
		{
			...req('POST arrêt 2', 'POST', '{{baseUrl}}/api/g4/arrets', {
				body:
					'{\n  "code": "AR2",\n  "nom": "Stop 2",\n  "latitude": 35.58,\n  "longitude": -5.36,\n  "ligneId": {{ligneId}}\n}',
			}),
			event: [saveId('arretId2')],
		},
		req('GET tous', 'GET', '{{baseUrl}}/api/g4/arrets'),
		req('GET par id', 'GET', '{{baseUrl}}/api/g4/arrets/{{arretId}}'),
		req('GET par ligne', 'GET', '{{baseUrl}}/api/g4/arrets/ligne/{{ligneId}}'),
	])
);

collection.item.push(
	folder('04 — Trajets', 'Après 2 arrêts', [
		{
			...req('POST créer trajet', 'POST', '{{baseUrl}}/api/g4/trajets', {
				body:
					'{\n  "ligneId": {{ligneId}},\n  "code": "T-A",\n  "nom": "Aller",\n  "sens": "ALLER",\n  "actif": true,\n  "arretSequence": [\n    { "arretId": {{arretId}}, "sequenceOrder": 1 },\n    { "arretId": {{arretId2}}, "sequenceOrder": 2 }\n  ]\n}',
			}),
			event: [saveId('trajetId')],
		},
		req('GET tous', 'GET', '{{baseUrl}}/api/g4/trajets'),
		req('GET par id', 'GET', '{{baseUrl}}/api/g4/trajets/{{trajetId}}'),
		req('GET arrêts du trajet', 'GET', '{{baseUrl}}/api/g4/trajets/{{trajetId}}/arrets'),
	])
);

collection.item.push(
	folder('05 — Horaires', 'Après trajet', [
		{
			...req('POST créer horaire', 'POST', '{{baseUrl}}/api/g4/horaires', {
				body:
					'{\n  "trajetId": {{trajetId}},\n  "arretId": {{arretId}},\n  "heurePassage": "08:00:00",\n  "jourSemaine": 1,\n  "validFrom": "2026-01-01",\n  "validTo": "2026-12-31",\n  "libelle": "Matin"\n}',
			}),
			event: [saveId('horaireId')],
		},
		req('GET tous', 'GET', '{{baseUrl}}/api/g4/horaires'),
		req('GET par id', 'GET', '{{baseUrl}}/api/g4/horaires/{{horaireId}}'),
	])
);

collection.item.push(
	folder('05b — Référentiel véhicules G7', 'UUID G7 obligatoire — avant affectation', [
		req('GET véhicules disponibles', 'GET', '{{baseUrl}}/api/g4/vehicules/disponibles', {
			desc: 'Véhicules DISPONIBLE (Kafka vehicle.registered ou sync)',
		}),
		req('GET tous véhicules référentiel', 'GET', '{{baseUrl}}/api/g4/vehicules'),
		req('GET véhicule par id', 'GET', '{{baseUrl}}/api/g4/vehicules/{{vehiculeId}}'),
		req('POST sync depuis G7', 'POST', '{{baseUrl}}/api/g4/vehicules/sync-from-g7/{{vehiculeId}}', {
			desc: 'Secours si Kafka indisponible — G7 doit être UP sur g7BaseUrl',
		}),
	])
);

collection.item.push(
	folder('06 — Affectations', 'Login gestionnaire.flotte — après véhicule DISPONIBLE', [
		{
			...req('POST créer affectation', 'POST', '{{baseUrl}}/api/g4/affectations', {
				body:
					'{\n  "vehiculeId": "{{vehiculeId}}",\n  "ligneId": {{ligneId}},\n  "dateDebut": "2026-05-07T06:00:00Z",\n  "statut": "ACTIF"\n}',
			}),
			event: [saveId('affectationId')],
		},
		req('GET toutes', 'GET', '{{baseUrl}}/api/g4/affectations'),
		req('GET par véhicule', 'GET', '{{baseUrl}}/api/g4/affectations/vehicule/{{vehiculeId}}'),
	])
);

collection.item.push(
	folder('07 — Missions', 'Login gestionnaire.flotte — après affectation', [
		{
			...req('POST créer mission', 'POST', '{{baseUrl}}/api/g4/missions', {
				body:
					'{\n  "vehiculeId": "{{vehiculeId}}",\n  "ligneId": {{ligneId}},\n  "trajetId": {{trajetId}},\n  "affectationId": {{affectationId}},\n  "statut": "PLANIFIEE",\n  "plannedStart": "2026-05-07T08:00:00Z"\n}',
			}),
			event: [saveId('missionId')],
		},
		req('GET toutes', 'GET', '{{baseUrl}}/api/g4/missions'),
		req('GET actives', 'GET', '{{baseUrl}}/api/g4/missions/actives'),
		req('GET statut', 'GET', '{{baseUrl}}/api/g4/missions/{{missionId}}/status'),
		req('PUT modifier', 'PUT', '{{baseUrl}}/api/g4/missions/{{missionId}}', {
			body:
				'{\n  "vehiculeId": "{{vehiculeId}}",\n  "ligneId": {{ligneId}},\n  "trajetId": {{trajetId}},\n  "statut": "EN_COURS",\n  "plannedStart": "2026-05-07T08:00:00Z"\n}',
		}),
		req('POST clôturer', 'POST', '{{baseUrl}}/api/g4/missions/{{missionId}}/cloturer'),
		req('POST annuler', 'POST', '{{baseUrl}}/api/g4/missions/{{missionId}}/annuler'),
	])
);

collection.item.push(
	folder('08 — Événements coordination', 'Pas incident G9 ici', [
		req('POST enregistrer événement', 'POST', '{{baseUrl}}/api/g4/events', {
			body:
				'{\n  "type": "RETARD",\n  "status": "SIGNALE",\n  "missionId": {{missionId}},\n  "vehiculeId": "{{vehiculeId}}",\n  "description": "Test"\n}',
		}),
		req('POST detect-delay', 'POST', '{{baseUrl}}/api/g4/events/detect-delay', {
			body: '{\n  "missionId": {{missionId}},\n  "retardMinutes": 5,\n  "cause": "Test"\n}',
		}),
		req('POST detect-deviation', 'POST', '{{baseUrl}}/api/g4/events/detect-deviation', {
			body: '{\n  "missionId": {{missionId}},\n  "details": "Hors itinéraire",\n  "ecartMetres": 100\n}',
		}),
		req('POST detect-breakdown', 'POST', '{{baseUrl}}/api/g4/events/detect-breakdown', {
			body:
				'{\n  "vehiculeId": "{{vehiculeId}}",\n  "missionId": {{missionId}},\n  "symptomes": "Panne"\n}',
		}),
		req('GET par type RETARD', 'GET', '{{baseUrl}}/api/g4/events/type/RETARD'),
		req('POST cancel-mission', 'POST', '{{baseUrl}}/api/g4/events/cancel-mission', {
			body: '{\n  "missionId": {{missionId}},\n  "motif": "Test",\n  "notifierG1": false\n}',
		}),
	])
);

collection.item.push(
	folder('09 — Impacts incident G9', 'Distinct de /api/g4/events', [
		{
			...req('POST impact', 'POST', '{{baseUrl}}/api/g4/incident-impacts', {
				body:
					'{\n  "incidentReference": "INC-2026-001",\n  "missionId": {{missionId}},\n  "vehiculeId": "{{vehiculeId}}",\n  "resume": "Test REST (Kafka en prod)"\n}',
			}),
			event: [saveId('impactId')],
		},
		req('GET tous', 'GET', '{{baseUrl}}/api/g4/incident-impacts'),
		req('GET par mission', 'GET', '{{baseUrl}}/api/g4/incident-impacts/mission/{{missionId}}'),
	])
);

collection.item.push(
	folder('10 — Notifications (G5)', 'Login flotte', [
		(() => {
			const r = req('POST envoyer notification', 'POST', '{{baseUrl}}/api/notifications/send', {
				body: notificationBody(),
				desc: '202 ACCEPTED ou 202 DEGRADED si G5 arrêté',
			});
			r.event = [expectStatus(202)];
			return r;
		})(),
	])
);

collection.item.push(
	folder('11 — Référence G7 (lecture v1)', 'GET seulement — contrat G7', [
		req('GET v1 lignes', 'GET', '{{baseUrl}}/api/v1/lignes'),
		req('GET v1 trajets ligne', 'GET', '{{baseUrl}}/api/v1/lignes/{{ligneId}}/trajet'),
		req('GET v1 horaires ligne', 'GET', '{{baseUrl}}/api/v1/lignes/{{ligneId}}/horaires'),
		req('GET v1 arrets', 'GET', '{{baseUrl}}/api/v1/arrets'),
		req('GET v1 arret id', 'GET', '{{baseUrl}}/api/v1/arrets/{{arretId}}'),
	])
);

collection.item.push(
	folder('12 — Supervision', 'Admin pour pending et operator', [
		req('GET logs (public)', 'GET', '{{baseUrl}}/api/g4/logs', { noauth: true }),
		req('GET operator status', 'GET', '{{baseUrl}}/api/v1/operator/status', {
			desc: 'Login admin.technique',
		}),
		req('GET pending notifications', 'GET', '{{baseUrl}}/api/g4/pending-notifications', {
			desc: 'Login admin.technique',
		}),
		req('POST retry pending', 'POST', '{{baseUrl}}/api/g4/pending-notifications/retry', {
			desc: 'Login admin.technique',
		}),
		req('GET actuator prometheus', 'GET', '{{baseUrl}}/actuator/prometheus', { noauth: true }),
	])
);

collection.item.push(
	folder('90 — Erreurs', 'Exemples', [
		req('GET missions sans JWT → 401', 'GET', '{{baseUrl}}/api/g4/missions', { noauth: true }),
		req('GET ligne 999999 → 404', 'GET', '{{baseUrl}}/api/g4/lignes/999999'),
	])
);

collection.item.push(
	folder('99 — Intégration G3 (optionnel)', 'Si G3 tourne sur 8083', [
		{
			...req('G3 Login', 'POST', '{{g3BaseUrl}}/api/auth/login', {
				noauth: true,
				body: '{\n  "username": "gestionnaire.flotte",\n  "password": "password"\n}',
			}),
			event: [
				{
					listen: 'test',
					script: {
						type: 'text/javascript',
						exec: [
							'if (pm.response.code === 200) {',
							"  pm.collectionVariables.set('g3AccessToken', pm.response.json().token);",
							'}',
						],
					},
				},
			],
		},
		(() => {
			const r = req('G4 GET missions avec token G3', 'GET', '{{baseUrl}}/api/g4/missions');
			r.request.auth = {
				type: 'bearer',
				bearer: [{ key: 'token', value: '{{g3AccessToken}}', type: 'string' }],
			};
			return r;
		})(),
	])
);

collection.item.push(
	folder('100 — Chaos G5 (optionnel)', 'Arrêter G5 puis tester', [
		req('POST notification DEGRADED', 'POST', '{{baseUrl}}/api/notifications/send', {
			body:
				'{\n  "notificationId": "CHAOS-001",\n  "sourceService": "COORDINATION",\n  "eventType": "DELAY_ALERT",\n  "channel": "EMAIL",\n  "recipient": { "userId": "{{recipientUserId}}", "email": "{{recipientEmail}}" },\n  "metadata": { "lineId": "L12", "reason": "RETARD_SIGNIFICATIF", "variables": { "vehiculeId": "{{vehiculeId}}", "valeur": "12", "arret": "Gare Sud" } }\n}',
		}),
	])
);

collection.item.push(
	folder('101 — OpenAPI', null, [
		req('GET v3 api-docs', 'GET', '{{baseUrl}}/v3/api-docs', { noauth: true }),
	])
);

const out =
	'c:/Users/daurinia/Desktop/SGITU-Microservices/service-coordination-transport/postman/SGITU-G4-Coordination-Transport.postman_collection.json';
fs.writeFileSync(out, JSON.stringify(collection, null, '\t'));
console.log('Written:', out);
