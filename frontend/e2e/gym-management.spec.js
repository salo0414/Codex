import { test, expect } from "@playwright/test";

const username = process.env.E2E_USERNAME || "admin";
const password = process.env.E2E_PASSWORD;

function uniquePhone() {
  return `139${Date.now().toString().slice(-8)}`;
}

async function login(page) {
  if (!password) throw new Error("请设置 E2E_PASSWORD 环境变量");
  await page.goto("/");
  await page.getByRole("button", { name: /管理员/ }).click();
  await page.getByPlaceholder("管理员账号").fill(username);
  await page.getByPlaceholder("请输入密码").fill(password);
  await page.getByRole("button", { name: /^登录/ }).click();
  await expect(page.getByRole("heading", { name: /早上好/ })).toBeVisible();
}

async function removeMember(page, phone) {
  await page.evaluate(async (value) => {
    const response = await fetch("/api/members");
    if (!response.ok) return;
    const members = await response.json();
    const member = members.find((item) => item.phone === value);
    if (member) {
      await fetch(`/api/members/${member.id}`, { method: "DELETE" });
    }
  }, phone);
}

test("新浏览器登录后新增会员，成功页、刷新和返回后数据仍一致", async ({ page }) => {
  const phone = uniquePhone();
  const memberName = `E2E <script>alert(1)</script> & 检查 ${phone}`;
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "选择登录身份" })).toBeVisible();
  await login(page);

  try {
    await page.getByRole("button", { name: /添加新会员/ }).click();
    await page.getByLabel("姓名").fill(memberName);
    await page.getByLabel("手机号码").fill(phone);
    await page.getByRole("button", { name: "确认创建" }).click();

    await expect(page.getByRole("heading", { name: "会员已成功添加" })).toBeVisible();
    await page.reload();
    await expect(page.getByRole("heading", { name: "会员已成功添加" })).toBeVisible();
    await page.getByRole("button", { name: "会员管理" }).click();
    const memberRow = page.getByRole("row").filter({ hasText: phone });
    await expect(memberRow.getByText(memberName)).toBeVisible();
    await expect(memberRow.locator("script")).toHaveCount(0);

    await page.goBack();
    await expect(page.getByRole("heading", { name: "会员已成功添加" })).toBeVisible();
  } finally {
    await page.goto("/");
    await removeMember(page, phone);
  }
});

test("会员表单拦截空值与错误格式，后端拒绝无效、超长和重复数据", async ({ page }) => {
  const phone = uniquePhone();
  await login(page);
  await page.getByRole("button", { name: "会员管理" }).click();
  await page.getByRole("button", { name: /新增会员/ }).click();

  const name = page.getByLabel("姓名");
  const phoneInput = page.getByLabel("手机号码");
  await page.getByRole("button", { name: "确认创建" }).click();
  await expect(name).toBeVisible();
  await expect(name).toHaveAttribute("required", "");
  await name.fill("测试会员");
  await phoneInput.fill("not-a-phone");
  await expect(phoneInput).toHaveJSProperty("validity.valid", false);
  await page.getByRole("button", { name: "确认创建" }).click();
  await expect(page.getByRole("heading", { name: "新增会员" })).toBeVisible();
  await page.getByRole("button", { name: "取消" }).click();

  const results = await page.evaluate(async (validPhone) => {
    const send = async (name, phone) => {
      const response = await fetch("/api/members", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, phone }),
      });
      return { status: response.status, body: await response.json().catch(() => ({})) };
    };
    return {
      invalid: await send("无效手机号检查", "abc"),
      tooLong: await send("超长".repeat(41), `${validPhone.slice(0, 10)}8`),
      created: await send("重复提交检查", validPhone),
      duplicate: await send("重复提交检查", validPhone),
    };
  }, phone);

  expect(results.invalid.status).toBe(400);
  expect(results.invalid.body.message).toContain("手机号");
  expect(results.tooLong.status).toBe(400);
  expect(results.created.status).toBe(200);
  expect(results.duplicate.status).toBe(409);
  await removeMember(page, phone);
});

test("接口返回 500 时显示错误提示且表单保留", async ({ page }) => {
  await login(page);
  await page.getByRole("button", { name: /添加新会员/ }).click();
  await page.getByLabel("姓名").fill("接口失败检查");
  await page.getByLabel("手机号码").fill(uniquePhone());
  await page.route("**/api/members", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ message: "模拟接口异常" }),
      });
    } else {
      await route.continue();
    }
  });

  const dialog = page.waitForEvent("dialog");
  await page.getByRole("button", { name: "确认创建" }).click();
  const alert = await dialog;
  expect(alert.message()).toContain("模拟接口异常");
  await alert.accept();
  await expect(page.getByRole("heading", { name: "新增会员" })).toBeVisible();
  await expect(page.getByLabel("姓名")).toHaveValue("接口失败检查");
});

test("登录前身份分流，管理员入口没有注册功能且角色不能冒用", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "选择登录身份" })).toBeVisible();

  await page.getByRole("button", { name: /会员用户/ }).click();
  await expect(page.getByRole("button", { name: "注册会员账户" })).toBeVisible();
  await page.getByRole("button", { name: /更换身份/ }).click();

  await page.getByRole("button", { name: /管理员/ }).click();
  await expect(page.getByRole("button", { name: "注册会员账户" })).toHaveCount(0);
  await page.getByPlaceholder("管理员账号").fill(username);
  await page.getByPlaceholder("请输入密码").fill(password);
  await page.getByRole("button", { name: /^登录/ }).click();
  await expect(page.getByRole("heading", { name: /早上好/ })).toBeVisible();

  const wrongRoleStatus = await page.evaluate(async (credentials) => {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...credentials, role: "USER" }),
    });
    return response.status;
  }, { username, password });
  expect(wrongRoleStatus).toBe(403);
  await expect(page.getByRole("heading", { name: /早上好/ })).toBeVisible();
});
