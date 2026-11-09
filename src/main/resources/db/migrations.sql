-- Idempotent data migrations, run on every startup after Hibernate updates the schema
-- (ddl-auto=update adds tables and columns but never alters existing ones).

-- Roles ROLE_SECRETARY_1 / ROLE_SECRETARY_2 renamed to ROLE_COUNCIL_SECRETARY / ROLE_FACULTY_SECRETARY.
ALTER TABLE user_roles MODIFY role ENUM('ROLE_DEV','ROLE_ADMIN','ROLE_SECRETARY_1','ROLE_SECRETARY_2','ROLE_COUNCIL_SECRETARY','ROLE_FACULTY_SECRETARY','ROLE_PRESIDENT','ROLE_COUNCIL_MEMBER') NOT NULL;
UPDATE user_roles SET role = 'ROLE_COUNCIL_SECRETARY' WHERE role = 'ROLE_SECRETARY_1';
UPDATE user_roles SET role = 'ROLE_FACULTY_SECRETARY' WHERE role = 'ROLE_SECRETARY_2';
ALTER TABLE user_roles MODIFY role ENUM('ROLE_DEV','ROLE_ADMIN','ROLE_COUNCIL_SECRETARY','ROLE_FACULTY_SECRETARY','ROLE_PRESIDENT','ROLE_COUNCIL_MEMBER') NOT NULL;

-- Virtual sessions are held by email: the location is optional.
ALTER TABLE sessions MODIFY location VARCHAR(200) NULL;

-- ROLE_DEV merged into ROLE_ADMIN (templates and integrity alerts stay with the administrator).
ALTER TABLE user_roles MODIFY role ENUM('ROLE_DEV','ROLE_ADMIN','ROLE_COUNCIL_SECRETARY','ROLE_FACULTY_SECRETARY','ROLE_PRESIDENT','ROLE_COUNCIL_MEMBER') NOT NULL;
DELETE ur_dev FROM user_roles ur_dev
    INNER JOIN user_roles ur_admin ON ur_admin.user_id = ur_dev.user_id AND ur_admin.role = 'ROLE_ADMIN'
    WHERE ur_dev.role = 'ROLE_DEV';
UPDATE user_roles SET role = 'ROLE_ADMIN' WHERE role = 'ROLE_DEV';

-- Former request types become topics of the council catalog.
-- The column was a MySQL enum of the four old values; widen it before rewriting rows.
ALTER TABLE council_requests MODIFY request_type VARCHAR(40) NOT NULL;
UPDATE council_requests SET request_type = 'PETITION' WHERE request_type IN ('ACADEMIC', 'ADMINISTRATIVE', 'STUDENT');
UPDATE council_requests SET request_type = 'EVALUATION' WHERE request_type = 'TEACHING';
ALTER TABLE user_roles MODIFY role ENUM('ROLE_ADMIN','ROLE_COUNCIL_SECRETARY','ROLE_FACULTY_SECRETARY','ROLE_PRESIDENT','ROLE_COUNCIL_MEMBER') NOT NULL;

-- Program head: activates and deactivates the secretary of their own program.
ALTER TABLE user_roles MODIFY role ENUM('ROLE_ADMIN','ROLE_COUNCIL_SECRETARY','ROLE_FACULTY_SECRETARY','ROLE_PRESIDENT','ROLE_COUNCIL_MEMBER','ROLE_PROGRAM_HEAD') NOT NULL;
