package com.fadikazoura.nextrep.user;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

/**
 * Validates phone numbers with Google's libphonenumber (knows the numbering rules of every country)
 * and stores them in the international E.164 format: "+972 50-123-4567" -> "+972501234567".
 */
public final class PhoneNumbers {

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private PhoneNumbers() {
    }

    /** @return the number in E.164 format, or null when the input is empty (the phone is optional) */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            var number = UTIL.parse(input.trim(), null); // null region: the number must start with + and a country code
            if (!UTIL.isValidNumber(number)) throw new InvalidPhoneNumberException();
            return UTIL.format(number, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new InvalidPhoneNumberException();
        }
    }
}
