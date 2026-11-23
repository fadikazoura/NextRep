import { test, expect } from '@playwright/test';

const QUESTION = 'I only have 30 minutes today — what should I do?';

/**
 * In CI there is no AI key, so the coach says it's unavailable instead of breaking, and puts the question back in
 * the box to send again; with a key it answers under the question. Either way the coach always replies.
 */
test('the AI coach opens from any page and always replies', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Try the demo' }).click();
    await expect(page.getByText("You're exploring a demo account")).toBeVisible({ timeout: 30_000 });

    await page.locator('#coach-button').click();
    const coach = page.getByRole('dialog', { name: 'NextRep Coach' });
    await expect(coach).toBeVisible();
    await coach.getByRole('button', { name: /30 minutes/ }).click();

    await expect(coach.locator('.coach-msg.from-coach')).toHaveCount(2, { timeout: 40_000 }); // greeting + reply
    await expect(coach.locator('.coach-typing')).toHaveCount(0);
    if (await coach.locator('.coach-msg.is-error').count()) {
        await expect(coach.locator('.coach-msg.is-error')).toContainText(/isn't available|network|questions/);
        await expect(coach.getByRole('textbox')).toHaveValue(QUESTION);
    } else {
        await expect(coach.locator('.coach-msg.from-user')).toHaveText(QUESTION);
    }
});
