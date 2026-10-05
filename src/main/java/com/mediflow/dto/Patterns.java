package com.mediflow.dto;

/** Shared validation patterns so every DTO validates the same way. */
public final class Patterns {

    private Patterns() {}

    /** Optional Sri Lankan phone: 0XXXXXXXXX or +94XXXXXXXXX (9 digits after the prefix, first digit 1-9). */
    public static final String PHONE = "^$|^(?:0|\\+94)[1-9][0-9]{8}$";
    public static final String PHONE_MSG = "Phone must be a valid Sri Lankan number, e.g. 0771234567 or +94771234567";

    /** Optional e-mail that must contain a domain with a TLD (a@b is rejected). */
    public static final String EMAIL = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String EMAIL_MSG = "Enter a valid email address, e.g. name@example.com";

    /** Optional free-text name that must contain at least one letter (rejects "123", "---"). */
    public static final String HAS_LETTER = "^$|^.*\\p{L}.*$";
    public static final String HAS_LETTER_MSG = "Must contain at least one letter";

    /** Person name: letters (any language), spaces, dot, apostrophe, hyphen. */
    public static final String PERSON_NAME = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*$";
    public static final String PERSON_NAME_MSG = "Name may contain only letters, spaces, . ' and -";

    /** Normalises +94771234567 -> 0771234567 so phones are stored/searched in one format. */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String p = phone.trim();
        return p.startsWith("+94") ? "0" + p.substring(3) : p;
    }
}
