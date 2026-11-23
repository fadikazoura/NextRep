package com.fadikazoura.nextrep.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public name shown in the app; unique ignoring upper/lower case. */
    @Column(nullable = false, length = 20)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** Optional, stored in international E.164 format, e.g. +972501234567. */
    @Column(length = 16)
    private String phone;

    /** null = initials avatar, {@link #PHOTO_AVATAR} = uploaded picture, otherwise the name of a preset avatar. */
    @Column(length = 30)
    private String avatarPreset;

    /** Every token carries this number; increasing it logs the user out on every device. */
    @Column(nullable = false)
    private int tokenVersion;

    @Column(nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Set only for demo accounts: after this moment the account and all its data are deleted. */
    private Instant demoExpiresAt;

    public static final String PHOTO_AVATAR = "photo";

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void changeUsername(String username) {
        this.username = username;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void changeAvatar(String avatarPreset) {
        this.avatarPreset = avatarPreset;
    }

    /** New password: the hash changes and every existing token stops working. */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        revokeTokens();
    }

    public void revokeTokens() {
        this.tokenVersion++;
    }

    public void markAsDemo(Instant expiresAt) {
        this.demoExpiresAt = expiresAt;
    }

    public boolean isDemo() {
        return demoExpiresAt != null;
    }
}
