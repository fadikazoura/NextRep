import { test, expect } from '@playwright/test';

/**
 * In CI there is no AI key, so the coach must say it's unavailable instead of breaking;
 * with a key it answers. Either way the conversation gets a reply from the coach.
 */
test('the AI coach opens from any page and always replies', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Try the demo' }).click();
    await expect(page.getByText("You're exploring a demo account")).toBeVisible({ timeout: 30_000 });

    await page.locator('#coach-button').click();
    const coach = page.getByRole('dialog', { name: 'NextRep Coach' });
    await expect(coach).toBeVisible();
    await coach.getByRole('button', { name: /30 minutes/ }).click();

    await expect(coach.locator('.coach-msg.from-user')).toHaveText("I only have 30 minutes today — what should I do?");
    await expect(coach.locator('.coach-msg.from-coach')).toHaveCount(2, { timeout: 40_000 }); // greeting + reply
    await expect(coach.locator('.coach-typing')).toHaveCount(0);
});
