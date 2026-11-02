
package q.qedet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.dto.RegistrationResponse;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.auth.security.PermissionCatalog;
import q.qedet.auth.security.TokenService;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.service.InvitationService;
import q.qedet.auth.service.AuthService;

import q.qedet.common.model.Permission;
import q.qedet.common.model.RoleType;
import q.qedet.employee.dto.EmployeeRequest;
import q.qedet.employee.dto.InviteRequest;
import q.qedet.employee.service.EmployeeService;
import q.qedet.finance.model.CashBoxType;
import q.qedet.finance.repository.CashBoxRepository;
import q.qedet.organization.dto.RegistrationRequest;
import q.qedet.organization.service.DepartmentService;
import q.qedet.organization.service.RegistrationService;


import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class QedetApplicationTests {

    @Autowired
    RegistrationService registrationService;

    @Autowired
    DepartmentService departmentService;

    @Autowired
    EmployeeService employeeService;

    @Autowired
    InvitationService invitationService;

    @Autowired
    AuthService authService;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    TokenService tokenService;

    @Autowired
    CashBoxRepository cashBoxRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerCreatesOrganizationOwnerAndDepartments() {
        AuthResponse response = register("Ahmed Electronics", "ahmed@example.com");

        authenticate(response.accessToken());

        assertThat(departmentService.list()).hasSize(7);
        assertThat(employeeService.list(1, 20, null, null, null)).hasSize(1);
        assertThat(employeeService.list(1, 20, null, null, null).get(0).organizationId()).isEqualTo(response.organizationId());
        assertThat(employeeService.list(1, 20, null, null, null).get(0).id()).isEqualTo(response.employeeId());
        assertThat(cashBoxRepository.findAllByOrganizationIdOrderByNameAsc(response.organizationId()))
                .extracting(cashBox -> cashBox.getType())
                .containsExactly(CashBoxType.MAIN);
    }

    @Test
    void employeeInvitationFlowEnforcesTenantIsolation() {
        AuthResponse ownerOne = register("Alpha Trading", "alpha@example.com");
        AuthResponse ownerTwo = register("Beta Trading", "beta@example.com");

        authenticate(ownerOne.accessToken());
        var departmentId = departmentService.list().get(0).id();

        var employee = employeeService.create(new EmployeeRequest(
                "Mona",
                "Saleh",
                "Mona Saleh",
                "mona@example.com",
                "+201100000000",
                departmentId,
                RoleType.WAREHOUSE_EMPLOYEE.name(),
                null,
                null,
                null,
                null
        ));

        String invitationToken = employeeService.invite(employee.id(), new InviteRequest("mona@example.com", "Please join", "Warehouse Employee"));
        invitationService.accept(invitationToken);
        assertThat(employeeService.get(employee.id()).status()).isEqualTo(q.qedet.common.model.EmployeeStatus.ACTIVE);

        authenticate(ownerTwo.accessToken());
        assertThrows(Exception.class, () -> employeeService.get(employee.id()));
    }

    private AuthResponse register(String companyName, String email) {
        RegistrationRequest request = new RegistrationRequest(
                "Owner One",
                email,
                "StrongPass1!",
                "+201000000000",
                companyName,
                "Saudi Arabia",
                "Asia/Riyadh"
        );
        RegistrationResponse registration = authService.registerUser(request);
        UserAccount user = userAccountRepository.findById(registration.data().userId())
                .orElseThrow();
        user.setEmailVerified(true);
        userAccountRepository.save(user);
        return registrationService.registerOrganization(user, request);
    }

    private void authenticate(String accessToken) {
        var session = tokenService.authenticate(accessToken);
        var employee = session.getEmployee();
        Set<Permission> permissions = PermissionCatalog.permissionsFor(employee.getRole());
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                session.getUser().getId(),
                employee.getId(),
                employee.getOrganization().getId(),
                employee.getRole(),
                permissions,
                session.getUser().getEmail()
        );
        var authorities = permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.name())).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, accessToken, authorities)
        );
    }

}
