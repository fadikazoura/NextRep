package com.fadikazoura.nextrep.user;

import java.time.Instant;

/** {@code demo} is true for a temporary demo account (the UI shows a "create your own account" banner). */
public record UserResponse(Long id, String username, String email, String phone, Avatar avatar, Instant createdAt,
                           boolean demo) {

    /** kind: INITIALS (no picture), PRESET (one of the built-in avatars, named in {@code preset}) or PHOTO (uploaded). */
    public record Avatar(String kind, String preset) {

        static Avatar of(String avatarPreset) {
            if (avatarPreset == null) return new Avatar("INITIALS", null);
            if (User.PHOTO_AVATAR.equals(avatarPreset)) return new Avatar("PHOTO", null);
            return new Avatar("PRESET", avatarPreset);
        }
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(),
                Avatar.of(user.getAvatarPreset()), user.getCreatedAt(), user.isDemo());
    }
}
