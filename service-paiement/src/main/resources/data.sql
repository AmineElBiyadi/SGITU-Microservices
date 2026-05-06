DELETE FROM test_mobile_money_accounts;

-- Compte Orange avec 1000 DH (C'est le numéro que tu utiliseras pour ta démo)
-- Numéro : 0611000044
INSERT INTO test_mobile_money_accounts (phone_hash, masked_phone, provider, balance, status)
VALUES ('009c2bd5e74dfa683f253006f25bf2b228f11d6155129e4e77c1984dc1ebc3c9', '0611****44', 'ORANGE', 1000.00, 'ACTIVE');
