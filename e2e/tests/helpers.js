import { expect } from '@playwright/test';

/** A unique user per test, so tests never depend on each other or on old data. */
export function newUser() {
    const id = `${Date.now().toString(36)}${Math.floor(Math.random() * 1e6).toString(36)}`;
    return { username: `e2e_${id}`.slice(0, 20), email: `e2e-${id}@example.com`, password: 'e2e-password-123' };
}

export async function register(page, user) {
    await page.goto('/#/register');
    await page.locator('#username').fill(user.username);
    await page.locator('#email').fill(user.email);
    await page.locator('#password').fill(user.password);
    await page.getByRole('button', { name: 'Create account' }).click();
}

/** The tab bar at the bottom of the app. */
export function tab(page, name) {
    return page.locator('#tabbar').getByRole('link', { name });
}

/** The rest timer slides up after every set; dismiss it so it doesn't cover the buttons under it. */
export async function skipRest(page) {
    const skip = page.getByRole('button', { name: 'Skip rest' });
    if (await skip.isVisible()) await skip.click();
}

export async function expectOnPage(page, hash) {
    await expect(page).toHaveURL(new RegExp(`#${hash.replace('/', '\\/')}$`));
}
