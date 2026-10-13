# SIGAC — English naming glossary and API contract

All code, identifiers, database objects, API paths, JSON fields, log/error messages and documentation
are written in English. This document is the single source of truth shared by the backend
(`sigac-backend/sigac-v1`) and the frontend (`sigac-frontend`).

Base package: `com.university.sigac` (was `com.universidad.sigac`). Base API path: `/api/v1`.
Modular monolith packages (see README): `core.*`, `config`, `modules.users`, `modules.templates`, `modules.requests`,
`modules.session`, `modules.minutes`, `modules.repository`, `modules.notifications`.

## Domain vocabulary

| Spanish (legacy)   | English                       |
|--------------------|-------------------------------|
| Acta               | Minutes                       |
| Consecutivo (acta) | serialNumber                  |
| Plantilla de acta  | MinutesTemplate / template    |
| Sesión             | Session                       |
| Punto de agenda    | AgendaItem / item             |
| Punto varios       | miscellaneous item            |
| Orden del día      | agenda                        |
| Citación           | notice                        |
| Asistencia         | Attendance                    |
| Solicitud          | CouncilRequest / request      |
| Radicar / radicador| file / filedBy                |
| Anexo              | Attachment                    |
| Historial          | history                       |
| Tipo de consejo    | CouncilType                   |
| Universidad        | University                    |
| Sede               | Campus                        |
| Facultad           | Faculty                       |
| Programa           | Program                       |
| Jerarquía          | hierarchy                     |
| Parámetro          | Parameter                     |
| Repositorio        | repository                    |
| Auditoría          | audit                         |
| Usuario            | User                          |

## Enums (persisted as strings)

- `MinutesStatus`: `DRAFT`, `EDITING`, `PENDING_SIGNATURE`, `PUBLISHED`
- `SessionStatus`: `SCHEDULED`, `CONVENED`, `STARTED`, `SUSPENDED`, `FINISHED` (method `canTransitionTo`)
- `RequestStatus`: `FILED`, `RETURNED`, `REJECTED`, `APPROVED`, `ON_AGENDA`, `PROCESSED`
- `RequestType`: `ACADEMIC`, `ADMINISTRATIVE`, `STUDENT`, `TEACHING`
- `DecisionOutcome`: `APPROVED`, `DENIED`, `POSTPONED`, `CANCELLED` (method `label()` returns English label)
- `RoleName`: `ROLE_DEV`, `ROLE_ADMIN`, `ROLE_SECRETARY_1`, `ROLE_SECRETARY_2`, `ROLE_PRESIDENT`, `ROLE_COUNCIL_MEMBER`
  (`VOTING` list, `isGlobal()`). `@PreAuthorize` uses `hasRole('SECRETARY_1')`, etc.

## Entities and tables

| Entity            | Table                  | Fields (Java → column)                                                                                                                                                                                                                                                                  |
|-------------------|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Minutes`         | `minutes`              | session→`session_id`, serialNumber, councilType→`council_type_id`, faculty→`faculty_id`, sessionDate, templateVersion, htmlContent, status, rejectionNote, signedBy, signerRole, signedAt, signatureIp, contentHash, filePath, fileHash, createdAt                                  |
| `Attachment`      | `attachments`          | request→`request_id`, originalName, path, sha256Hash, sizeBytes                                                                                                                                                                                                                       |
| `Attendance`      | `attendances`          | session→`session_id`, user→`user_id`, present (unique session_id+user_id)                                                                                                                                                                                                              |
| `AuditLog`        | `audit_log`            | event, userId, userEmail, ip, entity, entityId, detail, occurredAt                                                                                                                                                                                                                     |
| `SequenceCounter` | `sequence_counters`    | counterKey, lastValue                                                                                                                                                                                                                                                                   |
| `University`      | `universities`         | name                                                                                                                                                                                                                                                                                    |
| `Campus`          | `campuses`             | name, city, university→`university_id`                                                                                                                                                                                                                                                  |
| `Faculty`         | `faculties`            | name, campus→`campus_id`                                                                                                                                                                                                                                                                |
| `Program`         | `programs`             | name, faculty→`faculty_id`                                                                                                                                                                                                                                                              |
| `RequestHistory`  | `request_history`      | request→`request_id`, previousStatus, newStatus, reason, userName, occurredAt                                                                                                                                                                                                         |
| `Parameter`       | `parameters`           | paramKey (PK), paramValue, description                                                                                                                                                                                                                                                  |
| `PasswordResetToken` | `password_reset_tokens` | user→`user_id`, tokenHash, expiresAt, used                                                                                                                                                                                                                                           |
| `MinutesTemplate` | `minutes_templates`    | name, councilType→`council_type_id`, faculty→`faculty_id`, version, content, active, comment, createdBy, createdAt                                                                                                                                                                    |
| `AgendaItem`      | `agenda_items`         | session→`session_id`, position, title, description, request→`request_id`, miscellaneous, outcome, votesFor, votesAgainst, abstentions, decidedBy, debateNotes                                                                                                                        |
| `Session`         | `sessions`             | councilType→`council_type_id`, faculty→`faculty_id`, scheduledAt, location, status, agendaClosed, actualStart, actualEnd, items, createdAt                                                                                                                                            |
| `CouncilRequest`  | `council_requests`     | code, title, description, requester, requestType, faculty→`faculty_id`, program→`program_id`, filedBy→`filed_by_id`, status, filedAt, attachments                                                                                                                                    |
| `CouncilType`     | `council_types`        | code, name                                                                                                                                                                                                                                                                              |
| `User`            | `users`                | name, email, passwordHash, active, faculty→`faculty_id`, program→`program_id`, roles (`user_roles`: `user_id`, `role`), lastLogin                                                                                                                                                    |

Columns use snake_case of the Java field (e.g. `serial_number`, `html_content`, `votes_for`).
Audit trigger names stay `trg_audit_log_no_update` / `trg_audit_log_no_delete` with English messages.

Code formats: minutes serial number `MIN-<councilCode>-<year>-<nnn>` (counter key `MIN-<code>-<year>`),
request code `REQ-<year>-<nnnnnn>` (counter key `REQ-<year>`). Global parameter key: `quorum.percentage`.
Storage folders: `minutes/…`, `requests/…`.

Audit entity names: `MINUTES`, `SESSION`, `AGENDA_ITEM`, `TEMPLATE`, `REQUEST`, `ATTACHMENT`, `USER`.
Audit events: `EVENT_MINUTES_COMPILED`, `EVENT_MINUTES_EDITED`, `EVENT_MINUTES_SENT_TO_SIGN`,
`EVENT_MINUTES_REJECTED`, `EVENT_MINUTES_SIGNED`, `EVENT_AGENDA_ITEM_ADDED`, `EVENT_MISC_ITEM_ADDED`;
all other existing `EVENT_*` names keep their current (already English) value.

## REST endpoints

Auth (`/auth`): `POST /login`, `POST /forgot-password`, `POST /reset-password`.

Users (`/users`): `GET /me`, `PUT /me/password`, `GET ?q&page&size`, `GET /{id}`, `POST`, `PUT /{id}`,
`PATCH /{id}/status?active=true|false`.

Hierarchy (`/hierarchy`): `GET|POST /universities`, `PUT /universities/{id}`,
`GET /campuses?universityId`, `POST /campuses`, `PUT /campuses/{id}`,
`GET /faculties?campusId`, `POST /faculties`, `PUT /faculties/{id}`,
`GET /programs?facultyId`, `POST /programs`, `PUT /programs/{id}`.

Configuration: `GET|POST /council-types`, `PUT /council-types/{id}`, `GET /parameters`, `PUT /parameters/{key}`.

Templates (`/templates`, DEV only): `GET ?councilTypeId`, `GET /variables`, `GET /{id}`, `POST /validate`,
`POST`, `PUT /{id}/activate`, `POST /preview`.

Requests (`/requests`): `POST` (multipart: form fields + files part `attachments`), `PUT /{id}` (multipart),
`GET ?status&code&from&to&page&size`, `GET /{id}`, `GET /{id}/history`,
`GET /{id}/attachments/{attachmentId}/download`, `POST /{id}/approve`, `POST /{id}/reject`, `POST /{id}/return`.

Sessions (`/sessions`): `POST`, `GET ?status&page&size`, `GET /{id}`, `POST /{id}/items`,
`DELETE /{id}/items/{itemId}`, `PUT /{id}/items/order`, `POST /{id}/close-agenda`, `GET /{id}/notice` (PDF).

Live session (`/sessions/{sessionId}`): `GET|PUT /attendance`, `POST /start`, `POST /suspend`, `POST /resume`,
`POST /finish`, `POST /items/{itemId}/decision`, `POST /items/{itemId}/notes`, `POST /misc-items`.

Minutes (`/minutes`): `POST /session/{sessionId}`, `GET ?status&page&size`, `GET /{id}`, `GET /{id}/preview` (PDF),
`PUT /{id}/content`, `POST /{id}/send-to-signature`, `POST /{id}/reject`, `POST /{id}/approve-and-sign`.

Repository & audit: `GET /repository/minutes?serialNumber&councilTypeId&from&to&campusId&facultyId&text&page&size`,
`GET /repository/minutes/{id}/verify`, `GET /repository/minutes/{id}/viewer` (PDF),
`GET /repository/minutes/{id}/download`, `GET /audit?event&user&from&to&page&size`, `GET /dashboard`.

## DTOs (JSON field names)

Auth: `LoginRequest{email,password}`, `LoginResponse{token,type,expiresAt,user:UserDto}`,
`ForgotPasswordRequest{email}`, `ResetPasswordRequest{token,newPassword}`, `MessageResponse{message}`.

Users: `UserDto{id,name,email,active,roles[],facultyId,facultyName,programId,programName,lastLogin}`,
`UserCreateRequest{name,email,password,roles[],facultyId,programId}`,
`UserUpdateRequest{name,roles[],facultyId,programId}`, `ChangePasswordRequest{currentPassword,newPassword}`.

Configuration: `NodeDto{id,name,parentId,city}`, `UniversityRequest{name}`, `CampusRequest{name,city,universityId}`,
`FacultyRequest{name,campusId}`, `ProgramRequest{name,facultyId}`, `CouncilTypeDto{id,code,name}`,
`CouncilTypeRequest{code,name}`, `ParameterDto{key,value,description}`, `ParameterRequest{value}`.

Templates: `TemplateDto{id,name,councilTypeId,councilTypeName,facultyId,facultyName,version,active,comment,createdBy,createdAt,content}`,
`SaveTemplateRequest{councilTypeId,facultyId,name,content,comment}`, `TemplateContentRequest{content}`,
`VariableDto{name,type,description,example}` (type is `text` or `list`).

Template variables (Thymeleaf): `serialNumber`, `sessionDate`, `startTime`, `endTime`, `location`, `councilType`,
`university`, `campus`, `faculty`, `quorum`, `attendees` (items `{name,role,present}`),
`agenda` (items `{number,title,description}`),
`agreements` (items `{number,title,description,decision,votesFor,votesAgainst,abstentions,notes,miscellaneous}`).

Requests: `CouncilRequestForm{title,description,requester,requestType,programId}`,
`AttachmentDto{id,name,sizeBytes,sha256Hash}`,
`CouncilRequestDto{id,code,title,description,requester,requestType,status,facultyId,faculty,programId,program,filedBy,filedAt,attachments[]}`,
`HistoryDto{previousStatus,newStatus,reason,user,occurredAt}`, `ReasonRequest{reason}`.

Sessions: `SessionRequest{councilTypeId,scheduledAt,location}`, `AddItemRequest{requestId}`, `ReorderRequest{itemIds[]}`,
`AttendanceItem{userId,present}`, `AttendanceRequest{items[]}`,
`DecisionRequest{outcome,votesFor,votesAgainst,abstentions,changeReason}`, `NoteRequest{content}`,
`MiscItemRequest{title,description}`,
`SessionSummaryDto{id,councilType,faculty,scheduledAt,location,status,agendaClosed,totalItems}`,
`AgendaItemDto{id,position,title,description,requestCode,miscellaneous,outcome,votesFor,votesAgainst,abstentions,decidedBy,debateNotes}`,
`SessionDto{id,councilTypeId,councilType,facultyId,faculty,scheduledAt,location,status,agendaClosed,actualStart,actualEnd,items[]}`,
`QuorumDto{members,present,required,percentage,reached}`, `AttendeeDto{userId,name,role,present}`,
`AttendanceDto{sessionStatus,quorum,attendees[]}`.

Minutes: `MinutesDto{id,serialNumber,status,sessionId,councilType,faculty,sessionDate,rejectionNote,signedBy,signedAt,htmlContent}`,
`MinutesContentRequest{htmlContent}`, `RejectionRequest{note}`,
`RepositoryMinutesDto{id,serialNumber,councilType,university,campus,faculty,sessionDate,signedBy,signedAt,sha256Hash}`,
`VerificationDto{serialNumber,storedHash,computedHash,intact}`,
`AuditLogDto{id,event,userEmail,ip,entity,entityId,detail,occurredAt}`, `DashboardDto{user,indicators}`.

Dashboard `indicators` keys: `myRequestsByStatus` (map RequestStatus→count), `requestsPendingReview`,
`approvedRequestsWithoutAgenda`, `upcomingSessions`, `minutesInProgress`, `minutesPendingSignature`,
`finishedSessions`, `latestPublishedMinutes` (items `{id,serialNumber,sessionDate}`), `activeUsers`,
`inactiveUsers`, `publishedMinutesTotal`.

Error body: `ApiError{timestamp,status,error,message,path,details[]}`. Error messages are English.

## Frontend conventions

Routes: `/login`, `/forgot-password`, `/reset-password`, `/dashboard`, `/requests`, `/requests/new`,
`/requests/:id`, `/requests/:id/edit`, `/sessions`, `/sessions/new`, `/sessions/:id`, `/sessions/:id/live`,
`/minutes`, `/minutes/:id`, `/repository`, `/users`, `/hierarchy`, `/settings`, `/templates`, `/audit`, `/profile`.

UI text lives in `src/app/i18n/en.json` and `src/app/i18n/es.json` (same keys), served by `I18nService`
and the `t` pipe; the language switch (EN/ES) is in the top bar and on the login screen, persisted in
`localStorage` (`sigac.lang`), default `en`.

## Demo seed data

Users: `admin@sigac.local` / `Admin12345*`, `dev@sigac.local` / `Dev12345*` (unchanged).
Demo hierarchy/catalog names are proper nouns and may stay as institution names
(e.g. "Universidad Surcolombiana"); descriptive demo text is English.

## Additions (names not covered above, chosen during the backend translation)

- Global parameter key `attachments.max_mb` (was `archivos.max_mb`): maximum size per attached PDF, in MB.
- JWT claims: `uid`, `name` (was `nombre`), `roles`, `fac`, `prog`.
- Notice template variables (`/sigac/templates/notice.html`): `university`, `campus`, `faculty`, `councilType`,
  `sessionDate`, `sessionTime`, `location`, `agenda` (items `{number,title,description}`).
- Classpath templates: `/sigac/templates/minutes-default.html`, `/sigac/templates/notice.html`.
- Storage folder `notices/<sessionId>/notice.pdf` (was `citaciones/…`).
- Password reset email link: `<frontendUrl>/reset-password?token=<token>`.
- Attendee role labels (`AttendeeDto.role`, template `attendees[].role`): `President`, `Council member`.
- Audit entity `AGENDA_ITEM` is used for `EVENT_DECISION_REGISTERED` / `EVENT_DECISION_MODIFIED`.
- Extra internal records: `CouncilRequestDtos.FileContent{name,data}`, `RepositoryDtos.MinutesFile{name,data,hash}`.
- Response header on repository PDF delivery: `X-SIGAC-SHA256`.
