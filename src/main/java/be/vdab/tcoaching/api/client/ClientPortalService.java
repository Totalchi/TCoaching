package be.vdab.tcoaching.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
class ClientPortalService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ClientPortalMailService clientPortalMailService;
    private final int verificationTokenHours;
    private final int resetTokenHours;

    public ClientPortalService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            ClientPortalMailService clientPortalMailService,
            @Value("${portal.client.verification-token-hours:24}") int verificationTokenHours,
            @Value("${portal.client.reset-token-hours:2}") int resetTokenHours
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.clientPortalMailService = clientPortalMailService;
        this.verificationTokenHours = Math.max(1, verificationTokenHours);
        this.resetTokenHours = Math.max(1, resetTokenHours);
    }

    @Transactional
    public void register(ClientRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        ClientAccount existing = findClientByEmail(email);
        if (existing != null && existing.active()) {
            throw new ResponseStatusException(CONFLICT, "Account already exists");
        }

        long clientId;
        try {
            clientId = insertClient(request, email);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(CONFLICT, "Account already exists", ex);
        }

        String token = createToken(clientId, ClientTokenType.VERIFY_EMAIL, verificationTokenHours);
        scheduleAfterCommit(() -> clientPortalMailService.sendVerificationEmail(
                email,
                safeTrim(request.firstName()),
                normalizeLang(request.lang()),
                token
        ));
    }

    @Transactional
    public boolean verifyEmail(String token) {
        ClientTokenRecord tokenRecord = findValidToken(token, ClientTokenType.VERIFY_EMAIL);
        if (tokenRecord == null) {
            return false;
        }

        jdbcTemplate.update(
                "UPDATE clients SET verified = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                tokenRecord.clientId()
        );
        jdbcTemplate.update("UPDATE client_tokens SET used = TRUE WHERE id = ?", tokenRecord.id());
        jdbcTemplate.update(
                "UPDATE client_tokens SET used = TRUE WHERE client_id = ? AND type = ? AND id <> ?",
                tokenRecord.clientId(),
                ClientTokenType.VERIFY_EMAIL.databaseValue(),
                tokenRecord.id()
        );
        return true;
    }

    @Transactional
    public void requestPasswordReset(ClientResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        ClientAccount account = findClientByEmail(email);
        if (account == null || !account.active() || !account.verified()) {
            // Constant-time dummy operation to prevent email enumeration via timing
            passwordEncoder.encode(email);
            return;
        }

        jdbcTemplate.update(
                "UPDATE client_tokens SET used = TRUE WHERE client_id = ? AND type = ? AND used = FALSE",
                account.id(),
                ClientTokenType.RESET_PASSWORD.databaseValue()
        );

        String token = createToken(account.id(), ClientTokenType.RESET_PASSWORD, resetTokenHours);
        scheduleAfterCommit(() -> clientPortalMailService.sendPasswordResetEmail(
                account.email(),
                account.firstName(),
                account.lang(),
                token
        ));
    }

    @Transactional
    public boolean resetPassword(ClientResetPasswordConfirmRequest request) {
        ClientTokenRecord tokenRecord = findValidToken(request.token(), ClientTokenType.RESET_PASSWORD);
        if (tokenRecord == null) {
            return false;
        }

        jdbcTemplate.update(
                "UPDATE clients SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                passwordEncoder.encode(request.password()),
                tokenRecord.clientId()
        );
        jdbcTemplate.update("UPDATE client_tokens SET used = TRUE WHERE id = ?", tokenRecord.id());
        return true;
    }

    @Transactional(readOnly = true)
    public ClientDashboardResponse getDashboard(ClientPrincipal principal) {
        NextAppointmentResponse nextAppointment = jdbcTemplate.query(
                """
                        SELECT id, title, type, scheduled_at, duration_min, location, status, notes_shared
                        FROM appointments
                        WHERE client_id = ? AND scheduled_at >= ? AND status = 'scheduled'
                        ORDER BY scheduled_at ASC
                        LIMIT 1
                        """,
                (rs, ignored) -> new NextAppointmentResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getTimestamp("scheduled_at").toLocalDateTime(),
                        rs.getInt("duration_min"),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("notes_shared")
                ),
                principal.getClientId(),
                Timestamp.valueOf(LocalDateTime.now())
        ).stream().findFirst().orElse(null);

        int openInvoices = count(
                "SELECT COUNT(*) FROM invoices WHERE client_id = ? AND status IN ('draft', 'sent', 'overdue')",
                principal.getClientId()
        );
        int unreadCoachMessages = count(
                "SELECT COUNT(*) FROM messages WHERE client_id = ? AND sender = 'coach' AND read_at IS NULL",
                principal.getClientId()
        );

        TrainingPlanSummary activePlan = jdbcTemplate.query(
                """
                        SELECT id, title
                        FROM training_plans
                        WHERE client_id = ? AND status IN ('active', 'draft')
                        ORDER BY CASE status WHEN 'active' THEN 0 ELSE 1 END, created_at DESC
                        LIMIT 1
                        """,
                (rs, ignored) -> new TrainingPlanSummary(rs.getLong("id"), rs.getString("title")),
                principal.getClientId()
        ).stream().findFirst().orElse(null);
        int activeTrainingItemCount = activePlan == null
                ? 0
                : count("SELECT COUNT(*) FROM training_items WHERE plan_id = ?", activePlan.id());

        MessageResponse latestMessage = jdbcTemplate.query(
                """
                        SELECT id, sender, body, read_at, created_at
                        FROM messages
                        WHERE client_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                (rs, ignored) -> mapMessage(
                        rs.getLong("id"),
                        rs.getString("sender"),
                        rs.getString("body"),
                        timestampToLocalDateTime(rs.getTimestamp("read_at")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                principal.getClientId()
        ).stream().findFirst().orElse(null);

        return new ClientDashboardResponse(
                principal.getFullName(),
                principal.getLang(),
                nextAppointment,
                openInvoices,
                unreadCoachMessages,
                activePlan == null ? null : activePlan.title(),
                activeTrainingItemCount,
                latestMessage
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(long clientId) {
        return jdbcTemplate.query(
                """
                        SELECT id, title, type, scheduled_at, duration_min, location, status, notes_shared
                        FROM appointments
                        WHERE client_id = ?
                        ORDER BY scheduled_at DESC
                        """,
                (rs, ignored) -> new AppointmentResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("type"),
                        rs.getTimestamp("scheduled_at").toLocalDateTime(),
                        rs.getInt("duration_min"),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("notes_shared")
                ),
                clientId
        );
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse getTrainingPlan(long clientId) {
        TrainingPlanResponse plan = jdbcTemplate.query(
                """
                        SELECT id, title, description, start_date, end_date, status
                        FROM training_plans
                        WHERE client_id = ?
                        ORDER BY CASE status WHEN 'active' THEN 0 WHEN 'draft' THEN 1 ELSE 2 END, created_at DESC
                        LIMIT 1
                        """,
                (rs, ignored) -> new TrainingPlanResponse(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        toLocalDate(rs.getDate("start_date")),
                        toLocalDate(rs.getDate("end_date")),
                        rs.getString("status"),
                        List.of()
                ),
                clientId
        ).stream().findFirst().orElse(null);

        if (plan == null) {
            return null;
        }

        List<TrainingItemResponse> items = jdbcTemplate.query(
                """
                        SELECT id, sort_order, category, title, description, sets, reps, duration_sec, completed_by_client
                        FROM training_items
                        WHERE plan_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                (rs, ignored) -> new TrainingItemResponse(
                        rs.getLong("id"),
                        rs.getInt("sort_order"),
                        rs.getString("category"),
                        rs.getString("title"),
                        rs.getString("description"),
                        getNullableInt(rs, "sets"),
                        getNullableInt(rs, "reps"),
                        getNullableInt(rs, "duration_sec"),
                        rs.getBoolean("completed_by_client")
                ),
                plan.id()
        );

        return new TrainingPlanResponse(
                plan.id(),
                plan.title(),
                plan.description(),
                plan.startDate(),
                plan.endDate(),
                plan.status(),
                items
        );
    }

    @Transactional
    public void updateTrainingItemCompletion(long clientId, long itemId, boolean completed) {
        Integer owned = jdbcTemplate.query(
                """
                        SELECT ti.id
                        FROM training_items ti
                        JOIN training_plans tp ON tp.id = ti.plan_id
                        WHERE ti.id = ? AND tp.client_id = ?
                        """,
                (rs, ignored) -> rs.getInt(1),
                itemId,
                clientId
        ).stream().findFirst().orElse(null);
        if (owned == null) {
            throw new ResponseStatusException(NOT_FOUND, "Training item not found");
        }
        jdbcTemplate.update("UPDATE training_items SET completed_by_client = ? WHERE id = ?", completed, itemId);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(long clientId) {
        return jdbcTemplate.query(
                """
                        SELECT id, invoice_number, description, amount_cents, currency, status, due_date, paid_at, payment_method, pdf_url
                        FROM invoices
                        WHERE client_id = ?
                        ORDER BY created_at DESC
                        """,
                (rs, ignored) -> new InvoiceResponse(
                        rs.getLong("id"),
                        rs.getString("invoice_number"),
                        rs.getString("description"),
                        rs.getInt("amount_cents"),
                        rs.getString("currency"),
                        rs.getString("status"),
                        toLocalDate(rs.getDate("due_date")),
                        timestampToLocalDateTime(rs.getTimestamp("paid_at")),
                        rs.getString("payment_method"),
                        rs.getString("pdf_url")
                ),
                clientId
        );
    }

    @Transactional
    public List<MessageResponse> getMessages(long clientId) {
        jdbcTemplate.update(
                "UPDATE messages SET read_at = CURRENT_TIMESTAMP WHERE client_id = ? AND sender = 'coach' AND read_at IS NULL",
                clientId
        );
        return jdbcTemplate.query(
                """
                        SELECT id, sender, body, read_at, created_at
                        FROM messages
                        WHERE client_id = ?
                        ORDER BY created_at ASC
                        """,
                (rs, ignored) -> mapMessage(
                        rs.getLong("id"),
                        rs.getString("sender"),
                        rs.getString("body"),
                        timestampToLocalDateTime(rs.getTimestamp("read_at")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                clientId
        );
    }

    @Transactional
    public MessageResponse addClientMessage(long clientId, ClientMessageRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO messages (client_id, sender, body, created_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, clientId);
            statement.setString(2, "client");
            statement.setString(3, safeTrim(request.body()));
            statement.setTimestamp(4, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Message insert failed");
        }
        return new MessageResponse(key.longValue(), "client", safeTrim(request.body()), null, now);
    }

    ClientAuthResponse toAuthResponse(ClientPrincipal principal) {
        return new ClientAuthResponse(
                principal.getClientId(),
                principal.getEmail(),
                principal.getFirstName(),
                principal.getLastName(),
                principal.getFullName(),
                principal.getLang()
        );
    }

    private long insertClient(ClientRegistrationRequest request, String email) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO clients (email, password_hash, first_name, last_name, phone, lang, verified, active, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, email);
            statement.setString(2, passwordEncoder.encode(request.password()));
            statement.setString(3, safeTrim(request.firstName()));
            statement.setString(4, nullableText(request.lastName()));
            statement.setString(5, nullableText(request.phone()));
            statement.setString(6, normalizeLang(request.lang()));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Client insert failed");
        }
        return key.longValue();
    }

    private String createToken(long clientId, ClientTokenType type, int expiresAfterHours) {
        String token = generateToken();
        jdbcTemplate.update(
                """
                        INSERT INTO client_tokens (client_id, token, type, expires_at, used, created_at)
                        VALUES (?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)
                        """,
                clientId,
                token,
                type.databaseValue(),
                Timestamp.valueOf(LocalDateTime.now().plusHours(expiresAfterHours))
        );
        return token;
    }

    private ClientTokenRecord findValidToken(String token, ClientTokenType type) {
        if (token == null || !token.matches("[a-f0-9]{64}")) {
            return null;
        }
        return jdbcTemplate.query(
                """
                        SELECT id, client_id, expires_at, used
                        FROM client_tokens
                        WHERE token = ? AND type = ?
                        """,
                (rs, ignored) -> new ClientTokenRecord(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        rs.getBoolean("used")
                ),
                token,
                type.databaseValue()
        ).stream().filter(candidate -> !candidate.used() && candidate.expiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .orElse(null);
    }

    private ClientAccount findClientByEmail(String email) {
        return jdbcTemplate.query(
                """
                        SELECT id, email, first_name, last_name, lang, verified, active
                        FROM clients
                        WHERE LOWER(email) = ?
                        """,
                (rs, ignored) -> new ClientAccount(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("lang"),
                        rs.getBoolean("verified"),
                        rs.getBoolean("active")
                ),
                email
        ).stream().findFirst().orElse(null);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private MessageResponse mapMessage(long id, String sender, String body, LocalDateTime readAt, LocalDateTime createdAt) {
        return new MessageResponse(id, sender, body, readAt, createdAt);
    }

    private Integer getNullableInt(java.sql.ResultSet rs, String columnLabel) throws java.sql.SQLException {
        int value = rs.getInt(columnLabel);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDate toLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void scheduleAfterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLang(String lang) {
        return "en".equalsIgnoreCase(lang) ? "en" : "nl";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullableText(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private record ClientAccount(
            long id,
            String email,
            String firstName,
            String lastName,
            String lang,
            boolean verified,
            boolean active
    ) {
    }

    private record ClientTokenRecord(long id, long clientId, LocalDateTime expiresAt, boolean used) {
    }

    private record TrainingPlanSummary(long id, String title) {
    }
}
