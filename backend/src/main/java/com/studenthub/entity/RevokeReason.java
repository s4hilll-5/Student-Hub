package com.studenthub.entity;

/** Why a refresh token stopped being valid. */
public enum RevokeReason {
    ROTATED, LOGOUT, PASSWORD_CHANGED, REUSE_DETECTED, ACCOUNT_DISABLED
}
