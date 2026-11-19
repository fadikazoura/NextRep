import { h } from './dom.js';
import { t } from './i18n.js';
import { COUNTRIES, composePhone, splitPhone } from './countries.js';

/** Country code picker + local number. value() returns the full international number, or '' when empty. */
export function phoneField(id, phone) {
    const initial = splitPhone(phone);
    const country = h('select', { class: 'phone-country', 'aria-label': t('account.country') },
        COUNTRIES.map(([code, flag, dial, name]) =>
            h('option', { value: code, selected: code === initial.country, title: name }, `${flag} ${dial}`)));
    const number = h('input', {
        id, type: 'tel', inputmode: 'tel', autocomplete: 'tel-national', maxlength: 20,
        placeholder: t('account.phonePlaceholder'), value: initial.number,
    });
    return {
        el: h('div', { class: 'phone-field' }, country, number),
        value: () => composePhone(COUNTRIES.find(([code]) => code === country.value)[2], number.value),
    };
}
