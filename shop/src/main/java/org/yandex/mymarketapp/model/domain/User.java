package org.yandex.mymarketapp.model.domain;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Table("users")
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    private Long id;

    @NotNull
    @Column("username")
    private String username;

    @NotNull
    @Column("password")
    private String password;

    @NotNull
    @Column("role")
    private UserRole role;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(role);
    }
}
