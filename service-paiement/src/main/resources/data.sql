DELETE FROM test_mobile_money_accounts;

-- Compte Orange avec 1000 DH (C'est le numéro que tu utiliseras pour ta démo)
-- Numéro : 0611000044
INSERT INTO test_mobile_money_accounts (phone_hash, masked_phone, provider, balance, status)
VALUES ('AJwr1edN+mg/JTAG8lvysijxHWFVEp5Od8GYTcHrw8k=', '0611****44', 'ORANGE', 1000.00, 'ACTIVE');