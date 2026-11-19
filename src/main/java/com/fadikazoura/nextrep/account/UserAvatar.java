package com.fadikazoura.nextrep.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** An uploaded profile picture (small, already resized by the browser). */
@Entity
@Table(name = "user_avatars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {

    @Id
    private Long userId;

    @Column(nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public UserAvatar(Long userId, String contentType, byte[] data) {
        this.userId = userId;
        replace(contentType, data);
    }

    public void replace(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }
}
