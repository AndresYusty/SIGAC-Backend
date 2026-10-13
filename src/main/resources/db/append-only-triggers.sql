-- RN-19: the audit log is append-only: not even ADMIN or DEV can modify or delete it.
-- Runs after Hibernate creates the tables. Requires the TRIGGER privilege
-- (and log_bin_trust_function_creators=1 if the binlog is enabled; already set in docker-compose.yml).
DROP TRIGGER IF EXISTS trg_audit_log_no_update;
CREATE TRIGGER trg_audit_log_no_update BEFORE UPDATE ON audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only: UPDATE not allowed';
DROP TRIGGER IF EXISTS trg_audit_log_no_delete;
CREATE TRIGGER trg_audit_log_no_delete BEFORE DELETE ON audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is append-only: DELETE not allowed';
