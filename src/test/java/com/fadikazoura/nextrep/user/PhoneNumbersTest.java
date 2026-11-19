package com.fadikazoura.nextrep.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumbersTest {

    @Test
    void normalizesToInternationalFormat() {
        assertThat(PhoneNumbers.normalize("+972 52-555-1234")).isEqualTo("+972525551234");
        assertThat(PhoneNumbers.normalize("+1 (650) 253-0000")).isEqualTo("+16502530000");
    }

    @Test
    void emptyMeansNoPhone() {
        assertThat(PhoneNumbers.normalize(null)).isNull();
        assertThat(PhoneNumbers.normalize("  ")).isNull();
    }

    @Test
    void rejectsNumbersThatCannotExist() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("+972 12")).isInstanceOf(InvalidPhoneNumberException.class);
        // right length, but Israel never allocated 050-123-xxxx: libphonenumber knows the real number ranges
        assertThatThrownBy(() -> PhoneNumbers.normalize("+972 50-123-4567")).isInstanceOf(InvalidPhoneNumberException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("0501234567")).isInstanceOf(InvalidPhoneNumberException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("hello")).isInstanceOf(InvalidPhoneNumberException.class);
    }
}
