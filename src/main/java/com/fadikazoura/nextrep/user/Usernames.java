package com.fadikazoura.nextrep.user;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** Username rules in one place: 3–20 characters, English letters, digits, '.' and '_'. */
public final class Usernames {

    public static final String PATTERN = "^[A-Za-z0-9._]{3,20}$";
    public static final String MESSAGE = "3–20 characters: English letters, digits, '.' or '_'";

    private Usernames() {
    }

    /**
     * Builds a free username from an email ("Fadi.K@x.com" -> "fadi.k"), adding digits when it is taken.
     * Used when an API client registers without choosing one.
     */
    public static String fromEmail(String email, Predicate<String> taken) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._]", "");
        if (base.length() < 3) base = (base + "user").substring(0, Math.max(3, base.length()));
        base = base.length() > 14 ? base.substring(0, 14) : base;
        String candidate = base;
        while (taken.test(candidate)) {
            candidate = base + "_" + ThreadLocalRandom.current().nextInt(1000, 100000);
        }
        return candidate;
    }
}
