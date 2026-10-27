package com.aireck.javapp.repository;

import com.aireck.javapp.model.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
  Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

  List<RefreshToken> findAllByUser_IdAndRevokedFalse(Integer userId);
}
