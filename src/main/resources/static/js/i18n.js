/** English is the default; Hebrew switches the whole layout to right-to-left. */
const STRINGS = {
    en: {
        'app.tagline': 'Your smart gym coach',
        'app.pitch': 'Personal weekly plans (with AI), workout logging, and it tells you exactly how much to lift next time.',
        'theme.toDark': 'Dark mode',
        'theme.toLight': 'Light mode',
        'lang.switch': 'עב',
        'lang.switchLabel': 'Switch to Hebrew',
        'nav.home': 'Home',
        'nav.plan': 'Plan',
        'nav.workout': 'Workout',
        'nav.progress': 'Progress',
        'nav.profile': 'Profile',
        'auth.login': 'Log in',
        'auth.register': 'Create account',
        'auth.logout': 'Log out',
        'auth.email': 'Email',
        'auth.password': 'Password',
        'auth.passwordHint': 'At least 8 characters',
        'auth.noAccount': 'New here?',
        'auth.haveAccount': 'Already have an account?',
        'auth.loginTitle': 'Welcome back',
        'auth.registerTitle': 'Create your account',
        'auth.sessionExpired': 'Your session expired. Please log in again.',
        'home.hello': 'Hi, {email}',
        'home.subtitle': 'What are we doing today?',
        'home.plan': 'Your weekly plan',
        'home.workout': 'Start a workout',
        'home.progress': 'Records & charts',
        'home.profile': 'Your profile',
        'common.loading': 'Loading…',
        'common.comingSoon': 'Coming in the next step.',
        'common.back': 'Back',
        'errors.network': 'Cannot reach the server. Check your connection and try again.',
        'errors.wakingUp': 'The server is waking up — this can take a minute on the free plan…',
        'errors.invalidCredentials': 'Wrong email or password.',
        'errors.emailTaken': 'This email is already registered.',
        'errors.generic': 'Something went wrong. Please try again.',
        'errors.notFound': 'Page not found.',
    },
    he: {
        'app.tagline': 'המאמן החכם שלך לחדר הכושר',
        'app.pitch': 'תוכנית שבועית אישית (גם עם AI), רישום אימונים, והמלצה מדויקת כמה להרים בפעם הבאה.',
        'theme.toDark': 'מצב כהה',
        'theme.toLight': 'מצב בהיר',
        'lang.switch': 'EN',
        'lang.switchLabel': 'Switch to English',
        'nav.home': 'בית',
        'nav.plan': 'תוכנית',
        'nav.workout': 'אימון',
        'nav.progress': 'התקדמות',
        'nav.profile': 'פרופיל',
        'auth.login': 'התחברות',
        'auth.register': 'יצירת חשבון',
        'auth.logout': 'התנתקות',
        'auth.email': 'אימייל',
        'auth.password': 'סיסמה',
        'auth.passwordHint': 'לפחות 8 תווים',
        'auth.noAccount': 'חדש כאן?',
        'auth.haveAccount': 'כבר יש לך חשבון?',
        'auth.loginTitle': 'ברוך שובך',
        'auth.registerTitle': 'יצירת חשבון',
        'auth.sessionExpired': 'פג תוקף ההתחברות. התחבר שוב.',
        'home.hello': 'היי, {email}',
        'home.subtitle': 'מה עושים היום?',
        'home.plan': 'התוכנית השבועית',
        'home.workout': 'להתחיל אימון',
        'home.progress': 'שיאים וגרפים',
        'home.profile': 'הפרופיל שלך',
        'common.loading': 'טוען…',
        'common.comingSoon': 'יגיע בשלב הבא.',
        'common.back': 'חזרה',
        'errors.network': 'אין חיבור לשרת. בדוק את האינטרנט ונסה שוב.',
        'errors.wakingUp': 'השרת מתעורר — בתוכנית החינמית זה יכול לקחת דקה…',
        'errors.invalidCredentials': 'אימייל או סיסמה שגויים.',
        'errors.emailTaken': 'האימייל הזה כבר רשום.',
        'errors.generic': 'משהו השתבש. נסה שוב.',
        'errors.notFound': 'הדף לא נמצא.',
    },
};

const STORAGE_KEY = 'nextrep.lang';

function readSaved() {
    try {
        return localStorage.getItem(STORAGE_KEY);
    } catch {
        return null;
    }
}

let current = readSaved() === 'he' ? 'he' : 'en';

export function getLang() {
    return current;
}

/** Translates a key; {name} placeholders are filled from vars. Falls back to English, then to the key. */
export function t(key, vars = {}) {
    const text = STRINGS[current][key] ?? STRINGS.en[key] ?? key;
    return text.replace(/\{(\w+)}/g, (_, name) => String(vars[name] ?? ''));
}

export function setLang(lang) {
    current = lang === 'he' ? 'he' : 'en';
    try {
        localStorage.setItem(STORAGE_KEY, current);
    } catch { /* ignore */ }
    document.documentElement.lang = current;
    document.documentElement.dir = current === 'he' ? 'rtl' : 'ltr';
    window.dispatchEvent(new Event('langchange'));
}
