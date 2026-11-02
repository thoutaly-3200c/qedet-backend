package q.qedet.organization.policy.dto;

import jakarta.validation.constraints.*;
import q.qedet.organization.policy.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PolicyDtos {
 private PolicyDtos(){}
 public record AttendancePolicyRequest(LocalDate effectiveFrom, Boolean enabled, Boolean requireCheckIn, Boolean requireCheckOut,
  CheckInMethod checkInMethod, @Min(0) Integer gracePeriodMinutes, Boolean allowEarlyCheckIn, CheckOutMethod checkOutMethod,
  Boolean allowEarlyCheckout, Boolean earlyCheckoutRequiresApproval, Boolean requireAttendanceQr, Boolean requireLocation,
  Boolean allowRemoteAttendance, MissingCheckoutHandling missingCheckoutHandling, Boolean allowEmployeeCorrectionRequest,
  Boolean correctionRequiresApproval, Boolean correctionReasonRequired){}
 public record AttendancePolicyResponse(LocalDate effectiveFrom, boolean enabled, boolean requireCheckIn, boolean requireCheckOut,
  CheckInMethod checkInMethod, Integer gracePeriodMinutes, boolean allowEarlyCheckIn, CheckOutMethod checkOutMethod,
  boolean allowEarlyCheckout, boolean earlyCheckoutRequiresApproval, boolean requireAttendanceQr, boolean requireLocation,
  boolean allowRemoteAttendance, MissingCheckoutHandling missingCheckoutHandling, boolean allowEmployeeCorrectionRequest,
  boolean correctionRequiresApproval, boolean correctionReasonRequired){}
 public record LeavePolicyRequest(LocalDate effectiveFrom, Boolean enabled, Boolean allowEmployeeRequests, Boolean requireApproval,
  Boolean reasonRequired, Boolean attachmentRequired, @Min(0) Integer minimumNoticeDays, Boolean allowHalfDayLeave,
  Boolean allowHourlyLeave, Boolean allowNegativeBalance, Boolean carryOverEnabled, @DecimalMin("0") BigDecimal carryOverMaximumDays){}
 public record LeavePolicyResponse(LocalDate effectiveFrom, boolean enabled, boolean allowEmployeeRequests, boolean requireApproval,
  boolean reasonRequired, boolean attachmentRequired, Integer minimumNoticeDays, boolean allowHalfDayLeave, boolean allowHourlyLeave,
  boolean allowNegativeBalance, boolean carryOverEnabled, BigDecimal carryOverMaximumDays){}
 public record PayrollPolicyRequest(LocalDate effectiveFrom, Boolean enabled, PayrollFrequency payrollFrequency, PayDateRule payDateRule,
  Boolean payrollApprovalRequired, Boolean overtimeAuthorizationRequired, OvertimeCalculationMethod overtimeCalculationMethod,
  @DecimalMin("0") BigDecimal weekdayOvertimeMultiplier, @DecimalMin("0") BigDecimal weekendOvertimeMultiplier,
  @DecimalMin("0") BigDecimal holidayOvertimeMultiplier, Boolean lateDeductionEnabled, Boolean absenceDeductionEnabled,
  Boolean unpaidLeaveDeductionEnabled, MissingAttendanceHandling missingAttendanceHandling, @Min(1) @Max(31) Integer payrollCutoffDay){}
 public record PayrollPolicyResponse(LocalDate effectiveFrom, boolean enabled, PayrollFrequency payrollFrequency, PayDateRule payDateRule,
  boolean payrollApprovalRequired, boolean overtimeAuthorizationRequired, OvertimeCalculationMethod overtimeCalculationMethod,
  BigDecimal weekdayOvertimeMultiplier, BigDecimal weekendOvertimeMultiplier, BigDecimal holidayOvertimeMultiplier,
  boolean lateDeductionEnabled, boolean absenceDeductionEnabled, boolean unpaidLeaveDeductionEnabled,
  MissingAttendanceHandling missingAttendanceHandling, Integer payrollCutoffDay){}
}
