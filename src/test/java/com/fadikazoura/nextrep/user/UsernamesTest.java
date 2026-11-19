package com.fadikazoura.nextrep.user;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UsernamesTest {

    @Test
    void buildsUsernameFromEmail() {
        assertThat(Usernames.fromEmail("Fadi.K+gym@example.com", taken -> false)).isEqualTo("fadi.kgym");
    }

    @Test
    void addsDigitsWhenTheNameIsTaken() {
        String name = Usernames.fromEmail("fadi@example.com", Set.of("fadi")::contains);
        assertThat(name).startsWith("fadi_").matches(Usernames.PATTERN);
    }

    @Test
    void padsVeryShortNames() {
        assertThat(Usernames.fromEmail("a@example.com", taken -> false)).matches(Usernames.PATTERN);
    }
}
