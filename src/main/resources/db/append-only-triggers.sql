-- RN-19: la bitacora de auditoria es append-only: ni siquiera ADMIN o DEV pueden modificarla o borrarla.
-- Se ejecuta despues de que Hibernate crea las tablas. Requiere el privilegio TRIGGER
-- (y log_bin_trust_function_creators=1 si el binlog esta activo; ya viene en docker-compose.yml).
DROP TRIGGER IF EXISTS trg_audit_log_no_update;
CREATE TRIGGER trg_audit_log_no_update BEFORE UPDATE ON audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log es append-only: UPDATE no permitido';
DROP TRIGGER IF EXISTS trg_audit_log_no_delete;
CREATE TRIGGER trg_audit_log_no_delete BEFORE DELETE ON audit_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log es append-only: DELETE no permitido';
