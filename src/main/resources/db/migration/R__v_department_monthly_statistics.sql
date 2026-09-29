-- View to aggregate monthly statistics per department.
-- This is a repeatable migration which runs every time a checksum change is detected.
-- Any changes to the required attributes will result in a failed migration and adjustments to the view must be made.

CREATE OR REPLACE VIEW v_department_monthly_statistics AS
SELECT d.id                                                       AS department_id,
       d.name                                                     AS department_name,
       YEAR(m.created)                                            AS year,
       MONTH(m.created)                                           AS month,

       SUM(IF(UPPER(r.type) = 'SNAIL_MAIL', 1, 0))                AS snail_mail_count,
       SUM(IF(UPPER(r.type) = 'DIGITAL_MAIL', 1, 0))              AS digital_mail_count,
       SUM(IF(UPPER(r.type) = 'DIGITAL_REGISTERED_LETTER', 1, 0)) AS digital_registered_letter_count,
       SUM(IF(UPPER(r.type) = 'SMS', 1, 0))                       AS sms_count,
       SUM(IF(UPPER(r.type) = 'E_SIGNING', 1, 0))                 AS e_signing_count

FROM message m
         JOIN department d ON d.id = m.department_id
         JOIN recipient r ON r.message_id = m.id
WHERE m.created IS NOT NULL
GROUP BY d.id, d.name, YEAR(m.created), MONTH(m.created);
