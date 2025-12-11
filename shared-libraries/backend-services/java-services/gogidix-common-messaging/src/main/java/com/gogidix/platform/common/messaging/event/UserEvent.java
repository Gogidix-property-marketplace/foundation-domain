package com.gogidix.platform.common.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * User-related domain events
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserEvent extends DomainEvent {

    public enum UserEventType {
        USER_REGISTERED,
        USER_UPDATED,
        USER_DELETED,
        USER_VERIFIED,
        USER_SUSPENDED,
        USER_ACTIVATED,
        USER_LOGIN_SUCCESS,
        USER_LOGIN_FAILED,
        USER_PASSWORD_CHANGED,
        USER_PROFILE_UPDATED
    }

    private UserEventType userEventType;
    private String userId;
    private String username;
    private String email;
    private String userType;
    private String status;
    private String roles;
}
