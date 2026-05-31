package ma.sgitu.payment.repository;

import ma.sgitu.payment.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les factures
 * Requêtes JPA pour Invoice
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Trouve une facture par son numéro unique
     * @param invoiceNumber Numéro de facture (ex: INV-PAY-100)
     * @return Optional<Invoice>
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Trouve une facture par l'ID du paiement
     * @param paymentId ID du paiement
     * @return Optional<Invoice>
     */
    Optional<Invoice> findByPaymentId(Long paymentId);

    /**
     * Liste toutes les factures d'un utilisateur
     * @param userId ID utilisateur
     * @return List<Invoice>
     */
    List<Invoice> findByUserIdOrderByIssuedAtDesc(Long userId);

    /**
     * Vérifie si une facture existe pour un paiement donné
     * @param paymentId ID du paiement
     * @return boolean
     */
    boolean existsByPaymentId(Long paymentId);
}