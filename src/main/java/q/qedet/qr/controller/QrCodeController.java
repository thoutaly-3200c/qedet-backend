package q.qedet.qr.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import q.qedet.qr.dto.*;
import q.qedet.qr.entity.QrCodeStatus;
import q.qedet.qr.service.QrCodeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/qr")
public class    QrCodeController {
    private final QrCodeService service;

    public QrCodeController(QrCodeService service) {
        this.service = service;
    }

    @PostMapping
    public QrCodeResponse create(@Valid @RequestBody QrCodeRequest request) {
        return service.create(request);
    }

    @GetMapping("/{publicIdentifier}")
    public QrCodeResponse resolve(@PathVariable String publicIdentifier) {
        return service.resolve(publicIdentifier);
    }

    @GetMapping(value = "/{publicIdentifier}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable String publicIdentifier) {
        return ResponseEntity.ok(service.image(publicIdentifier));
    }

    @PostMapping("/{id}/disable")
    public QrCodeResponse disable(@PathVariable UUID id) {
        return service.disableQrCode(id);
    }

    @PostMapping("/{id}/reactivate")
    public QrCodeResponse reactivate(@PathVariable UUID id) {
        return service.reactivateQrCode(id);
    }

    @PostMapping("/{id}/revoke")
    public QrCodeResponse revoke(@PathVariable UUID id) {
        return service.revokeQrCode(id);
    }

    @PatchMapping("/{id}/status")
    public QrCodeResponse changeStatus(@PathVariable UUID id, @RequestParam QrCodeStatus status) {
        return service.changeStatus(id, status);
    }

    @PostMapping("/{id}/regenerate")
    public QrCodeResponse regenerate(@PathVariable UUID id) {
        return service.regenerate(id);
    }
}
