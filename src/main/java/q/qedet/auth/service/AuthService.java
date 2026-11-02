package q.qedet.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.dto.AuthRequest;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.dto.GoogleIdentityClaims;
import q.qedet.auth.dto.RegistrationResponse;
import q.qedet.auth.dto.VerifyEmailRequest;
import q.qedet.auth.dto.VerifyEmailResponse;
import q.qedet.auth.entity.EmailVerificationToken;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.EmailVerificationTokenRepository;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.auth.security.TokenHasher;
import q.qedet.auth.security.TokenService;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.ConflictException;
import q.qedet.common.exception.UnauthorizedException;
import q.qedet.common.model.EmployeeStatus;
import q.qedet.common.model.RegistrationStatus;
import q.qedet.common.model.UserStatus;
import q.qedet.common.response.ResponseMapper;
import q.qedet.common.usage.UsageTracker;
import q.qedet.email.EmailService;
import q.qedet.employee.entity.Employee;
import q.qedet.employee.repository.EmployeeRepository;
import q.qedet.organization.dto.RegistrationRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final InvitationService invitationService;
    private final UsageTracker usageTracker;
    private final PasswordPolicy passwordPolicy;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);

    public AuthService(UserAccountRepository userAccountRepository,
                       EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       InvitationService invitationService,
                       UsageTracker usageTracker,
                       PasswordPolicy passwordPolicy,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.invitationService = invitationService;
        this.usageTracker = usageTracker;
        this.passwordPolicy = passwordPolicy;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.emailService = emailService;
    }

    private static final Logger log =
            LoggerFactory.getLogger(AuthService.class);

    @Transactional
    public RegistrationResponse registerUser(RegistrationRequest request) {
        passwordPolicy.validate(request.password());
        String email = request.email().trim().toLowerCase();
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException(
                    "ACCOUNT_EXISTS",
                    "An account with this email address is already registered. Please sign in."
            );
        }

        UserAccount user = new UserAccount(email, passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        updateRegistrationDraft(user, request);
        user = userAccountRepository.save(user);
        sendVerificationCode(user);
        return new RegistrationResponse(
                true,
                "Account created successfully.",
                new RegistrationResponse.Data(
                        user.getId(),
                        user.getEmail(),
                        "VERIFY_EMAIL"
                )
        );
    }

    private void updateRegistrationDraft(UserAccount user, RegistrationRequest request) {
        user.setRegistrationName(request.name().trim());
        user.setRegistrationPhone(request.phone());
        user.setRegistrationCompanyName(request.companyName());
        user.setRegistrationCountry(request.country());
        user.setRegistrationTimezone(request.timezone());
        if (!user.isEmailVerified()) {
            user.setRegistrationStatus(RegistrationStatus.PENDING_EMAIL_VERIFICATION);
        }
    }

    private void sendVerificationCode(UserAccount user) {




        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String hash = TokenHasher.sha256(code);
        emailVerificationTokenRepository.save(new EmailVerificationToken(
                user, hash, java.time.Instant.now().plus(VERIFICATION_TTL)));

        log.debug("Verification email: {}", user.getEmail());
        emailService.sendVerificationCodeEmail(user.getEmail(), code);
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String code = request.code().trim();
        String codeHash = TokenHasher.sha256(code);



        String email = request.email().trim().toLowerCase();

        log.info("Verification email: {}", email);

        EmailVerificationToken token = emailVerificationTokenRepository
                .findByCodeHash(codeHash)
                .filter(candidate -> candidate.getUser().getEmail().equalsIgnoreCase(email))
                .orElseThrow(() -> new BadRequestException(" Invalid  verification code."));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(java.time.Instant.now())) {
            throw new BadRequestException("  verification code has expired.");
        }
        UserAccount user = token.getUser();
        user.setEmailVerified(true);
        user.setRegistrationStatus(RegistrationStatus.COMPLETED);
        userAccountRepository.save(user);
        token.setUsedAt(java.time.Instant.now());
        emailVerificationTokenRepository.save(token);
        return new VerifyEmailResponse(
                "EMAIL_VERIFIED",
                "Email verified successfully.",
                user.getRegistrationStatus().name()
        );
    }

    @Transactional
    public void resendVerificationCode(String email) {
        userAccountRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationCode);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            sendVerificationCode(user);
            return new AuthResponse(
                    null,
                    null,
                    user.getId(),
                    null,
                    null,
                    null,
                    java.util.Set.of(),
                    user.getRegistrationStatus(),
                    "VERIFICATION_REQUIRED",
                    "EMAIL_VERIFICATION",
                    "Please verify your email to continue.",
                    firstName(user, null),
                    lastName(user, null),
                    user.getEmail()
            );
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }
        List<Employee> employees = employeeRepository.findAllByUserId(user.getId());
        Employee employee = selectLoginEmployee(employees);
        if (employee != null
                && (employee.getStatus() == EmployeeStatus.INACTIVE
                || employee.getStatus() == EmployeeStatus.TERMINATED)) {
            throw new UnauthorizedException("Account is inactive");
        }
        TokenService.TokenPair tokens = tokenService.createSession(user, employee, Boolean.TRUE.equals(request.rememberMe()));
        if (employee == null) {
            return new AuthResponse(
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    user.getId(),
                    null,
                    null,
                    null,
                    java.util.Set.of(),
                    user.getRegistrationStatus(),
                    null,
                    null,
                    null,
                    firstName(user, null),
                    lastName(user, null),
                    user.getEmail()
            );
        }
        usageTracker.record(employee.getOrganization(), employee, "auth", "login", "user logged in");
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                user.getId(),
                employee.getId(),
                employee.getOrganization().getId(),
                employee.getRole().name(),
                ResponseMapper.permissionNames(employee.getRole()),
                user.getRegistrationStatus(),
                null,
                null,
                null,
                firstName(user, employee),
                lastName(user, employee),
                user.getEmail()
        );
    }

    /**
     * Resolves a verified Google OpenID identity to one Qedet account.  The
     * stable Google subject is authoritative for repeat logins; a verified
     * email is only used to link a previously password-only Qedet account.
     */
    @Transactional
    public TokenService.TokenPair authenticateGoogleUser(GoogleIdentityClaims claims) {
        String subject = requiredGoogleClaim(claims == null ? null : claims.subject(), "subject");
        String email = requiredGoogleClaim(claims == null ? null : claims.email(), "email").toLowerCase();
        log.info("[GOOGLE-AUTH] STEP 5 - Resolving Qedet UserAccount for verified Google identity");

        UserAccount user = userAccountRepository.findByGoogleSubject(subject).orElse(null);
        boolean created = false;
        if (user == null) {
            user = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user == null) {
                user = newGoogleUser(email, subject, claims);
                user = userAccountRepository.save(user);
                created = true;
            } else {
                ensureUserActive(user);
                linkGoogleIdentity(user, subject);
                // The matching address was verified by Google, so it satisfies
                // the existing Qedet email-verification state as well.
                user.setEmailVerified(true);
                userAccountRepository.save(user);
            }
        }

        ensureUserActive(user);
        // A legacy Google-linked account may predate the verified-email state.
        // Only mark the Qedet address verified when it is the verified address
        // returned by Google; never replace an account email from a later login.
        if (user.getEmail().equalsIgnoreCase(email) && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            userAccountRepository.save(user);
        }
        Employee employee = selectLoginEmployee(employeeRepository.findAllByUserId(user.getId()));
        if (employee != null
                && (employee.getStatus() == EmployeeStatus.INACTIVE
                || employee.getStatus() == EmployeeStatus.TERMINATED)) {
            throw new UnauthorizedException("Account is inactive");
        }
        log.info("[GOOGLE-AUTH] STEP 5 - Qedet UserAccount resolved userId={} created={}", user.getId(), created);
        log.info("[GOOGLE-AUTH] STEP 6 - Creating Qedet authentication userId={}", user.getId());
        TokenService.TokenPair tokens = tokenService.createSession(user, employee, false);
        if (employee != null) {
            usageTracker.record(employee.getOrganization(), employee, "auth", "google_login", "user logged in");
        }
        return tokens;
    }

    @Transactional(readOnly = true)
    public boolean accountExists(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email.trim().toLowerCase()).isPresent();
    }

    private UserAccount newGoogleUser(String email, String subject, GoogleIdentityClaims claims) {
        UserAccount user = new UserAccount(
                email,
                passwordEncoder.encode(UUID.randomUUID() + ":" + SECURE_RANDOM.nextLong())
        );
        user.setGoogleSubject(subject);
        user.setGooglePictureUrl(blankToNull(claims.pictureUrl()));
        user.setEmailVerified(true);
        user.setRegistrationName(claims.displayName());
        user.setRegistrationStatus(RegistrationStatus.PERSONAL_INFORMATION);
        return user;
    }

    private void linkGoogleIdentity(UserAccount user, String subject) {
        if (user.getGoogleSubject() == null || user.getGoogleSubject().isBlank()) {
            user.setGoogleSubject(subject);
            return;
        }
        if (!user.getGoogleSubject().equals(subject)) {
            throw new ConflictException("GOOGLE_IDENTITY_ALREADY_LINKED",
                    "This Qedet account is already linked to a different Google identity.");
        }
    }

    private void ensureUserActive(UserAccount user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }
    }

    private String requiredGoogleClaim(String value, String claim) {
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException("GOOGLE_AUTH_FAILED", "Verified Google " + claim + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public AuthResponse authResponseForSession(q.qedet.auth.entity.AuthSession session,
                                                TokenService.TokenPair tokens) {
        return buildAuthResponse(session.getUser(), session.getEmployee(), tokens);
    }

    @Transactional(readOnly = true)
    public AuthResponse currentUser(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        UserAccount user = userAccountRepository.findById(principal.userId())
                .orElseThrow(() -> new UnauthorizedException("User account not found"));
        Employee employee = principal.employeeId() == null ? null
                : employeeRepository.findById(principal.employeeId()).orElse(null);
        return new AuthResponse(
                null,
                null,
                user.getId(),
                principal.employeeId(),
                principal.organizationId(),
                principal.role() == null ? null : principal.role().name(),
                principal.permissions().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()),
                user.getRegistrationStatus(),
                null,
                null,
                null,
                firstName(user, employee),
                lastName(user, employee),
                user.getEmail()
        );
    }

    /**
     * Keep the current single-membership behavior isolated so it can later be
     * replaced with organization selection/context handling.
     */
    private Employee selectLoginEmployee(List<Employee> employees) {
        return employees.isEmpty() ? null : employees.get(0);
    }

    private AuthResponse buildAuthResponse(UserAccount user, Employee employee,
                                           TokenService.TokenPair tokens) {
        if (employee == null) {
            return new AuthResponse(
                    tokens.accessToken(), tokens.refreshToken(), user.getId(), null, null, null,
                    java.util.Set.of(), user.getRegistrationStatus(), null, null, null,
                    firstName(user, null), lastName(user, null), user.getEmail()
            );
        }
        return new AuthResponse(
                tokens.accessToken(), tokens.refreshToken(), user.getId(), employee.getId(),
                employee.getOrganization().getId(), employee.getRole().name(),
                ResponseMapper.permissionNames(employee.getRole()), user.getRegistrationStatus(),
                null, null, null, firstName(user, employee), lastName(user, employee), user.getEmail()
        );
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        var pair = tokenService.refresh(refreshToken);
        var session = tokenService.authenticate(pair.accessToken());
        Employee employee = session.getEmployee();
        UserAccount user = session.getUser();
        if (employee == null) {
            return new AuthResponse(
                    pair.accessToken(),
                    pair.refreshToken(),
                    user.getId(),
                    null,
                    null,
                    null,
                    java.util.Set.of(),
                    user.getRegistrationStatus(),
                    null,
                    null,
                    null,
                    firstName(user, null),
                    lastName(user, null),
                    user.getEmail()
            );
        }
        return new AuthResponse(
                pair.accessToken(),
                pair.refreshToken(),
                user.getId(),
                employee.getId(),
                employee.getOrganization().getId(),
                employee.getRole().name(),
                ResponseMapper.permissionNames(employee.getRole()),
                user.getRegistrationStatus(),
                null,
                null,
                null,
                firstName(user, employee),
                lastName(user, employee),
                user.getEmail()
        );
    }

    private String firstName(UserAccount user, Employee employee) {
        if (employee != null) {
            return employee.getFirstName();
        }
        return splitRegistrationName(user.getRegistrationName())[0];
    }

    private String lastName(UserAccount user, Employee employee) {
        if (employee != null) {
            return employee.getLastName();
        }
        return splitRegistrationName(user.getRegistrationName())[1];
    }

    private String[] splitRegistrationName(String name) {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+", 2);
        return new String[]{
                parts.length > 0 ? parts[0] : null,
                parts.length > 1 ? parts[1] : null
        };
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        tokenService.logout(accessToken, refreshToken);
    }
}
