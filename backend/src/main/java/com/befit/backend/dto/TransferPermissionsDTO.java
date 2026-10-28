package com.befit.backend.dto;

/**
 * What the logged-in user may do with transfer requests - the screens use it to show only the buttons that will work.
 * It is a convenience for the UI only: the server re-checks every action.
 */
public record TransferPermissionsDTO(boolean canCreate, boolean canDecide, boolean managerApprovalRequired) {}
