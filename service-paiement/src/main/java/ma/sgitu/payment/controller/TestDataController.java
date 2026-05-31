package ma.sgitu.payment.controller;

import lombok.RequiredArgsConstructor;
import ma.sgitu.payment.dto.response.TestCardResponse;
import ma.sgitu.payment.dto.response.TestMobileMoneyResponse;
import ma.sgitu.payment.entity.TestCard;
import ma.sgitu.payment.entity.TestMobileMoneyAccount;
import ma.sgitu.payment.repository.TestCardRepository;
import ma.sgitu.payment.repository.TestMobileMoneyAccountRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TestDataController {

    private final TestCardRepository testCardRepository;
    private final TestMobileMoneyAccountRepository testMobileMoneyRepository;

    @GetMapping("/test-cards")
    public List<TestCardResponse> getTestCards() {
        return testCardRepository.findAll().stream()
                .map(card -> TestCardResponse.builder()
                        .id(card.getId())
                        .last4(card.getLast4())
                        .cardHolderName(card.getCardHolderName())
                        .expiryMonth(card.getExpiryMonth())
                        .expiryYear(card.getExpiryYear())
                        .provider(card.getProvider())
                        .balance(card.getBalance())
                        .status(card.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/test-mobile-money-accounts")
    public List<TestMobileMoneyResponse> getTestMobileMoneyAccounts() {
        return testMobileMoneyRepository.findAll().stream()
                .map(account -> TestMobileMoneyResponse.builder()
                        .id(account.getId())
                        .maskedPhone(account.getMaskedPhone())
                        .provider(account.getProvider())
                        .balance(account.getBalance())
                        .status(account.getStatus().name())
                        .build())
                .collect(Collectors.toList());
    }
}