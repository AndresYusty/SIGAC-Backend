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
| Tipo de consejo    | CouncilType / CouncilKind     |
| Invitado           | SessionGuest / guest          |
| Punto urgente      | urgent item                   |
| Universidad        | University                    |
| Sede               | Campus                        |
| Facultad           | Faculty                       |
| Programa           | Program                       |
| Jerarquía          | hierarchy                     |
| Parámetro          | Parameter                     |
| Repositorio        | repository                    |
| Auditoría          | audit                         |
| Usuario            | User                          |
| Acuerdo            | Agreement                     |
| Tipo de acuerdo    | AgreementType / typeCode      |
| Considerando       | recitals                      |
| Artículos (acuerda)| articles                      |
| Epígrafe           | subject                       |
| Hoja membreteada   | letterhead sheet              |

## Enums (persisted as strings)

- `MinutesStatus`: `DRAFT`, `EDITING`, `PENDING_SIGNATURE`, `PUBLISHED`
- `SessionStatus`: `SCHEDULED`, `CONVENED`, `STARTED`, `SUSPENDED`, `FINISHED` (method `canTransitionTo`)
- `RequestStatus`: `FILED`, `RETURNED`, `REJECTED`, `APPROVED`, `ON_AGENDA`, `PROCESSED`
- `RequestType`: `ACADEMIC`, `ADMINISTRATIVE`, `STUDENT`, `TEACHING`
- `DecisionOutcome`: `APPROVED`, `DENIED`, `POSTPONED`, `CANCELLED` (method `label()` returns English label)
- `RoleName`: `ROLE_ADMIN`, `ROLE_COUNCIL_SECRETARY`, `ROLE_FACULTY_SECRETARY`, `ROLE_PRESIDENT`, `ROLE_COUNCIL_MEMBER`
  (`VOTING` list, `isGlobal()`). `@PreAuthorize` uses `hasRole('COUNCIL_SECRETARY')`, etc.
  Administrator (`ROLE_ADMIN`): users, hierarchy, settings, templates and integrity alerts. Council secretary
  (`ROLE_COUNCIL_SECRETARY`): sessions, agenda, live session and minutes. Faculty secretary
  (`ROLE_FACULTY_SECRETARY`): files requests. The president (dean) also acts as a voting member. A council member
  only sees the published minutes of the sessions they attended.
- `CouncilKind`: `ORDINARY` (`CO`), `EXTRAORDINARY` (`CE`, urgent items), `VIRTUAL` (`CV`, no location and no
  quorum), `EXPANDED` (`CAM`, guests with voice but no vote). Quorum = half + 1 for every kind except `VIRTUAL`.

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
| `Agreement`       | `agreements`           | faculty→`faculty_id`, typeCode, number, year→`agreement_year`, issueDate, subject, councilTitle, legalBasis, closing, presidentName, secretaryName, content (JSON `{values, recitals, articles}`), createdBy, createdAt, updatedAt (unique faculty_id+agreement_year+number) |
| `MinutesTemplate` | `minutes_templates`    | name, councilType→`council_type_id`, faculty→`faculty_id`, version, content, active, comment, createdBy, createdAt                                                                                                                                                                    |
| `AgendaItem`      | `agenda_items`         | session→`session_id`, position, title, description, request→`request_id`, miscellaneous, urgent, outcome, votesFor, votesAgainst, abstentions, decidedBy, debateNotes                                                                                                                        |
| `Session`         | `sessions`             | councilType→`council_type_id`, faculty→`faculty_id`, scheduledAt, location (null if virtual), status, agendaClosed, actualStart, actualEnd, items, guests, createdAt                                                                                                                                          |
| `CouncilRequest`  | `council_requests`     | code, title, description, requester, requestType, faculty→`faculty_id`, program→`program_id`, filedBy→`filed_by_id`, status, filedAt, attachments                                                                                                                                    |
| `CouncilType`     | `council_types`        | code, name, kind (null for legacy types)                                                                                                                                                                                                                                                |
| `SessionGuest`    | `session_guests`       | session→`session_id`, name, email, affiliation                                                                                                                                                                                                                                        |
| `User`            | `users`                | name, email, passwordHash, active, faculty→`faculty_id`, program→`program_id`, roles (`user_roles`: `user_id`, `role`), lastLogin                                                                                                                                                    |

Columns use snake_case of the Java field (e.g. `serial_number`, `html_content`, `votes_for`).
Audit trigger names stay `trg_audit_log_no_update` / `trg_audit_log_no_delete` with English messages.

Code formats: minutes serial number `MIN-<councilCode>-<year>-<nnn>` (counter key `MIN-<code>-<year>`),
request code `REQ-<year>-<nnnnnn>` (counter key `REQ-<year>`). Global parameter key: `quorum.percentage`.
Storage folders: `minutes/…`, `requests/…`.

Audit entity names: `MINUTES`, `SESSION`, `AGENDA_ITEM`, `TEMPLATE`, `REQUEST`, `ATTACHMENT`, `USER`, `AGREEMENT`.
Audit events: `EVENT_MINUTES_COMPILED`, `EVENT_MINUTES_EDITED`, `EVENT_MINUTES_SENT_TO_SIGN`,
`EVENT_MINUTES_REJECTED`, `EVENT_MINUTES_SIGNED`, `EVENT_AGREEMENT_CREATED`, `EVENT_AGREEMENT_EDITED`, `EVENT_AGENDA_ITEM_ADDED`, `EVENT_MISC_ITEM_ADDED`, `EVENT_URGENT_ITEM_ADDED`, `EVENT_SESSION_GUEST_ADDED`;
all other existing `EVENT_*` names keep their current (already English) value.

## REST endpoints

Auth (`/auth`): `POST /login`, `POST /forgot-password`, `POST /reset-password`.

Users (`/users`): `GET /me`, `PUT /me/password`, `GET ?q&page&size`, `GET /{id}`, `POST`, `PUT /{id}`,
`PATCH /{id}/status?active=true|false`.

Hierarchy (`/hierarchy`): `GET|POST /universities`, `PUT /universities/{id}`,
`GET /campuses?universityId`, `POST /campuses`, `PUT /campuses/{id}`,
`GET /faculties?campusId`, `POST /faculties`, `PUT /faculties/{id}`,
`GET /programs?facultyId`, `POST /programs`, `PUT /programs/{id}`.

Configuration: `GET /council-types`, `PUT /council-types/{id}` (rename only), `GET /parameters`, `PUT /parameters/{key}`.

Templates (`/templates`, DEV only): `GET ?councilTypeId`, `GET /variables`, `GET /{id}`, `POST /validate`,
`POST`, `PUT /{id}/activate`, `POST /preview`.

Requests (`/requests`): `POST` (multipart: form fields + files part `attachments`), `PUT /{id}` (multipart),
`GET ?status&code&from&to&page&size`, `GET /{id}`, `GET /{id}/history`,
`GET /{id}/attachments/{attachmentId}/download`, `POST /{id}/approve`, `POST /{id}/reject`, `POST /{id}/return`.

Sessions (`/sessions`): `POST`, `GET ?status&page&size`, `GET /{id}`, `POST /{id}/items`,
`POST /{id}/urgent-items`, `POST /{id}/guests`, `DELETE /{id}/guests/{guestId}`, `DELETE /{id}/items/{itemId}`, `PUT /{id}/items/order`, `POST /{id}/close-agenda`, `GET /{id}/notice` (PDF).

Live session (`/sessions/{sessionId}`): `GET|PUT /attendance`, `POST /start`, `POST /suspend`, `POST /resume`,
`POST /finish`, `POST /items/{itemId}/decision`, `POST /items/{itemId}/notes`, `POST /misc-items`.

Minutes (`/minutes`): `POST /session/{sessionId}`, `GET ?status&page&size`, `GET /{id}`, `GET /{id}/preview` (PDF),
`PUT /{id}/content`, `POST /{id}/send-to-signature`, `POST /{id}/reject`, `POST /{id}/approve-and-sign`.

Agreements (`/agreements`): `GET /types`, `GET /defaults`, `GET ?year&typeCode&text&page&size`, `GET /{id}`, `POST`,
`PUT /{id}`, `POST /preview` (PDF), `GET /{id}/pdf?inline=false`.

Letterhead (`/letterhead`): `GET` (`{customSheet, logo}`), `GET /sheet`, `GET /logo`, `PUT /sheet` and `PUT /logo`
(multipart `file`, PNG/JPG up to 5 MB, ADMIN), `DELETE /sheet`, `DELETE /logo` (ADMIN).

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
`FacultyRequest{name,campusId}`, `ProgramRequest{name,facultyId}`, `CouncilTypeDto{id,code,name,kind}`,
`CouncilTypeRequest{name}`, `ParameterDto{key,value,description}`, `ParameterRequest{value}`.

Templates: `TemplateDto{id,name,councilTypeId,councilTypeName,facultyId,facultyName,version,active,comment,createdBy,createdAt,content}`,
`SaveTemplateRequest{councilTypeId,facultyId,name,content,comment}`, `TemplateContentRequest{content}`,
`VariableDto{name,type,description,example}` (type is `text` or `list`).

Template variables (Thymeleaf): `serialNumber`, `sessionDate`, `startTime`, `endTime`, `location`, `councilType`,
`university`, `campus`, `faculty`, `quorum`, `attendees` (items `{name,role,present}`), `guests` (items `{name,affiliation}`), `hasGuests`,
`agenda` (items `{number,title,description,urgent}`),
`agreements` (items `{number,title,description,decision,votesFor,votesAgainst,abstentions,notes,miscellaneous,urgent}`).

Requests: `CouncilRequestForm{title,description,requester,requestType,programId}`,
`AttachmentDto{id,name,sizeBytes,sha256Hash}`,
`CouncilRequestDto{id,code,title,description,requester,requestType,status,facultyId,faculty,programId,program,filedBy,filedAt,attachments[]}`,
`HistoryDto{previousStatus,newStatus,reason,user,occurredAt}`, `ReasonRequest{reason}`.

Sessions: `SessionRequest{councilTypeId,scheduledAt,location}`, `AddItemRequest{requestId}`, `ReorderRequest{itemIds[]}`,
`AttendanceItem{userId,present}`, `AttendanceRequest{items[]}`,
`DecisionRequest{outcome,votesFor,votesAgainst,abstentions,changeReason}`, `NoteRequest{content}`,
`MiscItemRequest{title,description}`, `UrgentItemRequest{title,description}`, `GuestRequest{name,email,affiliation}`,
`SessionSummaryDto{id,councilType,councilKind,faculty,scheduledAt,location,status,agendaClosed,totalItems}`,
`AgendaItemDto{id,position,title,description,requestCode,miscellaneous,urgent,outcome,votesFor,votesAgainst,abstentions,decidedBy,debateNotes}`,
`SessionDto{id,councilTypeId,councilType,councilKind,facultyId,faculty,scheduledAt,location,status,agendaClosed,actualStart,actualEnd,items[],guests[]}`, `GuestDto{id,name,email,affiliation}`,
`QuorumDto{members,present,required,percentage,reached,applicable}`, `AttendeeDto{userId,name,role,present}`,
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

Modules mirror the backend: `src/app/modules/{users,templates,requests,session,minutes,agreements,repository}`, each
with `<module>.api.ts` (`UsersApi`, `TemplatesApi`, `RequestsApi`, `LiveSessionApi`, `MinutesApi`,
`RepositoryApi`), `<module>.models.ts`, `<module>.routes.ts`, `index.ts` and `pages/`.

Routes: `/login`, `/forgot-password`, `/reset-password`, `/dashboard`, `/requests`, `/requests/new`,
`/requests/:id`, `/requests/:id/edit`, `/sessions`, `/sessions/new`, `/sessions/:id`, `/sessions/:id/live`,
`/minutes`, `/minutes/:id`, `/agreements`, `/agreements/new`, `/agreements/:id`, `/repository`, `/users`, `/hierarchy`, `/settings`, `/templates`, `/audit`, `/profile`.

UI text lives in `src/app/i18n/en.json` and `src/app/i18n/es.json` (same keys), served by `I18nService`
and the `t` pipe; the language switch (EN/ES) is in the top bar and on the login screen, persisted in
`localStorage` (`sigac.lang`), default `en`.

## Demo seed data

Users: `admin@sigac.local` / `Admin12345*`.
Demo hierarchy/catalog names are proper nouns and may stay as institution names
(e.g. "Universidad Surcolombiana"); descriptive demo text is English.

## Additions (names not covered above, chosen during the backend translation)

- Global parameter key `attachments.max_mb` (was `archivos.max_mb`): maximum size per attached PDF, in MB.
- JWT claims: `uid`, `name` (was `nombre`), `roles`, `fac`, `prog`.
- Notice template variables (`/sigac/templates/notice.html`): `university`, `campus`, `faculty`, `councilType`,
  `sessionDate`, `sessionTime`, `location`, `agenda` (items `{number,title,description,urgent}`), `guests`, `hasGuests`.
- Classpath templates: `/sigac/templates/minutes-default.html`, `/sigac/templates/notice.html`,
  `/sigac/templates/agreement.html`; agreement types in `/sigac/agreements/types.json`.
- Storage folder `branding/` (`sheet`, `logo`): letterhead uploaded by the administrator.
- Storage folder `notices/<sessionId>/notice.pdf` (was `citaciones/…`).
- Password reset email link: `<frontendUrl>/reset-password?token=<token>`.
- `AttendeeDto.role` is a `RoleName` (`ROLE_PRESIDENT` / `ROLE_COUNCIL_MEMBER`); template `attendees[].role` labels: `President`, `Council member`.
- Audit entity `AGENDA_ITEM` is used for `EVENT_DECISION_REGISTERED` / `EVENT_DECISION_MODIFIED`.
- Extra internal records: `CouncilRequestDtos.FileContent{name,data}`, `RepositoryDtos.MinutesFile{name,data,hash}`.
- Response header on repository PDF delivery: `X-SIGAC-SHA256`.
