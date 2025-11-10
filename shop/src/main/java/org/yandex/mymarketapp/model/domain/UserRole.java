package org.yandex.mymarketapp.model.domain;

import org.springframework.security.core.GrantedAuthority;

public enum UserRole implements GrantedAuthority {

    ADMIN, USER, ANONYMOUS;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
