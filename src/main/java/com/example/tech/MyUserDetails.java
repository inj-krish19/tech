package com.example.tech;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class MyUserDetails extends User {
    private Integer authorId;

    public MyUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Integer authorId) {
        super(username, password, authorities);
        this.authorId = authorId;
    }

    public Integer getAuthorId() {
        return authorId;
    }
    
}
