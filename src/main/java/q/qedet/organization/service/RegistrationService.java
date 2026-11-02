package q.qedet.organization.service;

import org.springframework.stereotype.Service;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.entity.UserAccount;
import q.qedet.organization.dto.RegistrationRequest;

@Service
public class RegistrationService {
    private final OrganizationService organizationService;

    public RegistrationService(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    public AuthResponse registerOrganization(UserAccount user, RegistrationRequest request) {
        return organizationService.create(user, request);
    }
}