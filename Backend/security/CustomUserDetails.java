package com.hotelcraft.security;

import com.hotelcraft.model.Permission;
import com.hotelcraft.model.Role;
import com.hotelcraft.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> auths = new HashSet<>();
        for (Role role : user.getRoles()) {
            // Role names are stored as ROLE_ADMIN, ROLE_USER, etc. — use directly.
            auths.add(new SimpleGrantedAuthority(role.getName()));
            for (Permission p : role.getPermissions()) {
                auths.add(new SimpleGrantedAuthority(p.getName()));
            }
        }
        return auths;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    public User getUser() {
        return user;
    }
}
