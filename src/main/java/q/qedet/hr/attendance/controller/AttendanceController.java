package q.qedet.hr.attendance.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.hr.attendance.dto.AttendanceResponse;
import q.qedet.hr.attendance.dto.AttendanceTodayResponse;
import q.qedet.hr.attendance.dto.AttendanceOverviewResponse;
import q.qedet.hr.attendance.dto.AttendanceQrCreateRequest;
import q.qedet.hr.attendance.dto.AttendanceQrHistoryResponse;
import q.qedet.hr.attendance.dto.AttendanceQrResponse;
import q.qedet.hr.attendance.dto.AttendanceQrTodayResponse;
import q.qedet.hr.attendance.dto.ScanRequest;
import q.qedet.hr.attendance.model.AttendanceStatus;
import q.qedet.hr.attendance.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public AttendanceResponse scan(
            @PathVariable UUID organizationId,
            @Valid @RequestBody ScanRequest request) {
        return service.scan(organizationId, request);
    }

    @PostMapping("/qr")
    public AttendanceQrResponse createQr(@PathVariable UUID organizationId,
                                         @Valid @RequestBody AttendanceQrCreateRequest request) {
        return service.createQr(organizationId, request);
    }

    @GetMapping("/qr")
    public List<AttendanceQrResponse> listQrs(@PathVariable UUID organizationId) {
        return service.listQrs(organizationId);
    }

    @GetMapping("/qr/{qrId}")
    public AttendanceQrResponse qr(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.qr(organizationId, qrId);
    }

    @GetMapping("/qr/{qrId}/today")
    public AttendanceQrTodayResponse todayQr(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.todayQr(organizationId, qrId);
    }

    @PostMapping("/qr/{qrId}/today/regenerate")
    public AttendanceQrTodayResponse regenerateTodayQr(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.regenerateTodayQr(organizationId, qrId);
    }

    @PostMapping("/qr/{qrId}/activate")
    public AttendanceQrResponse activateQr(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.setQrActive(organizationId, qrId, true);
    }

    @PostMapping("/qr/{qrId}/deactivate")
    public AttendanceQrResponse deactivateQr(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.setQrActive(organizationId, qrId, false);
    }

    @GetMapping("/qr/{qrId}/history")
    public List<AttendanceQrHistoryResponse> qrHistory(@PathVariable UUID organizationId, @PathVariable UUID qrId) {
        return service.qrHistory(organizationId, qrId);
    }

    @GetMapping("/my")
    public AttendanceTodayResponse my(@PathVariable UUID organizationId, @RequestParam LocalDate date) {
        return service.my(organizationId, date);
    }

    @GetMapping("/my/history")
    public List<AttendanceResponse> myHistory(@PathVariable UUID organizationId,
                                               @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.myHistory(organizationId, from, to);
    }

    @GetMapping
    public List<AttendanceOverviewResponse> attendance(@PathVariable UUID organizationId, @RequestParam LocalDate date,
                                                        @RequestParam(required = false) UUID departmentId,
                                                        @RequestParam(required = false) UUID locationId,
                                                        @RequestParam(required = false) UUID scheduleId,
                                                        @RequestParam(required = false) AttendanceStatus status) {
        return service.attendance(organizationId, date, departmentId, locationId, scheduleId, status);
    }

    @GetMapping("/{employeeId}")
    public AttendanceResponse view(
            @PathVariable UUID organizationId,
            @PathVariable UUID employeeId,
            @RequestParam LocalDate date) {
        return service.view(organizationId, employeeId, date);
    }
}
