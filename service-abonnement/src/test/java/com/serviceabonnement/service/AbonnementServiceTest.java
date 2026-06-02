package com.serviceabonnement.service;

import com.serviceabonnement.client.AnalyseClient;
import com.serviceabonnement.client.PaiementClient;
import com.serviceabonnement.client.UtilisateurServiceClient;
import com.serviceabonnement.dto.external.PaymentCallbackDTO;
import com.serviceabonnement.dto.external.PaymentResponseDTO;
import com.serviceabonnement.dto.external.RefundCallbackDTO;
import com.serviceabonnement.dto.external.UserDTO;
import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.enums.CategorieAbonnement;
import com.serviceabonnement.enums.DureeOffre;
import com.serviceabonnement.enums.MoyenTransport;
import com.serviceabonnement.enums.StatutAbonnement;
import com.serviceabonnement.enums.StatutOffre;
import com.serviceabonnement.exception.AbonnementNotFoundException;
import com.serviceabonnement.producer.SubscriptionEventPublisher;
import com.serviceabonnement.repository.*;
import com.serviceabonnement.service.impl.AbonnementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AbonnementServiceImpl.
 * Ces tests simulent les relations inter-microservices (G3 Utilisateurs, G6 Paiement, G5 Notifications)
 * avec des Mocks Mockito, sans nécessiter une vraie connexion réseau.
 */
@ExtendWith(MockitoExtension.class)
class AbonnementServiceTest {

    // ── Mocks des dépendances inter-microservices ──────────────────────────────────
    @Mock
    private AbonnementRepository abonnementRepository;
    @Mock
    private PlanAbonnementRepository planRepository;
    @Mock
    private UtilisateurServiceClient userClient;   // Mock du microservice G3 (Utilisateurs)
    @Mock
    private PaiementClient paiementClient;         // Mock du microservice G6 (Paiement)
    @Mock
    private AnalyseClient analyseClient;           // Mock du microservice G8 (Analyse)
    @Mock
    private SubscriptionEventPublisher eventPublisher; // Mock des notifications Kafka (G5)
    @Mock
    private AnalytiqueTraceRepository analytiqueTraceRepository;
    @Mock
    private DesactivationRepository desactivationRepository;
    @Mock
    private RenouvellementRepository renouvellementRepository;
    @Mock
    private SuspensionAdminRepository suspensionAdminRepository;

    @InjectMocks
    private AbonnementServiceImpl abonnementService;

    private Abonnement sampleAbonnement;
    private PlanAbonnement samplePlan;

    @BeforeEach
    void setUp() {
        // Injection manuelle de 'self' pour permettre le fonctionnement des appels @Transactional internes mockés
        ReflectionTestUtils.setField(abonnementService, "self", abonnementService);

        samplePlan = new PlanAbonnement();
        samplePlan.setIdPlan(10L);
        samplePlan.setNomPlan("Forfait Mensuel Étudiant");
        samplePlan.setPrix(150.0);
        samplePlan.setDuree(DureeOffre.MENSUEL);
        samplePlan.setCategorie(CategorieAbonnement.ROLE_STUDENT);
        samplePlan.setTransportType(MoyenTransport.BUS);
        samplePlan.setEstActif(StatutOffre.ACTIF);

        sampleAbonnement = Abonnement.builder()
                .id(1L)
                .userId(42L)
                .userEmail("etudiant@test.com")
                .plan(samplePlan)
                .statut(StatutAbonnement.EN_ATTENTE_PAIEMENT)
                .prixPaye(150.0)
                .paiementId("TRX-001")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 1 : Confirmation de Paiement (Callback G6 → G2)
    // Scénario : Le microservice de paiement (G6) envoie un webhook SUCCESS
    //            → On attend que l'abonnement passe de EN_ATTENTE_PAIEMENT à ACTIF
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testConfirmerPaiement_Success_AbonnementActif() {
        // Arrange
        PaymentCallbackDTO callback = new PaymentCallbackDTO();
        callback.setTransactionToken("TRX-001");
        callback.setStatus("SUCCESS");

        when(abonnementRepository.findByPaiementId("TRX-001")).thenReturn(Optional.of(sampleAbonnement));
        when(abonnementRepository.findByUserIdAndStatut(any(), any())).thenReturn(java.util.List.of());

        // Act
        abonnementService.confirmerPaiement(callback);

        // Assert : L'abonnement doit maintenant être ACTIF
        assertEquals(StatutAbonnement.ACTIF, sampleAbonnement.getStatut(),
                "Après un paiement SUCCESS de G6, l'abonnement doit passer à ACTIF");
        assertNotNull(sampleAbonnement.getDateDebut(), "La date de début doit être définie");
        assertNotNull(sampleAbonnement.getDateFin(), "La date de fin doit être calculée");

        // On vérifie que le repository a bien été appelé pour sauvegarder
        verify(abonnementRepository, atLeastOnce()).save(sampleAbonnement);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 2 : Confirmation de Paiement ECHEC (Callback G6 → G2)
    // Scénario : Le microservice de paiement (G6) envoie un webhook FAILED
    //            → L'abonnement doit passer à ECHEC_PAIEMENT, pas à ACTIF
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testConfirmerPaiement_Echec_StatutEchecPaiement() {
        // Arrange
        PaymentCallbackDTO callback = new PaymentCallbackDTO();
        callback.setTransactionToken("TRX-001");
        callback.setStatus("FAILED");
        callback.setMessage("Solde insuffisant");

        when(abonnementRepository.findByPaiementId("TRX-001")).thenReturn(Optional.of(sampleAbonnement));

        // Act
        abonnementService.confirmerPaiement(callback);

        // Assert : L'abonnement ne doit PAS être actif après un échec de paiement
        assertEquals(StatutAbonnement.ECHEC_PAIEMENT, sampleAbonnement.getStatut(),
                "Après un paiement FAILED de G6, l'abonnement doit passer à ECHEC_PAIEMENT");
        verify(abonnementRepository).save(sampleAbonnement);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 3 : Confirmation de Remboursement (Callback G6 → G2)
    // Scénario : L'utilisateur a demandé une annulation, G6 confirme le remboursement
    //            → L'abonnement doit passer à ANNULE avec une date d'annulation
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testConfirmerRemboursement_Success_AbonnementAnnule() {
        // Arrange : L'abonnement est en cours d'annulation (en attente du remboursement G6)
        sampleAbonnement.setStatut(StatutAbonnement.ANNULATION_EN_COURS);

        RefundCallbackDTO callback = new RefundCallbackDTO();
        callback.setTransactionId("TRX-001");
        callback.setStatut("REMBOURSE");
        callback.setMontantRembourse(85.0); // G6 rembourse au prorata

        when(abonnementRepository.findByPaiementId("TRX-001")).thenReturn(Optional.of(sampleAbonnement));
        when(userClient.getUserById(42L)).thenReturn(null); // G3 peut être indisponible

        // Act
        abonnementService.confirmerRemboursement(callback);

        // Assert : L'abonnement doit être ANNULE
        assertEquals(StatutAbonnement.ANNULE, sampleAbonnement.getStatut(),
                "Après confirmation du remboursement par G6, l'abonnement doit être ANNULE");
        assertNotNull(sampleAbonnement.getDateAnnulation(), "La date d'annulation doit être renseignée");
        verify(abonnementRepository).save(sampleAbonnement);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 4 : Callback d'un token inconnu (Résilience des webhooks)
    // Scénario : G6 envoie un callback avec un token invalide
    //            → Doit lancer une exception et ne pas crasher silencieusement
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testConfirmerPaiement_TokenInconnu_LanceException() {
        // Arrange
        PaymentCallbackDTO callback = new PaymentCallbackDTO();
        callback.setTransactionToken("TOKEN-INCONNU");
        callback.setStatus("SUCCESS");

        when(abonnementRepository.findByPaiementId("TOKEN-INCONNU")).thenReturn(Optional.empty());
        when(renouvellementRepository.findByPaiementId("TOKEN-INCONNU")).thenReturn(Optional.empty());

        // Act & Assert : On attend une AbonnementNotFoundException
        assertThrows(AbonnementNotFoundException.class,
                () -> abonnementService.confirmerPaiement(callback),
                "Un token inconnu doit déclencher une AbonnementNotFoundException");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 5 : Calcul de Remboursement au Prorata
    // Scénario : Logique purement mathématique, sans appel externe
    //            → Vérification du calcul selon les jours restants
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testCalculerRemboursement_ProrataJoursRestants() {
        // Arrange : Abonnement de 30 jours, commencé il y a 10 jours → 20 jours restants
        sampleAbonnement.setDateDebut(LocalDateTime.now().minusDays(10));
        sampleAbonnement.setDateFin(LocalDateTime.now().plusDays(20));
        sampleAbonnement.setPrixPaye(150.0);

        // Act
        Double remboursement = abonnementService.calculerRemboursement(sampleAbonnement);

        // Assert : prorata = (150 / 30) * 20 = 100.0
        assertNotNull(remboursement);
        assertTrue(remboursement > 0, "Le remboursement doit être positif si des jours restent");
        assertTrue(remboursement < 150.0, "Le remboursement ne peut pas dépasser le prix payé");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 6 : Nouvelle Souscription Complète (G3, G6, Kafka G5)
    // Scénario : Un utilisateur s'abonne à un nouveau plan. Il respecte les rôles.
    //            On s'assure qu'on initie le paiement chez G6 et qu'on archive l'évènement.
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testSouscrire_Success() {
        // Arrange : Mock SecurityContext pour "extractUserRoles"
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(auth);
        
        // Simule le rôle correspondant au CategorieAbonnement.ROLE_STUDENT
        java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = 
                java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_STUDENT"));
        doReturn(authorities).when(auth).getAuthorities();

        // Arrange : G3 (Utilisateur)
        UserDTO user = new UserDTO();
        user.setId(42L);
        user.setEmail("etudiant@test.com");
        user.setActive(true);
        when(userClient.getUserById(42L)).thenReturn(user);

        // Arrange : Base locale (Plan + Pas de souscription en cours)
        when(planRepository.findById(10L)).thenReturn(Optional.of(samplePlan));
        when(abonnementRepository.findByUserIdAndStatut(42L, StatutAbonnement.ACTIF)).thenReturn(java.util.List.of());
        when(abonnementRepository.findByUserIdAndPlanIdPlanAndStatut(42L, 10L, StatutAbonnement.EN_ATTENTE_PAIEMENT))
                .thenReturn(Optional.empty()); // Pas d'idempotence

        // Arrange : Local save
        when(abonnementRepository.save(any(Abonnement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Arrange : G6 (Paiement)
        PaymentResponseDTO pmResponse = new PaymentResponseDTO();
        pmResponse.setTransactionId("NEW-TRX-123");
        when(paiementClient.initierPaiement(any())).thenReturn(pmResponse);

        // Act
        Abonnement result = abonnementService.souscrire(42L, 10L, "etudiant@test.com");

        // Assert
        assertNotNull(result);
        assertEquals(StatutAbonnement.EN_ATTENTE_PAIEMENT, result.getStatut());
        assertEquals("NEW-TRX-123", result.getPaiementId(), "Le paiement (G6) a dû être attaché à l'abonnement");

        // Vérifie qu'on a bien appelé le service distant G6
        verify(paiementClient, times(1)).initierPaiement(any());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 7 : Échec de souscription (Rôle Inéligible) → pas de G6 ni G5
    // Scénario : Un usager "ROLE_PASSENGER" tente un plan "ROLE_STUDENT".
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testSouscrire_Echec_RoleInvalide() {
        // Arrange : Mock SecurityContext avec mauvais rôle
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(auth);
        
        java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = 
                java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PASSENGER"));
        doReturn(authorities).when(auth).getAuthorities();

        UserDTO user = new UserDTO();
        user.setId(42L);
        user.setActive(true);
        when(userClient.getUserById(42L)).thenReturn(user);

        when(planRepository.findById(10L)).thenReturn(Optional.of(samplePlan));

        // Act & Assert
        com.serviceabonnement.exception.RegleMetierException exception = assertThrows(
                com.serviceabonnement.exception.RegleMetierException.class,
                () -> abonnementService.souscrire(42L, 10L, "etudiant@test.com")
        );

        assertTrue(exception.getMessage().contains("non eligible"));

        // Vérification : Aucun appel n'est fait à G6 ou G5 car la souscription bloque dès le début !
        verify(paiementClient, never()).initierPaiement(any());
        verify(eventPublisher, never()).publishEchecSouscription(any(), any(), any(), any(), any());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // TEST 8 : Notification G5 — Confirmation de Souscription (Kafka)
    // Scénario : Quand G6 valide le paiement (SUCCESS), notre service doit
    //            immédiatement publier un événement Kafka vers G5 (Notifications)
    //            en appelant publishConfirmationSouscription().
    // ═══════════════════════════════════════════════════════════════════════════════
    @Test
    void testConfirmerPaiement_Success_EnvoieNotificationG5() {
        // Arrange : Callback SUCCESS de G6
        PaymentCallbackDTO callback = new PaymentCallbackDTO();
        callback.setTransactionToken("TRX-001");
        callback.setStatus("SUCCESS");

        when(abonnementRepository.findByPaiementId("TRX-001")).thenReturn(Optional.of(sampleAbonnement));
        when(abonnementRepository.findByUserIdAndStatut(any(), any())).thenReturn(java.util.List.of());
        when(abonnementRepository.save(any(Abonnement.class))).thenReturn(sampleAbonnement);

        // Simuler G3 pour le fetchUser dans la méthode de notification
        com.serviceabonnement.dto.external.UserDTO user = new com.serviceabonnement.dto.external.UserDTO();
        user.setId(42L);
        user.setEmail("etudiant@test.com");
        when(userClient.getUserById(42L)).thenReturn(user);

        // Act
        abonnementService.confirmerPaiement(callback);

        // Assert : Le publisher Kafka (canal G5) a bien été invoqué avec les bonnes données
        verify(eventPublisher, times(1))
                .publishConfirmationSouscription(
                        argThat(u -> u.getId().equals(42L) && "etudiant@test.com".equals(u.getEmail())),
                        argThat(a -> a.getPlan().getNomPlan().equals("Forfait Mensuel Étudiant"))
                );
    }
}
