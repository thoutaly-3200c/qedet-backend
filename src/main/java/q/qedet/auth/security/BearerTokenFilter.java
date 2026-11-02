package q.qedet.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import q.qedet.employee.repository.EmployeeRoleRepository;
import q.qedet.employee.repository.RolePermissionRepository;
import q.qedet.common.exception.UnauthorizedException;
import q.qedet.common.model.Permission;

public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

    private final TokenService tokenService;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public BearerTokenFilter(TokenService tokenService, EmployeeRoleRepository employeeRoleRepository,
                             RolePermissionRepository rolePermissionRepository) {
        this.tokenService = tokenService;
        this.employeeRoleRepository = employeeRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean authMeRequest = isAuthMeRequest(request);
        if (authMeRequest) {
            log.info("[AUTH] /auth/me request received");
        }

        String header = request.getHeader("Authorization");
        boolean bearerPresent = header != null && header.startsWith("Bearer ");
        if (authMeRequest) {
            log.info("[AUTH] Bearer token present={}", bearerPresent);
        }

        if (bearerPresent) {
            String token = header.substring(7);
            try {
                var session = tokenService.authenticate(token);
                if (authMeRequest) {
                    log.info("[AUTH] Token validation result={} userId={}",
                            true, session.getUser().getId());
                }
                var employee = session.getEmployee();
                Set<Permission> permissions = EnumSet.noneOf(Permission.class);
                if (employee != null) {
                    permissions.addAll(PermissionCatalog.permissionsFor(employee.getRole()));
                    List<java.util.UUID> roleIds = employeeRoleRepository
                            .findAllByEmployeeIdAndEndedAtIsNullAndRoleOrganizationId(
                                    employee.getId(), employee.getOrganization().getId())
                            .stream()
                            .map(assignment -> assignment.getRole().getId())
                            .toList();
                    if (!roleIds.isEmpty()) {
                        rolePermissionRepository.findPermissionCodesByRoleIds(roleIds)
                                .forEach(code -> {
                                    try {
                                        permissions.add(Permission.valueOf(code));
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                });
                    }
                }
                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                        session.getUser().getId(),
                        employee == null ? null : employee.getId(),
                        employee == null ? null : employee.getOrganization().getId(),
                        employee == null ? null : employee.getRole(),
                        permissions,
                        session.getUser().getEmail()
                );
                var authorities = principal.permissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.name()))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (authMeRequest) {
                    Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
                    Object principalObj = contextAuth == null ? null : contextAuth.getPrincipal();
                    log.info("[AUTH] SecurityContext authenticated={} principalType={}",
                            contextAuth != null && contextAuth.isAuthenticated(),
                            principalObj == null ? null : principalObj.getClass().getSimpleName());
                }
            } catch (UnauthorizedException exception) {
                if (authMeRequest) {
                    log.info("[AUTH] Token validation result={} userId={}", false, null);
                }
                SecurityContextHolder.clearContext();
                if (authMeRequest) {
                    Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
                    Object principalObj = contextAuth == null ? null : contextAuth.getPrincipal();
                    log.info("[AUTH] SecurityContext authenticated={} principalType={}",
                            contextAuth != null && contextAuth.isAuthenticated(),
                            principalObj == null ? null : principalObj.getClass().getSimpleName());
                }
            }
        } else if (authMeRequest) {
            Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
            Object principalObj = contextAuth == null ? null : contextAuth.getPrincipal();
            log.info("[AUTH] SecurityContext authenticated={} principalType={}",
                    contextAuth != null && contextAuth.isAuthenticated(),
                    principalObj == null ? null : principalObj.getClass().getSimpleName());
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isAuthMeRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.endsWith("/api/v1/auth/me") || path.endsWith("/api/v1/auth/current-user"));
    }
}
