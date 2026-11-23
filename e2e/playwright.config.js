// @ts-check
import { defineConfig, devices } from '@playwright/test';

/**
 * The tests run against a server that is already up (locally: ./mvnw spring-boot:run; in CI: the built jar).
 * BASE_URL can point them at any environment, e.g. BASE_URL=https://nextrep-07r6.onrender.com npm test
 */
export default defineConfig({
    testDir: './tests',
    timeout: 60_000,
    expect: { timeout: 10_000 },
    fullyParallel: true,
    forbidOnly: Boolean(process.env.CI),
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list'], ['html', { open: 'never' }]],
    use: {
        baseURL: process.env.BASE_URL ?? 'http://localhost:8080',
        locale: 'en-US',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
    projects: [
        { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
        { name: 'phone', use: { ...devices['Pixel 7'] } },
    ],
});
