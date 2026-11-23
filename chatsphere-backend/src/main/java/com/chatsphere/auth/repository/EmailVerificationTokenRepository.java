package com.chatsphere.auth.repository;

import com.chatsphere.auth.entity.EmailVerificationToken;
import com.chatsphere.common.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);
}
