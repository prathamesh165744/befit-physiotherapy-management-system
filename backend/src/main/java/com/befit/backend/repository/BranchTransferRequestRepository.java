package com.befit.backend.repository;

import com.befit.backend.entity.BranchTransferRequest;
import com.befit.backend.entity.TransferStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BranchTransferRequestRepository extends JpaRepository<BranchTransferRequest, Long> {
    Optional<BranchTransferRequest> findFirstByPatientIdAndStatus(Long patientId, TransferStatus status);
    List<BranchTransferRequest> findByPatientIdOrderByRequestedAtDesc(Long patientId, Pageable pageable);
    List<BranchTransferRequest> findByToBranchIdInAndStatusOrderByRequestedAtDesc(Collection<String> toBranchIds, TransferStatus status, Pageable pageable);
    List<BranchTransferRequest> findByToBranchIdInOrderByRequestedAtDesc(Collection<String> toBranchIds, Pageable pageable);
    List<BranchTransferRequest> findByFromBranchIdInAndStatusOrderByRequestedAtDesc(Collection<String> fromBranchIds, TransferStatus status, Pageable pageable);
    List<BranchTransferRequest> findByFromBranchIdInOrderByRequestedAtDesc(Collection<String> fromBranchIds, Pageable pageable);

    /** Outgoing tracking: how many requests leaving these branches are in each status (rows of [status, count]). */
    @org.springframework.data.jpa.repository.Query("select t.status, count(t) from BranchTransferRequest t where t.fromBranchId in :ids group by t.status")
    List<Object[]> countOutgoingByStatus(@org.springframework.data.repository.query.Param("ids") Collection<String> ids);

    /** Same for requests arriving at these branches (the approval queue's own tabs). */
    @org.springframework.data.jpa.repository.Query("select t.status, count(t) from BranchTransferRequest t where t.toBranchId in :ids group by t.status")
    List<Object[]> countIncomingByStatus(@org.springframework.data.repository.query.Param("ids") Collection<String> ids);
}
