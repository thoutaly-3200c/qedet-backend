package q.qedet.hr.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import q.qedet.common.model.TimestampedEntity;
import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "hr_attendance_qr_tokens", uniqueConstraints = @UniqueConstraint(
        name = "uk_hr_attendance_qr_work_date", columnNames = {"attendance_qr_id", "work_date"}),
        indexes = @Index(name = "idx_hr_attendance_qr_token_hash", columnList = "token_hash"))
@Getter
@Setter
@NoArgsConstructor
public class AttendanceQrToken extends TimestampedEntity {
    @ManyToOne(optional = false) @JoinColumn(name = "attendance_qr_id", nullable = false)
    private AttendanceQr attendanceQr;
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "token_ciphertext", nullable = false, length = 512)
    private String tokenCiphertext;
    @Column(nullable = false)
    private Instant validFrom;
    @Column(nullable = false)
    private Instant validUntil;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AttendanceQrTokenStatus status = AttendanceQrTokenStatus.ACTIVE;
    private Instant lastUsedAt;
}
