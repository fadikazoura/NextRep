import { test, expect } from '@playwright/test';
import { newUser, register } from './helpers.js';

test('a new user is sent to set up their training profile', async ({ page }) => {
    await register(page, newUser());

    await expect(page).toHaveURL(/#\/profile$/);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Save and continue' })).toBeVisible();
});

test('a wrong password shows an error and keeps the user on the login page', async ({ page }) => {
    await page.goto('/#/login');
    await page.locator('#login').fill('nobody-here@example.com');
    await page.locator('#password').fill('definitely-wrong');
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page.getByRole('alert')).toHaveText('Wrong email/username or password.');
    await expect(page).toHaveURL(/#\/login$/);
});

test('the app switches to Hebrew, right to left', async ({ page }) => {
    await page.goto('/#/login');
    await page.locator('#lang-toggle').click();

    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    await expect(page.getByRole('button', { name: 'התחברות' })).toBeVisible();
});
