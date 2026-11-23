import { test, expect } from '@playwright/test';
import { newUser, register, skipRest, tab } from './helpers.js';

test('the core journey: profile, plan, a logged workout, and the new record', async ({ page }) => {
    await register(page, newUser());

    // 1. Training profile (only the required fields that have no default)
    await expect(page).toHaveURL(/#\/profile$/);
    await page.locator('#birthDate').fill('1998-04-12');
    await page.locator('#heightCm').fill('178');
    await page.getByRole('button', { name: 'Save and continue' }).click();

    // 2. A plan from the rule-based generator (instant; no AI in tests)
    await expect(page).toHaveURL(/#\/plan$/);
    await page.getByRole('button', { name: 'Use the classic plan' }).click();
    const firstDay = page.getByRole('link', { name: 'Start', exact: true }).first();
    await expect(firstDay).toBeVisible();

    // 3. Start that day's workout and log one set of the first exercise
    await firstDay.click();
    await expect(page).toHaveURL(/#\/workout$/);
    const card = page.locator('article.card').first();
    const exercise = (await card.locator('.ex-title').innerText()).trim();
    const [weight, reps] = await card.locator('.set-form input').all();
    await weight.fill('40');
    await reps.fill('10');
    await card.getByRole('button', { name: 'Log set' }).click();
    await expect(card.locator('.set-list li')).toHaveCount(1);
    await expect(card.locator('.set-value')).toHaveText('40 kg × 10');
    await skipRest(page);

    // 4. Finish: the summary counts the set
    await page.getByRole('button', { name: 'Finish workout' }).click();
    await expect(page.getByRole('heading', { name: 'Workout complete!' })).toBeVisible();

    // 5. The record shows up on the progress page
    await tab(page, 'Progress').click();
    await expect(page.getByRole('heading', { name: 'Personal records' })).toBeVisible();
    const record = page.locator('article.card', { hasText: exercise });
    await expect(record).toBeVisible();
    await expect(record).toContainText('40 kg × 10');
});
