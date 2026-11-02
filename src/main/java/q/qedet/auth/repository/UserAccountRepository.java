package q.qedet.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import q.qedet.auth.entity.UserAccount;


import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByGoogleSubject(String googleSubject);
}
