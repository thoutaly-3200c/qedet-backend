package q.qedet.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import q.qedet.auth.service.InvitationService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final String invitationAcceptRedirectUrl;

    public InvitationController(
            InvitationService invitationService,
            @Value("${qedet.frontend.invitation-accept-redirect-url:http://localhost:8443/login}") String invitationAcceptRedirectUrl
    ) {
        this.invitationService = invitationService;
        this.invitationAcceptRedirectUrl = invitationAcceptRedirectUrl;
    }

    @GetMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token) {
        invitationService.accept(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(invitationAcceptRedirectUrl))
                .build();
    }
}
