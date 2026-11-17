package com.portfolio.identity.internal.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByGoogleSubject(String googleSubject);

    boolean existsByEmailIgnoreCase(String email);
}
