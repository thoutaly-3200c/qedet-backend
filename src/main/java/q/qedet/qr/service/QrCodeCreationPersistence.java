package q.qedet.qr.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.qr.entity.QrCode;
import q.qedet.qr.repository.QrCodeRepository;

/**
 * Persists a QR association in its own transaction.  A duplicate-key failure
 * rolls back only this insert, allowing the caller to resolve the winning
 * association and return an idempotent result (or a domain conflict).
 */
@Service
public class QrCodeCreationPersistence {
    private final QrCodeRepository repository;

    public QrCodeCreationPersistence(QrCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QrCode save(QrCode qr) throws DataIntegrityViolationException {
        return repository.saveAndFlush(qr);
    }
}
