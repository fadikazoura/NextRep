/** Country calling codes for the phone field. Israel first (the default), then A–Z. */
export const COUNTRIES = [
    ['IL', '🇮🇱', '+972', 'Israel'],
    ['AE', '🇦🇪', '+971', 'United Arab Emirates'], ['AR', '🇦🇷', '+54', 'Argentina'], ['AT', '🇦🇹', '+43', 'Austria'],
    ['AU', '🇦🇺', '+61', 'Australia'], ['BE', '🇧🇪', '+32', 'Belgium'], ['BR', '🇧🇷', '+55', 'Brazil'],
    ['CA', '🇨🇦', '+1', 'Canada'], ['CH', '🇨🇭', '+41', 'Switzerland'], ['CN', '🇨🇳', '+86', 'China'],
    ['CY', '🇨🇾', '+357', 'Cyprus'], ['DE', '🇩🇪', '+49', 'Germany'], ['DK', '🇩🇰', '+45', 'Denmark'],
    ['EG', '🇪🇬', '+20', 'Egypt'], ['ES', '🇪🇸', '+34', 'Spain'], ['FI', '🇫🇮', '+358', 'Finland'],
    ['FR', '🇫🇷', '+33', 'France'], ['GB', '🇬🇧', '+44', 'United Kingdom'], ['GR', '🇬🇷', '+30', 'Greece'],
    ['IE', '🇮🇪', '+353', 'Ireland'], ['IN', '🇮🇳', '+91', 'India'], ['IT', '🇮🇹', '+39', 'Italy'],
    ['JO', '🇯🇴', '+962', 'Jordan'], ['JP', '🇯🇵', '+81', 'Japan'], ['KR', '🇰🇷', '+82', 'South Korea'],
    ['LB', '🇱🇧', '+961', 'Lebanon'], ['MA', '🇲🇦', '+212', 'Morocco'], ['MX', '🇲🇽', '+52', 'Mexico'],
    ['NL', '🇳🇱', '+31', 'Netherlands'], ['NO', '🇳🇴', '+47', 'Norway'], ['NZ', '🇳🇿', '+64', 'New Zealand'],
    ['PL', '🇵🇱', '+48', 'Poland'], ['PS', '🇵🇸', '+970', 'Palestine'], ['PT', '🇵🇹', '+351', 'Portugal'],
    ['RO', '🇷🇴', '+40', 'Romania'], ['RU', '🇷🇺', '+7', 'Russia'], ['SA', '🇸🇦', '+966', 'Saudi Arabia'],
    ['SE', '🇸🇪', '+46', 'Sweden'], ['TR', '🇹🇷', '+90', 'Turkey'], ['UA', '🇺🇦', '+380', 'Ukraine'],
    ['US', '🇺🇸', '+1', 'United States'], ['ZA', '🇿🇦', '+27', 'South Africa'],
];

/** "+972" + "050-123 4567" -> "+972501234567" (drops spaces, dashes and the leading 0 of local numbers). */
export function composePhone(dialCode, number) {
    const digits = String(number ?? '').replace(/\D/g, '').replace(/^0+/, '');
    return digits ? `${dialCode}${digits}` : '';
}

/** Splits a stored "+972501234567" back into the country and the local number, for editing. */
export function splitPhone(phone) {
    if (!phone) return { country: 'IL', number: '' };
    const match = [...COUNTRIES]
        .sort((a, b) => b[2].length - a[2].length)
        .find(([, , dial]) => phone.startsWith(dial));
    return match ? { country: match[0], number: phone.slice(match[2].length) } : { country: 'IL', number: phone };
}
