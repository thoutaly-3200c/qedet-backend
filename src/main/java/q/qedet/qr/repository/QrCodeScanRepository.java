package q.qedet.qr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.qr.entity.QrCodeScan;

import java.util.UUID;

public interface QrCodeScanRepository extends JpaRepository<QrCodeScan, UUID> {
}
