BEGIN;

DELETE FROM PUBLIC.TRANSFERS;
DELETE FROM PUBLIC.ACCOUNTS;

INSERT INTO PUBLIC.ACCOUNTS (
    ID,
    PUBLIC_IDENTIFIER,
    BALANCE
)
VALUES
       (1, 'fa11d9b8-7a19-4805-a53e-d3dfdc87584c', 50.32),
       (2, 'bb68d31c-fee4-43c2-8f30-4521de151a9f', 0.10)
;

INSERT INTO PUBLIC.TRANSFERS (
    ID,
    SOURCE_ACCOUNT,
    DESTINATION_ACCOUNT,
    AMOUNT,
    STATUS
)
VALUES
(
    'ede9bdd4-10ec-41b1-85fc-2831585f6352',
    1,
    2,
    20.32,
    1
);

COMMIT;