package com.fadikazoura.nextrep.account;

import java.util.Set;

/**
 * Checks uploaded avatar pictures by their first bytes ("magic numbers"), not by the file name or the
 * Content-Type the client claims — both are easy to fake.
 */
final class AvatarImages {

    static final int MAX_BYTES = 200 * 1024;

    /** The built-in avatars the web app offers; the drawings themselves live in the frontend. */
    static final Set<String> PRESETS = Set.of(
            "dumbbell", "kettlebell", "barbell", "flame", "bolt", "trophy",
            "medal", "heart", "target", "timer", "crown", "rocket");

    private AvatarImages() {
    }

    /** @return the real content type of the picture */
    static String detectContentType(byte[] data) {
        if (data == null || data.length < 12) throw new InvalidAvatarException("The file is empty or too small");
        if (data.length > MAX_BYTES) throw new InvalidAvatarException("The picture must be at most 200 KB");
        if (startsWith(data, 0, 0x89, 'P', 'N', 'G')) return "image/png";
        if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P')) return "image/webp";
        throw new InvalidAvatarException("Only PNG, JPEG or WebP pictures are allowed");
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }
}
