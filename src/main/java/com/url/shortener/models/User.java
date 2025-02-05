package com.url.shortener.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String username;
    private String password;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude  // Prevent potential infinite recursion in toString
    @EqualsAndHashCode.Exclude  // Prevent potential infinite recursion in equals/hashCode
    private ForgotPassword forgotPassword;

    private String role = "ROLE_USER";
}