    package q.qedet.hr.attendance.service;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import javax.crypto.Cipher;
    import javax.crypto.spec.GCMParameterSpec;
    import jakarta.validation.Valid;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import q.qedet.auth.security.ActorContext;
    import q.qedet.common.exception.BadRequestException;
    import q.qedet.common.exception.ConflictException;
    import q.qedet.common.exception.ForbiddenException;
    import q.qedet.common.exception.NotFoundException;
    import q.qedet.common.model.DepartmentStatus;
    import q.qedet.common.model.EmployeeStatus;
    import q.qedet.common.model.LocationStatus;
    import q.qedet.common.model.Permission;
    import q.qedet.employee.entity.Employee;
    import q.qedet.employee.repository.EmployeeRepository;
    import q.qedet.hr.attendance.dto.*;
    import q.qedet.hr.attendance.entity.AttendanceEvent;
    import q.qedet.hr.attendance.entity.AttendanceQr;
    import q.qedet.hr.attendance.entity.AttendanceQrToken;
    import q.qedet.hr.attendance.entity.AttendanceRecord;
    import q.qedet.hr.attendance.model.AttendanceEventType;
    import q.qedet.hr.attendance.model.AttendanceQrTokenStatus;
    import q.qedet.hr.attendance.model.AttendanceStatus;
    import q.qedet.hr.attendance.model.AttendanceTodayStatus;
    import q.qedet.hr.attendance.repository.AttendanceEventRepository;
    import q.qedet.hr.attendance.repository.AttendanceQrRepository;
    import q.qedet.hr.attendance.repository.AttendanceQrTokenRepository;
    import q.qedet.hr.attendance.repository.AttendanceRecordRepository;
    import q.qedet.organization.entity.Department;
    import q.qedet.organization.entity.OrganizationSite;
    import q.qedet.organization.policy.entity.WorkSchedule;
    import q.qedet.organization.policy.entity.WorkdayDefinition;
    import q.qedet.organization.policy.entity.DepartmentScheduleAssignment;
    import q.qedet.organization.policy.model.WorkScheduleStatus;
    import q.qedet.organization.policy.model.AssignmentStatus;
    import q.qedet.organization.policy.repository.DepartmentScheduleAssignmentRepository;
    import q.qedet.organization.policy.repository.WorkScheduleRepository;
    import q.qedet.organization.policy.service.AttendanceScheduleService;
    import q.qedet.organization.policy.service.PolicyService;
    import q.qedet.organization.repository.DepartmentRepository;
    import q.qedet.organization.repository.OrganizationSiteRepository;
    
    import java.nio.charset.StandardCharsets;
    import java.security.MessageDigest;
    import java.security.SecureRandom;
    import java.time.*;
    import java.util.Base64;
    import java.util.HexFormat;
    import java.util.List;
    import java.util.UUID;
    
    @Service
    public class AttendanceService {
        private static final Logger log =
                LoggerFactory.getLogger(AttendanceService.class);
        private static final Duration QR_EARLY_ACCESS = Duration.ofMinutes(90);
        private static final Duration QR_LATE_ACCESS = Duration.ofMinutes(90);
        private final EmployeeRepository employees;
        private final AttendanceRecordRepository records;
        private final AttendanceEventRepository events;
        private final AttendanceQrRepository attendanceQrs;
        private final AttendanceQrTokenRepository qrTokens;
        private final DepartmentRepository departments;
        private final WorkScheduleRepository workSchedules;
        private final DepartmentScheduleAssignmentRepository departmentAssignments;
        private final ActorContext actor;
        private final AttendanceScheduleService schedules;
        private final OrganizationSiteRepository sites;
        private final PolicyService policies;
        private final AttendanceQrSynchronizationService attendanceQrSynchronization;
        private final byte[] encryptionKey;
        private final SecureRandom random = new SecureRandom();
    
        public AttendanceService(EmployeeRepository employees, AttendanceRecordRepository records,
                                 AttendanceEventRepository events, AttendanceQrRepository attendanceQrs,
                                 AttendanceQrTokenRepository qrTokens, DepartmentRepository departments,
                                 WorkScheduleRepository workSchedules, ActorContext actor,
                                 AttendanceScheduleService schedules, OrganizationSiteRepository sites,
                                 PolicyService policies, DepartmentScheduleAssignmentRepository departmentAssignments,
                                 AttendanceQrSynchronizationService attendanceQrSynchronization,
                                 @Value("${qedet.attendance.qr-encryption-key}") String encryptionKey) {
            this.employees = employees;
            this.records = records;
            this.events = events;
            this.attendanceQrs = attendanceQrs;
            this.qrTokens = qrTokens;
            this.departments = departments;
            this.workSchedules = workSchedules;
            this.departmentAssignments = departmentAssignments;
            this.actor = actor;
            this.schedules = schedules;
            this.sites = sites;
            this.policies = policies;
            this.attendanceQrSynchronization = attendanceQrSynchronization;
            this.encryptionKey = sha256(encryptionKey);
        }
    
        @Transactional
        public AttendanceQrResponse createQr(UUID organizationId, @Valid AttendanceQrCreateRequest request) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            Department department = departments.findByIdAndOrganizationId(request.departmentId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Department not found"));
            WorkSchedule schedule = workSchedules.findByIdAndOrganizationId(request.scheduleId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Schedule not found"));
            OrganizationSite OrganizationSite = sites.findByIdAndOrganizationId(request.locationId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("OrganizationSite not found"));
            if (department.getStatus() != DepartmentStatus.ACTIVE || OrganizationSite.getStatus() != LocationStatus.ACTIVE) {
                throw new ConflictException("Department and OrganizationSite must be active");
            }
            if (schedule.getStatus() != WorkScheduleStatus.ACTIVE) {
                throw new ConflictException("Schedule is not active");
            }
            DepartmentScheduleAssignment assignment = departmentAssignments
                    .findAllByOrganizationIdAndDepartmentId(organizationId, department.getId()).stream()
                    .filter(candidate -> candidate.getStatus() == AssignmentStatus.ACTIVE)
                    .filter(candidate -> candidate.getSchedule().getId().equals(schedule.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException(
                            "An active department schedule assignment must match the QR configuration"));
            AttendanceQr qr = attendanceQrs.findByOrganizationIdAndDepartmentId(organizationId, department.getId())
                    .orElseGet(AttendanceQr::new);
            qr.setOrganization(department.getOrganization());
            qr.setDepartment(department);
            qr.setSchedule(assignment.getSchedule());
            qr.setSite(OrganizationSite);
            qr.setActive(true);
            return qrResponse(attendanceQrs.save(qr));
        }
    
        @Transactional
        public List<AttendanceQrResponse> listQrs(UUID organizationId) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            attendanceQrSynchronization.synchronizeOrganization(organizationId);
            return attendanceQrs.findAllByOrganizationId(organizationId).stream().map(this::qrResponse).toList();
        }
    
        @Transactional(readOnly = true)
        public AttendanceQrResponse qr(UUID organizationId, UUID qrId) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            return qrResponse(getQr(organizationId, qrId));
        }
    
        @Transactional
        public AttendanceQrTodayResponse todayQr(UUID organizationId, UUID qrId) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            AttendanceQr qr = getQr(organizationId, qrId);
            LocalDate date = LocalDate.now(zone(qr.getSchedule()));
            AttendanceQrToken token = reconcile(qr, date, false);
            return todayResponse(qr, token);
        }
    
        @Transactional
        public AttendanceQrTodayResponse regenerateTodayQr(UUID organizationId, UUID qrId) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            AttendanceQr qr = getQr(organizationId, qrId);
            LocalDate date = LocalDate.now(zone(qr.getSchedule()));
            AttendanceQrToken token = reconcile(qr, date, true);
            if (token == null || token.getStatus() != AttendanceQrTokenStatus.ACTIVE) {
                throw new ConflictException("Today's attendance QR cannot be generated");
            }
            return todayResponse(qr, token);
        }
    
        @Transactional
        public AttendanceQrResponse setQrActive(UUID organizationId, UUID qrId, boolean active) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            throw new ConflictException("Attendance QR activation is managed by the department schedule assignment");
        }
    
        @Transactional(readOnly = true)
        public List<AttendanceQrHistoryResponse> qrHistory(UUID organizationId, UUID qrId) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_MANAGE);
            getQr(organizationId, qrId);
            return qrTokens.findAllByAttendanceQrIdOrderByWorkDateDesc(qrId).stream()
                    .map(token -> new AttendanceQrHistoryResponse(token.getWorkDate(), token.getValidFrom(),
                            token.getValidUntil(), token.getStatus())).toList();
        }
    
        @Scheduled(cron = "0 5 * * * *")
        @Transactional
        public void generateDailyAttendanceQrCodes() {
            for (AttendanceQr qr : attendanceQrs.findAllByActiveTrue()) {
                reconcile(qr, LocalDate.now(zone(qr.getSchedule())), false);
            }
        }
    
        @Transactional
        public AttendanceResponse scan(UUID organizationId, ScanRequest request) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_CHECK_IN);
            Employee employee = employees.findByIdAndOrganizationId(actor.employeeId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Employee not found"));
            if (employee.getStatus() != EmployeeStatus.ACTIVE) throw new ConflictException("Employee is inactive");
    
            AttendanceQrToken token = qrTokens.findByTokenHashAndStatus(hash(request.qrToken().trim()),
                            AttendanceQrTokenStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException("Invalid attendance QR"));
            AttendanceQr qr = token.getAttendanceQr();
            Instant now = Instant.now();
            ZoneId zone = zone(qr.getSchedule());
            LocalDate date = now.atZone(zone).toLocalDate();
            validateScan(employee, qr, token, date, now);
            token.setLastUsedAt(now);
    
            AttendanceRecord record = records.findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employee.getId(), date)
                    .orElseGet(() -> {
                        AttendanceRecord fresh = new AttendanceRecord(employee.getOrganization(), employee, date);
                        fresh.setSite(qr.getSite());
                        return fresh;
                    });
            log.info(
                    "[ATTENDANCE DEBUG][SCAN] organizationId={}, employeeId={}, " +
                            "workDate={}, checkInAt={}, checkOutAt={}, status={}, zone={}",
                    organizationId,
                    employee.getId(),
                    date,
                    record.getCheckInAt(),
                    record.getCheckOutAt(),
                    record.getStatus(),
                    zone
            );
            if (record.getCheckInAt() != null && record.getCheckOutAt() == null) {
                actor.requirePermission(Permission.ATTENDANCE_CHECK_OUT);
                record.setCheckOutAt(now);
                record.setWorkedMinutes((int) Duration.between(record.getCheckInAt(), now).toMinutes());
                calculate(record, date);
                record.setStatus(AttendanceStatus.COMPLETED);
                records.save(record);
                event(record, AttendanceEventType.ATTENDANCE_CHECK_OUT, now, qr.getSite());
                if (record.getEarlyLeaveMinutes() > 0) event(record, AttendanceEventType.EARLY_LEAVE_DETECTED, now, qr.getSite());
                if (record.getOvertimeMinutes() > 0) event(record, AttendanceEventType.OVERTIME_DETECTED, now, qr.getSite());
                return response(record);
            }
            if (record.getCheckInAt() != null && record.getCheckOutAt() != null) throw new ConflictException("Attendance already completed");
    
            record.setSchedule(qr.getSchedule());
            record.setScheduledMinutes(scheduledMinutes(qr.getSchedule(), date));
            record.setCheckInAt(now);
            record.setStatus(AttendanceStatus.IN_PROGRESS);
            record.setSite(qr.getSite());
            calculate(record, date);
            records.save(record);
            event(record, AttendanceEventType.ATTENDANCE_CHECK_IN, now, qr.getSite());
            if (record.getLateMinutes() > 0) event(record, AttendanceEventType.LATE_DETECTED, now, qr.getSite());
            return response(record);
        }
    
        @Transactional(readOnly = true)
        public AttendanceResponse view(UUID organizationId, UUID employeeId, LocalDate date) {
            requireOrganization(organizationId);
            if (!actor.employeeId().equals(employeeId)) actor.requirePermission(Permission.ATTENDANCE_VIEW_ALL);
            return response(records.findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employeeId, date)
                    .orElseThrow(() -> new NotFoundException("Attendance record not found")));
        }
    
        @Transactional(readOnly = true)
        public AttendanceTodayResponse my(UUID organizationId, LocalDate date) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_VIEW_SELF);
            Employee employee = employees.findByIdAndOrganizationId(actor.employeeId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Employee not found"));
            AttendanceRecord record = records.findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employee.getId(), date)
                    .orElse(null);
            log.info(
                    "[ATTENDANCE DEBUG][MY] organizationId={}, employeeId={}, " +
                            "requestedDate={}, recordFound={}, checkInAt={}, checkOutAt={}, status={}",
                    organizationId,
                    employee.getId(),
                    date,
                    record != null,
                    record != null ? record.getCheckInAt() : null,
                    record != null ? record.getCheckOutAt() : null,
                    record != null ? record.getStatus() : null
            );
            if (record != null) {
                return todayResponse(employee, date, record.getSchedule(), record.getSite(),
                        record.getCheckInAt() == null ? AttendanceTodayStatus.NOT_CHECKED_IN
                                : record.getCheckOutAt() == null ? AttendanceTodayStatus.CHECKED_IN
                                : AttendanceTodayStatus.CHECKED_OUT,
                        record.getCheckInAt(), record.getCheckOutAt());
            }
    
            WorkSchedule schedule = schedules.scheduleFor(employee, date);
            if (schedule == null || schedule.getStatus() != WorkScheduleStatus.ACTIVE) {
                return todayResponse(employee, date, null, null, AttendanceTodayStatus.NO_SCHEDULE, null, null);
            }
            WorkdayDefinition workday = workday(schedule, date);
            if (workday == null || !workday.isWorking() || workday.getStartTime() == null || workday.getEndTime() == null) {
                return todayResponse(employee, date, null, null, AttendanceTodayStatus.DAY_OFF, null, null);
            }
            return todayResponse(employee, date, schedule, employee.getSite(), AttendanceTodayStatus.NOT_CHECKED_IN, null, null);
        }
    
        @Transactional(readOnly = true)
        public List<AttendanceResponse> myHistory(UUID organizationId, LocalDate from, LocalDate to) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_VIEW_SELF);
            validateRange(from, to);
            return records.findAllByOrganizationIdAndEmployeeIdAndWorkDateBetween(organizationId, actor.employeeId(), from, to)
                    .stream().map(this::response).toList();
        }
    
        @Transactional(readOnly = true)
        public List<AttendanceOverviewResponse> attendance(UUID organizationId, LocalDate date, UUID departmentId,
                                                            UUID locationId, UUID scheduleId, AttendanceStatus status) {
            requireOrganization(organizationId);
            actor.requirePermission(Permission.ATTENDANCE_VIEW_ALL);
            return records.findAllByOrganizationIdAndWorkDateBetween(organizationId, date, date).stream()
                    .filter(record -> departmentId == null || (record.getEmployee().getDepartment() != null
                            && departmentId.equals(record.getEmployee().getDepartment().getId())))
                    .filter(record -> locationId == null || (record.getSite() != null && locationId.equals(record.getSite().getId())))
                    .filter(record -> scheduleId == null || (record.getSchedule() != null && scheduleId.equals(record.getSchedule().getId())))
                    .filter(record -> status == null || status == record.getStatus()).map(this::overview).toList();
        }
    
        private AttendanceQrToken reconcile(AttendanceQr qr, LocalDate date, boolean regenerate) {
            expirePastTokens(date);
            if (!qr.isActive() || qr.getSchedule().getStatus() != WorkScheduleStatus.ACTIVE || !isWorking(qr.getSchedule(), date)) {
                return null;
            }
            AttendanceQrToken token = qrTokens.findByAttendanceQrIdAndWorkDate(qr.getId(), date).orElse(null);
            WorkdayDefinition workday = workday(qr.getSchedule(), date);
            ZonedDateTime start = ZonedDateTime.of(date, workday.getStartTime(), zone(qr.getSchedule()));
            ZonedDateTime end = ZonedDateTime.of(date, workday.getEndTime(), zone(qr.getSchedule()));
            if (!end.isAfter(start)) end = end.plusDays(1);
            Instant validUntil = end.toInstant().plus(QR_LATE_ACCESS);
            if (!Instant.now().isBefore(validUntil)) {
                if (token != null && token.getStatus() == AttendanceQrTokenStatus.ACTIVE) {
                    token.setStatus(AttendanceQrTokenStatus.EXPIRED);
                }
                return token;
            }
            if (token != null && !regenerate && token.getStatus() == AttendanceQrTokenStatus.ACTIVE) return token;
            if (token == null) {
                token = new AttendanceQrToken();
                token.setAttendanceQr(qr);
                token.setWorkDate(date);
            }
            String raw = randomToken();
            token.setTokenHash(hash(raw));
            token.setTokenCiphertext(encrypt(raw));
            token.setValidFrom(start.toInstant().minus(QR_EARLY_ACCESS));
            token.setValidUntil(validUntil);
            token.setStatus(AttendanceQrTokenStatus.ACTIVE);
            return qrTokens.save(token);
        }
    
        private void validateScan(Employee employee, AttendanceQr qr, AttendanceQrToken token, LocalDate date, Instant now) {
            log.info(
                    "QR CHECK | now={} | scheduleZone={} | currentDate={} | tokenWorkDate={} | validFrom={} | validUntil={} | status={}",
                    now,
                    zone(qr.getSchedule()).getId(),
                    date,
                    token.getWorkDate(),
                    token.getValidFrom(),
                    token.getValidUntil(),
                    token.getStatus()
            );

            if (!qr.isActive() || qr.getDepartment().getStatus() != DepartmentStatus.ACTIVE
                    || qr.getSite().getStatus() != LocationStatus.ACTIVE) throw new ConflictException("Attendance QR is inactive");
            if (!token.getWorkDate().equals(date) || now.isBefore(token.getValidFrom()) || !now.isBefore(token.getValidUntil())) {
                throw new BadRequestException("Attendance QR has expired");
            }
            if (employee.getDepartment() == null || !employee.getDepartment().getId().equals(qr.getDepartment().getId())) {
                throw new ForbiddenException("Employee is not assigned to this department");
            }
            WorkSchedule applicable = schedules.scheduleFor(employee, date);
            if (applicable == null || applicable.getStatus() != WorkScheduleStatus.ACTIVE
                    || !applicable.getId().equals(qr.getSchedule().getId()) || !isWorking(applicable, date)) {
                throw new ConflictException("Employee has no applicable working schedule");
            }
        }
    
        private void expirePastTokens(LocalDate date) {
            qrTokens.findAllByStatusAndWorkDateBefore(AttendanceQrTokenStatus.ACTIVE, date)
                    .forEach(token -> token.setStatus(AttendanceQrTokenStatus.EXPIRED));
            Instant now = Instant.now();
            qrTokens.findAllByStatus(AttendanceQrTokenStatus.ACTIVE).stream()
                    .filter(token -> !now.isBefore(token.getValidUntil()))
                    .forEach(token -> token.setStatus(AttendanceQrTokenStatus.EXPIRED));
        }
    
        private boolean isWorking(WorkSchedule schedule, LocalDate date) {
            WorkdayDefinition definition = workday(schedule, date);
            return definition != null && definition.isWorking() && definition.getStartTime() != null && definition.getEndTime() != null;
        }
    
        private WorkdayDefinition workday(WorkSchedule schedule, LocalDate date) {
            return schedule.getWorkdays().stream().filter(day -> day.getDayOfWeek() == date.getDayOfWeek()).findFirst().orElse(null);
        }
    
        private void calculate(AttendanceRecord record, LocalDate date) {
            if (record.getSchedule() == null) return;
            ZoneId zone = zone(record.getSchedule());
            WorkdayDefinition definition = workday(record.getSchedule(), date);
            if (definition == null || !definition.isWorking()) return;
            if (record.getCheckInAt() != null && definition.getStartTime() != null) {
                int grace = java.util.Optional.ofNullable(policies.resolveAttendancePolicy(record.getOrganization().getId(), date))
                        .map(q.qedet.organization.policy.entity.AttendancePolicy::getGracePeriodMinutes).orElse(0);
                long late = Duration.between(LocalDateTime.of(date, definition.getStartTime()).plusMinutes(grace),
                        record.getCheckInAt().atZone(zone).toLocalDateTime()).toMinutes();
                record.setLateMinutes((int) Math.max(0, late));
            }
            if (record.getCheckOutAt() != null && definition.getEndTime() != null) {
                long early = Duration.between(record.getCheckOutAt().atZone(zone).toLocalDateTime(),
                        LocalDateTime.of(date, definition.getEndTime())).toMinutes();
                record.setEarlyLeaveMinutes((int) Math.max(0, early));
                int worked = record.getWorkedMinutes() == null ? 0 : record.getWorkedMinutes();
                record.setOvertimeMinutes(Math.max(0, worked - scheduledMinutes(record.getSchedule(), date)));
            }
        }
    
        private int scheduledMinutes(WorkSchedule schedule, LocalDate date) {
            WorkdayDefinition day = workday(schedule, date);
            if (day == null || !day.isWorking() || day.getStartTime() == null || day.getEndTime() == null) return 0;
            return Math.max(0, (int) Duration.between(day.getStartTime(), day.getEndTime()).toMinutes() - day.getBreakDurationMinutes());
        }
    
        private void event(AttendanceRecord record, AttendanceEventType type, Instant occurredAt, OrganizationSite OrganizationSite) {
            AttendanceEvent event = new AttendanceEvent(record.getOrganization(), record.getEmployee(), record, type, occurredAt, null);
            event.setSite(OrganizationSite);
            events.save(event);
        }
    
        private AttendanceResponse response(AttendanceRecord record) {
            return new AttendanceResponse(record.getId(), record.getEmployee().getId(), record.getWorkDate(), record.getStatus(),
                    record.getCheckInAt(), record.getCheckOutAt(), record.getWorkedMinutes(), record.getOvertimeMinutes(),
                    record.isAuthorizedLeave(), record.getScheduledMinutes(), record.getLateMinutes(), record.getEarlyLeaveMinutes(),
                    record.getSite() == null ? null : record.getSite().getId());
        }
    
        private AttendanceTodayResponse todayResponse(Employee employee, LocalDate date, WorkSchedule schedule,
                                                       OrganizationSite OrganizationSite, AttendanceTodayStatus status,
                                                       Instant checkIn, Instant checkOut) {
            WorkdayDefinition workday = schedule == null ? null : workday(schedule, date);
            boolean hasSchedule = status == AttendanceTodayStatus.NOT_CHECKED_IN
                    || status == AttendanceTodayStatus.CHECKED_IN
                    || status == AttendanceTodayStatus.CHECKED_OUT;
            return new AttendanceTodayResponse(
                    date,
                    status,
                    hasSchedule,
                    status == AttendanceTodayStatus.DAY_OFF,
                    employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                    schedule == null ? null : schedule.getName(),
                    workday == null ? null : new AttendanceTodayResponse.Schedule(workday.getStartTime(), workday.getEndTime()),
                    OrganizationSite == null ? null : OrganizationSite.getName(),
                    checkIn,
                    checkOut
            );
        }
    
        private AttendanceOverviewResponse overview(AttendanceRecord record) {
            Employee employee = record.getEmployee();
            return new AttendanceOverviewResponse(employee.getId(), employee.getFirstName() + " " + employee.getLastName(),
                    employee.getDepartment() == null ? null : employee.getDepartment().getId(),
                    employee.getDepartment() == null ? null : employee.getDepartment().getName(),
                    record.getSchedule() == null ? null : record.getSchedule().getId(),
                    record.getSchedule() == null ? null : record.getSchedule().getName(),
                    record.getSite() == null ? null : record.getSite().getId(), record.getCheckInAt(), record.getCheckOutAt(),
                    record.getLateMinutes(), record.getEarlyLeaveMinutes(), record.getOvertimeMinutes(), record.getStatus());
        }
    
        private AttendanceQrTodayResponse todayResponse(AttendanceQr qr, AttendanceQrToken token) {
            if (token == null) return new AttendanceQrTodayResponse(qr.getId(), qr.getDepartment().getId(), qr.getDepartment().getName(),
                    qr.getSchedule().getId(), qr.getSchedule().getName(), qr.getSite().getId(), qr.getSite().getName(),
                    LocalDate.now(zone(qr.getSchedule())), null, null, null, "NO_WORKDAY");
            String rawToken = token.getStatus() == AttendanceQrTokenStatus.ACTIVE ? decrypt(token.getTokenCiphertext()) : null;
            return new AttendanceQrTodayResponse(qr.getId(), qr.getDepartment().getId(), qr.getDepartment().getName(),
                    qr.getSchedule().getId(), qr.getSchedule().getName(), qr.getSite().getId(), qr.getSite().getName(),
                    token.getWorkDate(), rawToken, token.getValidFrom(), token.getValidUntil(),
                    token.getStatus().name());
        }
    
        private AttendanceQrResponse qrResponse(AttendanceQr qr) {
            return new AttendanceQrResponse(qr.getId(), qr.getDepartment().getId(), qr.getDepartment().getName(),
                    qr.getSchedule().getId(), qr.getSchedule().getName(), qr.getSite().getId(), qr.getSite().getName(), qr.isActive());
        }
    
        private AttendanceQr getQr(UUID organizationId, UUID qrId) {
            return attendanceQrs.findByIdAndOrganizationId(qrId, organizationId)
                    .orElseThrow(() -> new NotFoundException("Attendance QR not found"));
        }
        private void requireOrganization(UUID organizationId) {
            if (!actor.organizationId().equals(organizationId)) throw new ForbiddenException("Organization access denied");
        }
        private void validateRange(LocalDate from, LocalDate to) {
            if (from == null || to == null || to.isBefore(from)) throw new BadRequestException("Invalid date range");
        }
        private ZoneId zone(WorkSchedule schedule) {
            return ZoneId.of(schedule.getTimezone() == null || schedule.getTimezone().isBlank() ?  "Africa/Cairo" : schedule.getTimezone());
        }
        private String randomToken() {
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        private String hash(String value) {
            return HexFormat.of().formatHex(sha256(value));
        }
        private byte[] sha256(String value) {
            try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
            catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
        }
        private String encrypt(String value) {
            try {
                byte[] iv = new byte[12]; random.nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, iv));
                byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                byte[] payload = new byte[iv.length + encrypted.length];
                System.arraycopy(iv, 0, payload, 0, iv.length); System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            } catch (Exception exception) { throw new IllegalStateException("Unable to encrypt attendance QR token", exception); }
        }
        private String decrypt(String payload) {
            try {
                byte[] bytes = Base64.getUrlDecoder().decode(payload);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(encryptionKey, "AES"),
                        new GCMParameterSpec(128, bytes, 0, 12));
                return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
            } catch (Exception exception) { throw new IllegalStateException("Unable to decrypt attendance QR token", exception); }
        }
    }
