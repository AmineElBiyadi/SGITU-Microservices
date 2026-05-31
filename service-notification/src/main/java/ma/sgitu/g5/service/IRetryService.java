package ma.sgitu.g5.service;

public interface IRetryService {

    /**
     * Détermine si une nouvelle tentative doit être effectuée.
     * Basé sur le nombre de tentatives déjà effectuées.
     *
     * Politique par défaut : max 3 tentatives.
     *
     * @param currentRetryCount nombre de tentatives déjà faites
     * @return true si on doit réessayer, false si on passe en FAILED définitif
     */
    boolean shouldRetry(int currentRetryCount);

    /**
     * Calcule le délai d'attente avant la prochaine tentative
     * en utilisant un backoff exponentiel.
     *
     * Exemple : tentative 0 → 30s, tentative 1 → 60s, tentative 2 → 120s
     *
     * @param currentRetryCount nombre de tentatives déjà faites
     * @return délai en secondes avant la prochaine tentative
     */
    int nextDelaySeconds(int currentRetryCount);

    /**
     * Planifie l'exécution différée du retry pour une notification donnée.
     * Appelé par NotificationServiceImpl après une mise à jour du statut.
     *
     * Implémentation suggérée : @Scheduled + queue interne,
     * ou TaskScheduler Spring pour déclencher dispatchAsyncFromEntity()
     * après le délai calculé.
     *
     * @param notificationId UUID de la notification à relancer
     * @param delaySeconds   délai d'attente en secondes
     */
    void scheduleRetry(String notificationId, int delaySeconds);
}