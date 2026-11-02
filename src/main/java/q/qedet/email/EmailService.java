package q.qedet.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${qedet.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${qedet.email.from:noreply@qedet.com}")
    private String from;

    @Value("${qedet.email.from-name:Qedet}")
    private String fromName;

    @Value("${qedet.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${qedet.frontend.base-url:http://localhost:8443}")
    private String frontendBaseUrl;

    public void sendVerificationEmail(String recipient, String verificationUrl) {
        send(recipient, "Verify your Qedet account", """
                Welcome to Qedet!

                Thank you for creating your account.

                Please verify your email address by clicking the link below:

                %s

                This verification link will expire soon.

                If you did not create this account, you can safely ignore this email.

                Best regards,
                %s
                """.formatted(verificationUrl, fromName));
    }

    public void sendVerificationCodeEmail(String recipient, String verificationCode) {
        send(recipient, "Verify your Qedet account", """
                Welcome to Qedet!

                Your email verification code is: %s

                This code expires in 15 minutes. If you did not create this account, you can safely ignore this email.

                Best regards,
                %s
                """.formatted(verificationCode, fromName));
    }

    public void sendInvitationEmail(String recipient, String invitationToken) {
        String invitationUrl = backendBaseUrl.replaceAll("/+$", "")
                + "/api/v1/invitations/"
                + URLEncoder.encode(invitationToken, StandardCharsets.UTF_8);
        invitationUrl += "/accept";
        String plainText = """
                You have been invited to join an organization on Qedet.

                Accept the invitation using the link below:

                %s

                Best regards,
                %s
                """.formatted(invitationUrl, fromName);
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <p>You have been invited to join an organization on Qedet.</p>
                    <p>Click the button below to accept the invitation:</p>
                    <p>
                        <a href="%s"
                           style="display:inline-block;padding:12px 20px;background-color:#2563eb;color:#ffffff;text-decoration:none;border-radius:6px;">
                            Accept Invitation
                        </a>
                    </p>
                    <p>Best regards,<br>%s</p>
                </body>
                </html>
                """.formatted(escapeHtml(invitationUrl), escapeHtml(fromName));

        if (!emailEnabled) {
            log.warn("[EMAIL] Invitation email skipped: emailEnabled=false");
            return;
        }
        log.info("[EMAIL] Sending invitation email to {}", recipient);
        try {
            MimeMessageHelper message = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject("You are invited to join Qedet");
            message.setText(plainText, html);
            mailSender.send(message.getMimeMessage());
            log.info("[EMAIL] Invitation email sent successfully to {}", recipient);
        } catch (Exception exception) {
            log.error("[EMAIL] Failed to send invitation email to {}", recipient, exception);
            throw new IllegalStateException("Unable to send invitation email", exception);
        }
    }

    public void sendPasswordResetEmail(String recipient, String resetToken) {
        String resetUrl = frontendBaseUrl.replaceAll("/+$", "")
                + "/auth/create-new-password?token="
                + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
        String plainText = """
                We received a request to reset your Qedet password.

                Reset your password using the link below:

                %s

                This link expires in 20 minutes. If you did not request this, you can safely ignore this email.

                Best regards,
                %s
                """.formatted(resetUrl, fromName);
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <body>
                    <p>We received a request to reset your Qedet password.</p>
                    <p>Click the button below to choose a new password:</p>
                    <p>
                        <a href="%s"
                           style="display:inline-block;padding:12px 20px;background-color:#2563eb;color:#ffffff;text-decoration:none;border-radius:6px;">
                            Reset your password
                        </a>
                    </p>
                    <p>This link expires in 20 minutes. If you did not request this, you can safely ignore this email.</p>
                    <p>Best regards,<br>%s</p>
                </body>
                </html>
                """.formatted(escapeHtml(resetUrl), escapeHtml(fromName));

        if (!emailEnabled) {
            return;
        }
        try {
            MimeMessageHelper message = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject("Reset your Qedet password");
            message.setText(plainText, html);
            mailSender.send(message.getMimeMessage());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send password reset email", exception);
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;");
    }

    private void send(String recipient, String subject, String text) {
        if (!emailEnabled) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender. send(message);
    }
}