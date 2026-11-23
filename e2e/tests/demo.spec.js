import { test, expect } from '@playwright/test';
import { tab } from './helpers.js';

test('"Try the demo" opens a ready account full of history', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Try the demo' }).click();

    await expect(page.getByText("You're exploring a demo account")).toBeVisible({ timeout: 30_000 });
    const total = page.locator('.stat', { hasText: 'Total workouts' }).locator('strong');
    await expect(total).not.toHaveText('0');

    await tab(page, 'Progress').click();
    await expect(page.getByRole('heading', { name: 'Personal records' })).toBeVisible();
    await expect(page.locator('.pr-big').first()).toBeVisible();
});

test('a demo visitor can leave to create a real account', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Try the demo' }).click();
    await page.getByRole('button', { name: 'Create my account' }).click({ timeout: 30_000 });

    await expect(page).toHaveURL(/#\/register$/);
    await expect(page.locator('#email')).toBeVisible();
});
