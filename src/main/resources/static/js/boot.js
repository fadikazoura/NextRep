// Runs before the page is painted, so the saved theme and language apply without a flash.
(function () {
    var root = document.documentElement;
    var theme = null;
    var lang = null;
    try {
        theme = localStorage.getItem('nextrep.theme');
        lang = localStorage.getItem('nextrep.lang');
    } catch (e) { /* storage unavailable: use defaults */ }
    if (!theme) {
        theme = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    }
    root.setAttribute('data-theme', theme);
    if (lang === 'he') {
        root.lang = 'he';
        root.dir = 'rtl';
    }
})();
