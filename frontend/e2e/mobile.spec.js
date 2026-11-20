import { test, expect } from "@playwright/test";

test("移动视口登录后无横向溢出", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: /管理员/ }).click();
  await page.getByPlaceholder("管理员账号").fill(process.env.E2E_USERNAME || "admin");
  if (!process.env.E2E_PASSWORD) throw new Error("请设置 E2E_PASSWORD 环境变量");
  await page.getByPlaceholder("请输入密码").fill(process.env.E2E_PASSWORD);
  await page.getByRole("button", { name: /^登录/ }).click();
  await expect(page.getByRole("heading", { name: /早上好/ })).toBeVisible();
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    .toBe(true);
});
