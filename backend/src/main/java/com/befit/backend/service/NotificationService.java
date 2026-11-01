package com.befit.backend.service;

import com.befit.backend.dto.NotificationDTO;
import com.befit.backend.entity.*;
import com.befit.backend.exception.ApiException;
import com.befit.backend.repository.BranchTransferRequestRepository;
import com.befit.backend.repository.NotificationRepository;
import com.befit.backend.repository.PatientRepository;
import com.befit.backend.repository.UserRepository;
import com.befit.backend.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-app notifications about branch-transfer workflow events.
 *
 * RECIPIENTS (each person at most once, never the person who caused the event, only active accounts):
 *   TRANSFER_REQUESTED : everyone who can DECIDE it - admins, the destination branch's BRANCH_MANAGERs, and the destination
 *                        receptionists while befit.transfer.manager-approval-required=false.
 *   TRANSFER_APPROVED / REJECTED : whoever raised the request, the patient (if they have a portal login), and the SOURCE
 *                        branch's BRANCH_MANAGERs.
 *   TRANSFER_CANCELLED : the deciders (as above), whoever raised the request, and the patient.
 *
 * ACCESS: reading and updating always go through the caller's own user id (NotificationRepository has no query without it),
 * and another user's notification is reported as "not found". Links are computed per viewer at read time from the same
 * branch rules the transfer screens use, and are null when the viewer may not open the workflow.
 *
 * DELIVERY never breaks the business action: it runs after the transfer's transaction commits, in its own transaction, and
 * a failure is only logged.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    static final String TRANSFER = "TRANSFER";
    static final int DEFAULT_LIMIT = 30;
    static final int MAX_LIMIT = 100;

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final PatientRepository patients;
    private final BranchTransferRequestRepository transfers;
    private final BranchService branchService;
    private final TransactionTemplate newTransaction;
    private final boolean managerApprovalRequired;

    public NotificationService(NotificationRepository notifications, UserRepository users, PatientRepository patients,
                               BranchTransferRequestRepository transfers, BranchService branchService,
                               PlatformTransactionManager transactionManager,
                               @Value("${befit.transfer.manager-approval-required:true}") boolean managerApprovalRequired) {
        this.notifications = notifications;
        this.users = users;
        this.patients = patients;
        this.transfers = transfers;
        this.branchService = branchService;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.managerApprovalRequired = managerApprovalRequired;
    }

    // ================================================================== reading: always the caller's own

    @Transactional(readOnly = true)
    public List<NotificationDTO> list(AuthUser user, boolean unreadOnly, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        PageRequest page = PageRequest.of(0, size);
        List<Notification> rows = unreadOnly
                ? notifications.findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.id(), page)
                : notifications.findByRecipientUserIdOrderByCreatedAtDesc(user.id(), page);
        return toDtos(user, rows);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthUser user) {
        return notifications.countByRecipientUserIdAndReadAtIsNull(user.id());
    }

    /** Marks one of the caller's notifications read. Somebody else's (or a missing one) is a plain 404. */
    @Transactional
    public NotificationDTO markRead(AuthUser user, Long id) {
        Notification n = notifications.findByIdAndRecipientUserId(id, user.id())
                .orElseThrow(() -> ApiException.notFound("Notification not found."));
        if (!n.isRead()) {
            n.setReadAt(LocalDateTime.now());
            notifications.save(n);
        }
        return toDtos(user, List.of(n)).get(0);
    }

    /** Marks all of the caller's unread notifications read; returns how many changed. */
    @Transactional
    public int markAllRead(AuthUser user) {
        return notifications.markAllRead(user.id(), LocalDateTime.now());
    }

    private List<NotificationDTO> toDtos(AuthUser viewer, List<Notification> rows) {
        List<Long> transferIds = rows.stream().filter(n -> TRANSFER.equals(n.getRelatedType()) && n.getRelatedId() != null)
                .map(Notification::getRelatedId).distinct().toList();
        Map<Long, BranchTransferRequest> byId = transferIds.isEmpty() ? Map.of()
                : transfers.findAllById(transferIds).stream().collect(Collectors.toMap(BranchTransferRequest::getId, t -> t));
        Long ownPatientId = null;
        if (viewer.role() == Role.PATIENT && !transferIds.isEmpty()) {
            ownPatientId = patients.findByUserEmail(viewer.email()).map(Patient::getId).orElse(null);
        }
        List<NotificationDTO> out = new ArrayList<>();
        for (Notification n : rows) {
            BranchTransferRequest t = TRANSFER.equals(n.getRelatedType()) && n.getRelatedId() != null ? byId.get(n.getRelatedId()) : null;
            out.add(NotificationDTO.of(n, t == null ? null : transferLink(viewer, t, ownPatientId)));
        }
        return out;
    }

    /** The page this viewer may open for the transfer, or null. Mirrors who can use the transfer screens (see TransferService). */
    static String transferLink(AuthUser viewer, BranchTransferRequest t, Long ownPatientId) {
        return switch (viewer.role()) {
            case ADMIN -> "/staff-dashboard/transfers";
            case BRANCH_MANAGER, RECEPTIONIST -> {
                String branch = viewer.branchId();
                if (branch == null || branch.isBlank()) yield null;
                if (branch.equals(t.getToBranchId())) yield "/staff-dashboard/transfers";
                if (branch.equals(t.getFromBranchId())) yield "/staff-dashboard/transfers/outgoing";
                yield null;
            }
            case PATIENT -> ownPatientId != null && ownPatientId.equals(t.getPatientId()) ? "/dashboard/profile" : null;
            default -> null;
        };
    }

    // ================================================================== producing (called by TransferService)

    /** A transfer was requested (or raised by staff) and now waits for a decision. */
    public void transferRequested(BranchTransferRequest t, Patient p, AuthUser actor) {
        deliver(() -> {
            Map<String, String> names = branchService.nameMap();
            String text = who(p) + " requested a transfer from " + name(names, t.getFromBranchId()) + " to " + name(names, t.getToBranchId())
                    + ". Review it under Transfers.";
            Map<Long, User> to = new LinkedHashMap<>();
            addDeciders(to, t);
            send(to, actor, NotificationType.TRANSFER_REQUESTED, t, u -> text);
        });
    }

    /** A transfer was approved or rejected (t.getStatus() says which). */
    public void transferDecided(BranchTransferRequest t, Patient p, AuthUser actor) {
        if (t.getStatus() != TransferStatus.APPROVED && t.getStatus() != TransferStatus.REJECTED) return;
        deliver(() -> {
            boolean approved = t.getStatus() == TransferStatus.APPROVED;
            Map<String, String> names = branchService.nameMap();
            String from = name(names, t.getFromBranchId()), to = name(names, t.getToBranchId());
            Map<Long, User> recipients = new LinkedHashMap<>();
            User requester = requesterOf(t);
            add(recipients, requester);
            add(recipients, loginOf(p));
            users.findByRoleAndBranchIdAndActiveTrue(Role.BRANCH_MANAGER, t.getFromBranchId()).forEach(u -> add(recipients, u));

            Long patientUserId = loginOf(p) == null ? null : loginOf(p).getId();
            String staffText = "The transfer of " + who(p) + " from " + from + " to " + to + " was " + (approved ? "approved" : "rejected")
                    + (actor.fullName() == null || actor.fullName().isBlank() ? "" : " by " + actor.fullName()) + ".";
            String patientText = "Your request to move from " + from + " to " + to + (approved
                    ? " was approved. You are now registered at " + to + "."
                    : " was not approved. You stay registered at " + from + ".");
            send(recipients, actor, approved ? NotificationType.TRANSFER_APPROVED : NotificationType.TRANSFER_REJECTED, t,
                    u -> u.getId().equals(patientUserId) ? patientText : staffText);
        });
    }

    /** A pending transfer request was withdrawn. */
    public void transferCancelled(BranchTransferRequest t, Patient p, AuthUser actor) {
        deliver(() -> {
            Map<String, String> names = branchService.nameMap();
            String from = name(names, t.getFromBranchId()), to = name(names, t.getToBranchId());
            Map<Long, User> recipients = new LinkedHashMap<>();
            addDeciders(recipients, t);
            add(recipients, requesterOf(t));
            add(recipients, loginOf(p));
            Long patientUserId = loginOf(p) == null ? null : loginOf(p).getId();
            String staffText = "The transfer request for " + who(p) + " from " + from + " to " + to + " was withdrawn.";
            String patientText = "Your request to move from " + from + " to " + to + " was cancelled.";
            send(recipients, actor, NotificationType.TRANSFER_CANCELLED, t, u -> u.getId().equals(patientUserId) ? patientText : staffText);
        });
    }

    // ------------------------------------------------------------------ helpers

    /** Everyone allowed to approve/reject this request: admins, destination managers, destination receptionists if the rule allows. */
    private void addDeciders(Map<Long, User> into, BranchTransferRequest t) {
        users.findByRoleAndActiveTrue(Role.ADMIN).forEach(u -> add(into, u));
        users.findByRoleAndBranchIdAndActiveTrue(Role.BRANCH_MANAGER, t.getToBranchId()).forEach(u -> add(into, u));
        if (!managerApprovalRequired) {
            users.findByRoleAndBranchIdAndActiveTrue(Role.RECEPTIONIST, t.getToBranchId()).forEach(u -> add(into, u));
        }
    }

    private User requesterOf(BranchTransferRequest t) {
        return t.getRequestedBy() == null ? null : users.findByEmail(t.getRequestedBy()).orElse(null);
    }

    private static User loginOf(Patient p) { return p == null ? null : p.getUser(); }

    private static void add(Map<Long, User> into, User u) {
        if (u != null && u.getId() != null && u.isActive()) into.putIfAbsent(u.getId(), u);
    }

    private void send(Map<Long, User> recipients, AuthUser actor, NotificationType type, BranchTransferRequest t,
                      java.util.function.Function<User, String> message) {
        for (User u : recipients.values()) {
            if (actor != null && u.getId().equals(actor.id())) continue;            // never notify the person who did it
            notifications.save(new Notification(u.getId(), type, message.apply(u), TRANSFER, t.getId()));
        }
    }

    private static String who(Patient p) {
        if (p == null) return "A patient";
        String n = p.getFullName() == null || p.getFullName().isBlank() ? "A patient" : p.getFullName().trim();
        return p.getId() == null ? n : n + " (REG-" + p.getId() + ")";
    }

    private static String name(Map<String, String> names, String branchId) {
        String n = names.get(branchId);
        return n != null ? n : branchId;
    }

    /** Runs the work after the surrounding transaction commits (own transaction); immediately when there is none. Never throws. */
    private void deliver(Runnable work) {
        Runnable safe = () -> {
            try {
                work.run();
            } catch (RuntimeException e) {
                log.warn("Could not create notification: {}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { newTransaction.executeWithoutResult(s -> safe.run()); }
                    catch (RuntimeException e) { log.warn("Could not store notification: {}", e.getMessage()); }
                }
            });
        } else {
            safe.run();
        }
    }
}
