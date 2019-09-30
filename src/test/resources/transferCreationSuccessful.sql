BEGIN;

DELETE FROM PUBLIC.TRANSFERS;
DELETE FROM PUBLIC.ACCOUNTS;

INSERT INTO PUBLIC.ACCOUNTS (
    ID,
    PUBLIC_IDENTIFIER,
    BALANCE
)
VALUES
       (1, 'a0066ed4-f2be-47ea-b591-8f21bfdb46a3', 50.32),
       (2, '9dc98139-9c99-4bf8-aa42-0dc2f6fe3165', 0.10),
       (3, '2f3b9c39-4267-4f74-9d51-f1ed5d3286c5', 420.00),
       (4, 'c4089f6b-5763-402a-88f4-a9158a1514fe', 0.00),
       (5, '90048b26-be08-4a2e-be4a-b25669474991', 673.00),
       (6, 'a39d2b51-6cca-4309-8cc2-d54d80542103', 90.61),
       (7, 'fcfef11f-70ee-4e5e-b4f5-e21e5cd1081d', 5754.09)
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
    'b4c74412-43dc-4c25-ac41-ec0d8d522d2f',
    7,
    1,
    50.32,
    1
),
(
    '686f744d-8f07-4fe0-930f-aac5c0b48d8e',
    7,
    6,
    20.00,
    1
),
(
    '3c8bd761-2d7d-4e60-a9f1-cb8d455597f7',
    7,
    6,
    20.00,
    0
);

COMMIT;