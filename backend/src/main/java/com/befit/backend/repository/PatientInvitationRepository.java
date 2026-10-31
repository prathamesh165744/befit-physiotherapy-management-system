package com.befit.backend.repository;

import com.befit.backend.entity.PatientInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PatientInvitationRepository extends JpaRepository<PatientInvitation, Long> {
    Optional<PatientInvitation> findByTokenHash(String tokenHash);

    /** Used when an invitation is redeemed: the row is locked so the same link can never create two accounts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PatientInvitation i where i.tokenHash = :hash")
    Optional<PatientInvitation> findForUpdateByTokenHash(@Param("hash") String hash);

    /** Invitations of this patient that were neither used nor revoked (expired ones included; check isUsable). */
    List<PatientInvitation> findByPatientIdAndUsedAtIsNullAndRevokedAtIsNull(Long patientId);

    @Query("select i from PatientInvitation i where i.patientId = :patientId and i.usedAt is null and i.revokedAt is null " +
           "and i.expiresAt > :now order by i.createdAt desc")
    List<PatientInvitation> findActive(@Param("patientId") Long patientId, @Param("now") LocalDateTime now);

    /** Directory: which of these patients have a live invitation? */
    @Query("select distinct i.patientId from PatientInvitation i where i.patientId in :ids and i.usedAt is null " +
           "and i.revokedAt is null and i.expiresAt > :now")
    List<Long> activeInvitedPatientIds(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);
}
