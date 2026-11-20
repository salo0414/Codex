<script setup>
import { ref, computed, onMounted, watch } from "vue";
import {
  LayoutDashboard,
  UsersRound,
  CalendarDays,
  Dumbbell,
  UserRound,
  Search,
  Bell,
  Plus,
  ArrowUpRight,
  MapPin,
  ChevronRight,
  MoreHorizontal,
  Activity,
  Menu,
  X,
  LockKeyhole,
  Eye,
  EyeOff,
  LogOut,
} from "lucide-vue-next";

const page = ref("总览"),
  nav = [
    { label: "总览", icon: LayoutDashboard },
    { label: "会员管理", icon: UsersRound },
    { label: "课程管理", icon: CalendarDays },
    { label: "教练团队", icon: UserRound },
    { label: "预约管理", icon: Dumbbell },
    { label: "用户管理", icon: UserRound },
  ];
// 集中保存页面数据、筛选条件和弹窗状态，便于各管理页共享并在变更后统一刷新。
const members = ref([]),
  classes = ref([]),
  trainers = ref([]),
  bookings = ref([]),
  accounts = ref([]),
  stats = ref({}),
  search = ref(""),
  dialog = ref(""),
  mobileOpen = ref(false),
  error = ref("");
const memberForm = ref({
    name: "",
    phone: "",
    membershipType: "月卡",
    expireDate: "",
    status: "正常",
  }),
  classForm = ref({
    name: "",
    category: "力量训练",
    trainer: "",
    startTime: "",
    duration: 60,
    capacity: 20,
    room: "训练室 A",
  }),
  trainerForm = ref({ name: "", specialty: "", phone: "" }),
  bookingForm = ref({ memberId: "", classId: "" });
// 登录状态以服务端会话为准；浏览器本地状态不能替代后端身份校验。
const authenticated = ref(false),
  role = ref(""),
  selectedRole = ref(""),
  loginForm = ref({ username: "", password: "" }),
  loginError = ref(""),
  showPassword = ref(false),
  registerMode = ref(false),
  registerError = ref(""),
  registerForm = ref({ name: "", phone: "", password: "" }),
  profile = ref(null),
  userPage = ref("课程");
const saving = ref(false), successMessage = ref("");
let restoringHistory = false;

// 页面导航写入浏览器历史，让刷新恢复当前位置、后退回到上一管理页。
watch(
  page,
  (currentPage) => {
    if (!authenticated.value || restoringHistory) return;
    history.pushState(
      { fitflowPage: currentPage, successMessage: successMessage.value },
      "",
      `#${encodeURIComponent(currentPage)}`,
    );
  },
  { flush: "sync" },
);

function restorePageFromHistory(state) {
  const savedPage = state?.fitflowPage;
  const allowedPages = [...nav.map((item) => item.label), "操作成功"];
  restoringHistory = true;
  page.value = allowedPages.includes(savedPage) ? savedPage : "总览";
  successMessage.value = state?.successMessage || "";
  restoringHistory = false;
}

function handleBrowserBack(event) {
  if (!authenticated.value) return;
  dialog.value = "";
  mobileOpen.value = false;
  restorePageFromHistory(event.state);
}

async function login() {
  loginError.value = "";
  try {
    const identity = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: loginForm.value.username.trim(),
        password: loginForm.value.password,
        role: selectedRole.value,
      }),
    });
    authenticated.value = true;
    role.value = identity.role;
    history.replaceState(
      { fitflowPage: page.value, successMessage: successMessage.value },
      "",
      `#${encodeURIComponent(page.value)}`,
    );
    loginForm.value.password = "";
    await load();
  } catch (e) {
    loginError.value = e.message;
  }
}
async function register() {
  if (selectedRole.value !== "USER") return;
  registerError.value = "";
  try {
    await api("/auth/register", {
      method: "POST",
      body: JSON.stringify(registerForm.value),
    });
    registerMode.value = false;
    loginForm.value.username = registerForm.value.phone;
    loginForm.value.password = "";
    loginError.value = "注册成功，请登录。会籍激活后即可预约课程。";
  } catch (e) {
    registerError.value = e.message;
  }
}
async function logout() {
  try {
    await api("/auth/logout", { method: "POST" });
  } finally {
    authenticated.value = false;
    role.value = "";
    history.replaceState(null, "", location.pathname);
    page.value = "总览";
    members.value = [];
    classes.value = [];
    trainers.value = [];
    bookings.value = [];
  }
}
// 刷新或重新打开页面时，向后端确认会话，而不是信任客户端缓存。
async function checkSession() {
  try {
    const identity = await api("/auth/me");
    authenticated.value = true;
    role.value = identity.role;
    restorePageFromHistory(history.state);
    await load();
  } catch {
    authenticated.value = false;
  }
}
const visibleMembers = computed(() =>
  members.value.filter((m) =>
    `${m.name}${m.phone}`.toLowerCase().includes(search.value.toLowerCase()),
  ),
);
const upcoming = computed(() =>
  [...classes.value]
    .filter((c) => new Date(c.startTime) > new Date())
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
    .slice(0, 5),
);
const today = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
// 统一处理 API 状态码和响应，确保 401 等错误能反馈到登录或页面提示。
async function api(url, options = {}) {
  const r = await fetch("/api" + url, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
  });
  if (!r.ok) {
    const text = await r.text();
    let message = text || "请求失败";
    try {
      const payload = JSON.parse(text);
      message = payload.message || payload.detail || message;
    } catch {}
    throw new Error(message);
  }
  return r.status === 204 ? null : r.json();
}
// 登录后一次性加载仪表盘及各管理页所需数据。
async function load() {
  try {
    error.value = "";
    if (role.value === "USER") {
      [classes.value, bookings.value, profile.value] = await Promise.all([
        api("/classes"),
        api("/bookings"),
        api("/profile"),
      ]);
      return;
    }
    [
      stats.value,
      members.value,
      classes.value,
      trainers.value,
      bookings.value,
      accounts.value,
    ] = await Promise.all([
      api("/dashboard"),
      api("/members"),
      api("/classes"),
      api("/trainers"),
      api("/bookings"),
      api("/users"),
    ]);
  } catch (e) {
    error.value = "暂时无法连接服务，请确认 Spring Boot、MySQL 和 Redis 已启动。";
  }
}
async function bookClass(classId) {
  try {
    await api("/bookings", { method: "POST", body: JSON.stringify({ classId }) });
    await load();
    userPage.value = "我的预约";
  } catch (e) {
    alert(e.message);
  }
}
async function cancelOwnBooking(id) {
  try {
    await api(`/bookings/${id}`, { method: "DELETE" });
    await load();
  } catch (e) {
    alert(e.message);
  }
}
async function saveProfile() {
  try {
    profile.value = await api("/profile", {
      method: "PUT",
      body: JSON.stringify({ name: profile.value.name, phone: profile.value.phone }),
    });
    alert("个人资料已保存");
  } catch (e) {
    alert(e.message);
  }
}
function open(kind, item) {
  dialog.value = kind;
  if (kind === "member" && item)
    memberForm.value = { ...item, expireDate: item.expireDate || "" };
  if (kind === "class" && item)
    classForm.value = {
      ...item,
      startTime: item.startTime?.slice(0, 16) || "",
    };
  if (kind === "trainer" && item) trainerForm.value = { ...item };
}
// 关闭弹窗时重置表单，避免新增操作沿用上次编辑的数据。
function close() {
  dialog.value = "";
  memberForm.value = {
    name: "",
    phone: "",
    membershipType: "月卡",
    expireDate: "",
    status: "正常",
  };
  classForm.value = {
    name: "",
    category: "力量训练",
    trainer: "",
    startTime: "",
    duration: 60,
    capacity: 20,
    room: "训练室 A",
  };
  trainerForm.value = { name: "", specialty: "", phone: "" };
}
// 会员与课程表单根据是否包含 ID 选择新增或更新接口。
async function saveMember() {
  if (saving.value) return;
  saving.value = true;
  try {
    const f = { ...memberForm.value };
    await api(f.id ? `/members/${f.id}` : "/members", {
      method: f.id ? "PUT" : "POST",
      body: JSON.stringify(f),
    });
    close();
    successMessage.value = f.id ? "会员资料已更新" : "会员已成功添加";
    page.value = "操作成功";
    await load();
  } catch (e) {
    alert(e.message);
  } finally {
    saving.value = false;
  }
}
async function saveClass() {
  try {
    const f = {
      ...classForm.value,
      startTime: new Date(classForm.value.startTime).toISOString(),
    };
    await api(f.id ? `/classes/${f.id}` : "/classes", {
      method: f.id ? "PUT" : "POST",
      body: JSON.stringify(f),
    });
    close();
    await load();
  } catch (e) {
    alert(e.message);
  }
}
async function saveTrainer() {
  try {
    await api("/trainers", {
      method: "POST",
      body: JSON.stringify(trainerForm.value),
    });
    close();
    await load();
  } catch (e) {
    alert(e.message);
  }
}
async function saveBooking() {
  try {
    await api("/bookings", {
      method: "POST",
      body: JSON.stringify(bookingForm.value),
    });
    close();
    await load();
  } catch (e) {
    alert(e.message);
  }
}
async function remove(url) {
  if (confirm("确定删除这条记录吗？"))
    try {
      await api(url, { method: "DELETE" });
      await load();
    } catch (e) {
      alert(e.message);
    }
}
onMounted(() => {
  window.addEventListener("popstate", handleBrowserBack);
  checkSession();
});
</script>

<template>
  <div v-if="!authenticated" class="login-page">
    <div class="login-right">
      <div class="login-mobile-brand">
        <span class="brand-mark"><Activity :size="21" /></span
        ><span class="brand-name">fit<span>flow</span><small>FITNESS STUDIO</small></span>
      </div>
      <section v-if="!selectedRole" class="login-card role-select">
        <div class="login-welcome"><h2>选择登录身份</h2></div>
        <button class="role-choice" @click="selectedRole = 'USER'">
          <span class="role-icon"><UserRound :size="20" /></span>
          <span><b>会员用户</b><small>浏览课程与管理预约</small></span>
          <ChevronRight :size="18" />
        </button>
        <button class="role-choice" @click="selectedRole = 'ADMIN'">
          <span class="role-icon"><LayoutDashboard :size="20" /></span>
          <span><b>管理员</b><small>管理会员、课程与门店运营</small></span>
          <ChevronRight :size="18" />
        </button>
      </section>
      <form v-else class="login-card" @submit.prevent="registerMode ? register() : login()">
        <button type="button" class="role-back" @click="selectedRole = ''; registerMode = false; loginError = ''; registerError = ''">‹ 更换身份</button>
        <div class="login-welcome">
          <h2>{{ registerMode ? "注册会员" : selectedRole === 'ADMIN' ? "管理员登录" : "会员登录" }}</h2>
          <p v-if="registerMode">会员会籍由管理员开通后即可预约课程。</p>
        </div>
        <label v-if="registerMode" class="login-label">姓名
          <div class="login-input"><UserRound :size="17" /><input v-model="registerForm.name" placeholder="姓名" maxlength="80" required /></div>
        </label>
        <label class="login-label">{{ registerMode || selectedRole === 'USER' ? "手机号" : "管理员账号" }}
          <div class="login-input"><UserRound :size="17" /><input v-if="registerMode" v-model="registerForm.phone" type="tel" pattern="1[3-9][0-9]{9}" maxlength="11" autocomplete="username" placeholder="手机号" required /><input v-else-if="selectedRole === 'USER'" v-model="loginForm.username" type="tel" autocomplete="username" placeholder="手机号" required /><input v-else v-model="loginForm.username" autocomplete="username" placeholder="管理员账号" required /></div>
        </label>
        <label class="login-label">密码
          <div class="login-input"><LockKeyhole :size="17" /><input v-if="registerMode" v-model="registerForm.password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" minlength="8" placeholder="至少 8 位" required /><input v-else v-model="loginForm.password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" required /><button type="button" @click="showPassword = !showPassword" :aria-label="showPassword ? '隐藏密码' : '显示密码'"><component :is="showPassword ? EyeOff : Eye" :size="17" /></button></div>
        </label>
        <p v-if="loginError && !registerMode" class="login-error">{{ loginError }}</p>
        <p v-if="registerError && registerMode" class="login-error">{{ registerError }}</p>
        <button class="login-submit" type="submit">{{ registerMode ? "注册" : "登录" }} <ArrowUpRight :size="17" /></button>
        <button v-if="selectedRole === 'USER'" type="button" class="auth-mode-toggle" @click="registerMode = !registerMode; loginError = ''; registerError = ''">{{ registerMode ? "已有账户？返回登录" : "注册会员账户" }}</button>
      </form>
    </div>
  </div>
  <div v-else-if="role === 'USER'" class="user-shell">
    <header class="user-header">
      <a class="brand" href="#" @click.prevent="userPage = '课程'"><span class="brand-mark"><Activity :size="21" /></span><span class="brand-name">fit<span>flow</span><small>MEMBER SPACE</small></span></a>
      <nav><button :class="{active:userPage==='课程'}" @click="userPage='课程'">课程浏览</button><button :class="{active:userPage==='我的预约'}" @click="userPage='我的预约'">我的预约</button><button :class="{active:userPage==='个人资料'}" @click="userPage='个人资料'">个人资料</button></nav>
      <button class="secondary-btn" @click="logout">退出登录</button>
    </header>
    <main class="user-content">
      <div v-if="error" class="error-banner">{{ error }}</div>
      <template v-if="userPage === '课程'">
        <div class="page-heading"><div><div class="eyebrow">FITNESS CLASSES</div><h1>课程浏览</h1><p>选择课程，开启今天的训练。</p></div></div>
        <div class="user-class-grid">
          <article v-for="c in classes" :key="c.id" class="panel user-class-card">
            <div class="eyebrow">{{ c.category || '团体课程' }}</div><h2>{{ c.name }}</h2><p>教练：{{ c.trainer || '待安排' }}</p><p>{{ c.startTime ? new Date(c.startTime).toLocaleString('zh-CN') : '时间待定' }}</p><p>{{ c.room || '训练室' }} · 剩余 {{ Math.max(0, c.capacity - c.booked) }} 个名额</p>
            <button class="primary-btn" :disabled="c.booked >= c.capacity" @click="bookClass(c.id)">{{ c.booked >= c.capacity ? '名额已满' : '预约课程' }} <ChevronRight :size="15" /></button>
          </article>
          <section v-if="!classes.length" class="panel empty-state">暂时没有开放课程。</section>
        </div>
      </template>
      <template v-else-if="userPage === '我的预约'">
        <div class="page-heading"><div><div class="eyebrow">MY BOOKINGS</div><h1>我的预约</h1><p>查看或取消你的课程预约。</p></div></div>
        <section class="panel user-bookings"><article v-for="b in bookings" :key="b.id"><div><b>{{ b.className }}</b><span>预约时间：{{ new Date(b.bookedAt).toLocaleString('zh-CN') }}</span><span>状态：{{ b.status }}</span></div><button class="row-action danger" @click="cancelOwnBooking(b.id)">取消预约</button></article><div v-if="!bookings.length" class="empty-state">你还没有预约课程。</div></section>
      </template>
      <template v-else>
        <div class="page-heading"><div><div class="eyebrow">MY PROFILE</div><h1>个人资料</h1><p>会籍状态：{{ profile?.status }} · {{ profile?.membershipType }}</p></div></div>
        <form v-if="profile" class="panel user-profile" @submit.prevent="saveProfile"><label>姓名<input v-model="profile.name" maxlength="80" required /></label><label>手机号<input v-model="profile.phone" type="tel" pattern="1[3-9][0-9]{9}" maxlength="11" required /></label><label>会籍到期<input :value="profile.expireDate || '尚未开通'" disabled /></label><button class="primary-btn">保存资料 <ChevronRight :size="15" /></button></form>
      </template>
    </main>
  </div>
  <div v-else class="shell">
    <aside class="sidebar" :class="{ 'sidebar-open': mobileOpen }">
      <a
        class="brand"
        href="#"
        @click.prevent="
          page = '总览';
          mobileOpen = false;
        "
        ><span class="brand-mark"><Activity :size="21" /></span
        ><span class="brand-name"
          >fit<span>flow</span><small>FITNESS STUDIO</small></span
        ></a
      >
      <div class="workspace">
        <div class="workspace-icon">F</div>
        <div class="workspace-copy">
          <b>FitFlow Studio</b><span>旗舰店 · 上海</span>
        </div>
        <ChevronRight :size="15" />
      </div>
      <div class="nav-label">工作台</div>
      <nav>
        <button
          v-for="n in nav"
          :key="n.label"
          :class="{ active: page === n.label }"
          @click="
            page = n.label;
            mobileOpen = false;
          "
        >
          <component :is="n.icon" :size="17" /><span>{{ n.label }}</span
          ><i v-if="n.label === '预约管理' && bookings.length">{{
            bookings.length
          }}</i>
        </button>
      </nav>
      <div class="sidebar-bottom">
        <div class="plan-card">
          <div class="plan-top"><span class="plan-dot"></span>PRO 会员方案</div>
          <p>你的门店正在稳定成长。<br />看看本周经营表现吧。</p>
          <button @click="page = '总览'">
            查看经营报告 <ArrowUpRight :size="14" />
          </button>
          <div class="plan-art"><Activity :size="58" /></div>
        </div>
        <button class="profile" @click="logout" title="退出登录">
          <div class="avatar admin">林</div>
          <span><b>林店长</b><small>退出登录</small></span
          ><LogOut :size="17" />
        </button>
      </div>
    </aside>
    <main class="main">
      <header class="topbar">
        <button class="icon-btn mobile-menu" @click="mobileOpen = !mobileOpen">
          <Menu :size="19" />
        </button>
        <div class="crumb">
          <span>FitFlow Studio</span><ChevronRight :size="14" /><b>{{
            page
          }}</b>
        </div>
        <div class="top-actions">
          <span class="date-chip"><CalendarDays :size="15" />{{ today }}</span
          ><button class="icon-btn notification">
            <Bell :size="17" /><i></i>
          </button>
          <div class="avatar admin top-avatar">林</div>
        </div>
      </header>
      <div class="content">
        <div v-if="error" class="error-banner">
          {{ error }} <button @click="load">重试</button>
        </div>
        <template v-if="page === '总览'"
          ><div class="welcome">
            <div>
              <div class="eyebrow"><span></span> MONDAY MOTIVATION</div>
              <h1>早上好，林店长 <span>✦</span></h1>
              <p>新的一天，开启更好的状态。这里是你的门店今日概况。</p>
            </div>
            <button class="primary-btn" @click="open('member')">
              <Plus :size="17" /> 新增会员
            </button>
          </div>
          <div class="stat-grid">
            <article class="stat-card">
              <div class="stat-head">
                <span>会员总数</span
                ><span class="stat-icon green"><UsersRound :size="17" /></span>
              </div>
              <div class="stat-value">
                {{ stats.memberCount ?? "—" }}<small>人</small>
              </div>
              <div class="stat-foot">
                <span class="trend">↗ {{ stats.newToday ?? 0 }} 位</span>
                今日新增会员
              </div>
              <div class="sparkline">
                <svg viewBox="0 0 140 30" preserveAspectRatio="none">
                  <path
                    d="M0 25 C15 22 18 23 30 18 S50 21 62 13 S82 18 97 10 S118 14 140 2"
                  />
                </svg>
              </div>
            </article>
            <article class="stat-card">
              <div class="stat-head">
                <span>有效会员</span
                ><span class="stat-icon blue"><Activity :size="17" /></span>
              </div>
              <div class="stat-value">
                {{ stats.activeMembers ?? "—" }}<small>人</small>
              </div>
              <div class="stat-foot">
                会员状态 <span class="status-live">● 正常</span>
              </div>
              <div class="mini-bars">
                <i
                  v-for="h in [
                    27, 43, 35, 52, 40, 60, 46, 68, 51, 76, 55, 82, 63, 90,
                  ]"
                  :key="h"
                  :style="{ height: h + '%' }"
                ></i>
              </div>
            </article>
            <article class="stat-card">
              <div class="stat-head">
                <span>今日课程</span
                ><span class="stat-icon orange"
                  ><CalendarDays :size="17"
                /></span>
              </div>
              <div class="stat-value">
                {{ stats.classCount ?? "—" }}<small>节</small>
              </div>
              <div class="stat-foot">今日排课 · 持续加载中</div>
              <div class="ring-wrap">
                <div class="ring">
                  <span>{{ stats.classCount || 0 }}</span>
                </div>
                <small>今日课程</small>
              </div>
            </article>
            <article class="stat-card">
              <div class="stat-head">
                <span>在职教练</span
                ><span class="stat-icon purple"><Dumbbell :size="17" /></span>
              </div>
              <div class="stat-value">
                {{ stats.trainerCount ?? "—" }}<small>位</small>
              </div>
              <div class="stat-foot">用专业陪伴每一份坚持</div>
              <div class="coach-stack">
                <span
                  v-for="(t, i) in trainers.slice(0, 4)"
                  :key="t.id"
                  :style="{ '--i': i }"
                  >{{ t.name?.slice(0, 1) }}</span
                ><b>团队协作 · 状态在线</b>
              </div>
            </article>
          </div>
          <div class="dashboard-grid">
            <section class="panel schedule-panel">
              <div class="section-head">
                <div>
                  <div class="eyebrow subtle">ON THE FLOOR</div>
                  <h2>
                    即将开始的课程
                    <span class="count-pill">{{ upcoming.length }}</span>
                  </h2>
                </div>
                <button class="text-link" @click="page = '课程管理'">
                  全部课程 <ArrowUpRight :size="15" />
                </button>
              </div>
              <div v-if="upcoming.length" class="schedule-list">
                <div
                  v-for="(c, i) in upcoming"
                  :key="c.id"
                  class="schedule-row"
                >
                  <div class="time-col">
                    <b>{{
                      new Date(c.startTime).toLocaleTimeString("zh-CN", {
                        hour: "2-digit",
                        minute: "2-digit",
                        hour12: false,
                      })
                    }}</b
                    ><span>{{
                      new Date(c.startTime).toLocaleDateString("zh-CN", {
                        month: "2-digit",
                        day: "2-digit",
                      })
                    }}</span>
                  </div>
                  <div class="schedule-dot" :class="'dot-' + (i % 4)"></div>
                  <div class="class-info">
                    <b>{{ c.name }}</b
                    ><span
                      ><span class="category-dot"></span
                      >{{ c.category || "团体课程" }} ·
                      {{ c.trainer || "待安排" }}</span
                    >
                  </div>
                  <div class="class-room">
                    <MapPin :size="13" />{{ c.room || "训练室" }}
                  </div>
                  <div class="capacity">
                    <div class="capacity-track">
                      <i
                        :style="{
                          width:
                            Math.min(
                              100,
                              ((c.booked || 0) / (c.capacity || 1)) * 100,
                            ) + '%',
                        }"
                      ></i>
                    </div>
                    <span>{{ c.booked || 0 }}/{{ c.capacity }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-state">
                <CalendarDays :size="28" /><b>还没有即将开始的课程</b
                ><span>创建课程后会显示在这里</span
                ><button
                  class="text-link"
                  @click="
                    page = '课程管理';
                    open('class');
                  "
                >
                  创建第一节课 <Plus :size="14" />
                </button>
              </div>
            </section>
            <section class="panel quick-panel">
              <div class="section-head">
                <div>
                  <div class="eyebrow subtle">QUICK ACTIONS</div>
                  <h2>快速操作</h2>
                </div>
                <MoreHorizontal :size="19" />
              </div>
              <div class="quick-list">
                <button @click="open('member')">
                  <span class="quick-icon green"><UsersRound :size="18" /></span
                  ><span
                    ><b>添加新会员</b><small>录入会员信息与卡种</small></span
                  ><ChevronRight :size="16" /></button
                ><button @click="open('class')">
                  <span class="quick-icon orange"
                    ><CalendarDays :size="18" /></span
                  ><span
                    ><b>安排团体课程</b><small>创建课程并设置名额</small></span
                  ><ChevronRight :size="16" /></button
                ><button @click="open('booking')">
                  <span class="quick-icon purple"><Dumbbell :size="18" /></span
                  ><span
                    ><b>新建课程预约</b><small>为会员预约健身课程</small></span
                  ><ChevronRight :size="16" />
                </button>
              </div>
              <div class="tip-card">
                <div class="tip-spark">✳</div>
                <div>
                  <b>小小提醒</b>
                  <p>稳定的训练节奏，<br />来自每一次恰到好处的预约。</p>
                </div>
                <span class="tip-orb"></span>
              </div>
            </section>
          </div>
          <section class="panel activity-panel">
            <div class="section-head">
              <div>
                <div class="eyebrow subtle">RECENTLY JOINED</div>
                <h2>最近加入的会员</h2>
              </div>
              <button class="text-link" @click="page = '会员管理'">
                查看全部 <ArrowUpRight :size="15" />
              </button>
            </div>
            <div v-if="members.length" class="member-strip">
              <div
                v-for="m in [...members].slice(-5).reverse()"
                :key="m.id"
                class="member-tile"
              >
                <div class="avatar" :class="'tone-' + (m.id % 5)">
                  {{ m.name?.slice(0, 1) }}
                </div>
                <div>
                  <b>{{ m.name }}</b
                  ><small
                    >{{ m.membershipType || "普通会员" }} · {{ m.phone }}</small
                  >
                </div>
                <span
                  class="member-status"
                  :class="m.status === '正常' ? 'good' : ''"
                  >{{ m.status }}</span
                >
              </div>
            </div>
            <div v-else class="strip-empty">
              新会员加入后，会展示在这里 <span>✦</span>
            </div>
          </section></template
        >
        <template v-else-if="page === '会员管理'"
          ><div class="page-heading">
            <div>
              <div class="eyebrow">MEMBER DIRECTORY</div>
              <h1>会员管理</h1>
              <p>管理会员资料、会籍状态与到期信息</p>
            </div>
            <button class="primary-btn" @click="open('member')">
              <Plus :size="17" /> 新增会员
            </button>
          </div>
          <section class="panel table-panel">
            <div class="table-toolbar">
              <div>
                <h2>
                  全部会员 <span class="count-pill">{{ members.length }}</span>
                </h2>
                <p>会员档案一览</p>
              </div>
              <label class="search-box"
                ><Search :size="16" /><input
                  v-model="search"
                  placeholder="搜索姓名或手机号"
              /></label>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>会员</th>
                    <th>手机号</th>
                    <th>会籍类型</th>
                    <th>到期日期</th>
                    <th>累计到访</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="m in visibleMembers" :key="m.id">
                    <td>
                      <span class="table-person"
                        ><span class="avatar" :class="'tone-' + (m.id % 5)">{{
                          m.name?.slice(0, 1)
                        }}</span
                        ><b>{{ m.name }}</b></span
                      >
                    </td>
                    <td>{{ m.phone }}</td>
                    <td>
                      <span class="type-pill">{{ m.membershipType }}</span>
                    </td>
                    <td>{{ m.expireDate || "—" }}</td>
                    <td>{{ m.visits || 0 }} 次</td>
                    <td>
                      <span
                        class="status-pill"
                        :class="m.status === '正常' ? 'good' : ''"
                        >{{ m.status }}</span
                      >
                    </td>
                    <td>
                      <button class="row-action" @click="open('member', m)">
                        编辑</button
                      ><button
                        class="row-action danger"
                        @click="remove('/members/' + m.id)"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="!visibleMembers.length">
                    <td colspan="7" class="table-empty">没有找到会员记录</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section></template
        >
        <template v-else-if="page === '课程管理'"
          ><div class="page-heading">
            <div>
              <div class="eyebrow">CLASS SCHEDULE</div>
              <h1>课程管理</h1>
              <p>安排课程时间、教练与训练空间</p>
            </div>
            <button class="primary-btn" @click="open('class')">
              <Plus :size="17" /> 创建课程
            </button>
          </div>
          <section class="panel table-panel">
            <div class="table-toolbar">
              <div>
                <h2>
                  课程安排 <span class="count-pill">{{ classes.length }}</span>
                </h2>
                <p>全部团体课程与训练课</p>
              </div>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>课程名称</th>
                    <th>类型</th>
                    <th>教练</th>
                    <th>开始时间</th>
                    <th>场地</th>
                    <th>预约人数</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="c in [...classes].sort(
                      (a, b) => new Date(a.startTime) - new Date(b.startTime),
                    )"
                    :key="c.id"
                  >
                    <td>
                      <b>{{ c.name }}</b>
                    </td>
                    <td>
                      <span class="type-pill">{{ c.category }}</span>
                    </td>
                    <td>{{ c.trainer || "待安排" }}</td>
                    <td>
                      {{
                        c.startTime
                          ? new Date(c.startTime).toLocaleString("zh-CN", {
                              month: "2-digit",
                              day: "2-digit",
                              hour: "2-digit",
                              minute: "2-digit",
                            })
                          : "—"
                      }}
                    </td>
                    <td>{{ c.room || "—" }}</td>
                    <td>{{ c.booked || 0 }} / {{ c.capacity }}</td>
                    <td>
                      <button class="row-action" @click="open('class', c)">
                        编辑</button
                      ><button
                        class="row-action danger"
                        @click="remove('/classes/' + c.id)"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                  <tr v-if="!classes.length">
                    <td colspan="7" class="table-empty">
                      暂无课程，创建第一节课吧
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section></template
        >
        <template v-else-if="page === '教练团队'"
          ><div class="page-heading">
            <div>
              <div class="eyebrow">MEET THE TEAM</div>
              <h1>教练团队</h1>
              <p>认识与你一起创造改变的专业伙伴</p>
            </div>
            <button class="primary-btn" @click="open('trainer')">
              <Plus :size="17" /> 添加教练
            </button>
          </div>
          <div class="trainer-grid">
            <article
              v-for="(t, i) in trainers"
              :key="t.id"
              class="trainer-card panel"
            >
              <div class="trainer-banner" :class="'banner-' + (i % 4)">
                <div class="trainer-big-avatar" :class="'tone-' + (i % 5)">
                  {{ t.name?.slice(0, 1) }}
                </div>
                <button
                  class="icon-btn trainer-more"
                  @click="remove('/trainers/' + t.id)"
                >
                  <MoreHorizontal :size="19" />
                </button>
                <div class="banner-orb"></div>
              </div>
              <div class="trainer-body">
                <div class="trainer-role">FITNESS COACH</div>
                <h2>{{ t.name }}</h2>
                <div class="specialty-tag">
                  ✦ {{ t.specialty || "综合训练" }}
                </div>
                <div class="trainer-contact">
                  <span><UserRound :size="14" />专业教练</span
                  ><span>{{ t.phone || "联系方式待补充" }}</span>
                </div>
              </div>
            </article>
            <div v-if="!trainers.length" class="panel empty-trainers">
              <Dumbbell :size="30" /><b>教练团队从这里开始</b>
              <p>添加教练资料，让会员认识你的专业伙伴。</p>
              <button class="primary-btn" @click="open('trainer')">
                <Plus :size="16" /> 添加教练
              </button>
            </div>
          </div></template
        >
        <template v-else-if="page === '用户管理'"
          ><div class="page-heading"><div><div class="eyebrow">ACCOUNT ADMINISTRATION</div><h1>用户管理</h1><p>查看注册账户和会员激活情况；在会员管理中设置会籍状态与有效期。</p></div></div>
          <section class="panel table-panel"><div class="table-scroll"><table><thead><tr><th>账户</th><th>角色</th><th>会员档案</th><th>会籍状态</th><th>操作</th></tr></thead><tbody><tr v-for="account in accounts" :key="account.id"><td>{{ account.username }}</td><td>{{ account.role === 'ADMIN' ? '管理员' : '普通用户' }}</td><td>{{ account.memberName }}</td><td><span class="status-pill" :class="account.status === '正常' ? 'good' : ''">{{ account.status }}</span></td><td><button v-if="account.memberId" class="row-action" @click="page='会员管理'; search=account.username">查看会员档案</button><span v-else>系统管理员</span></td></tr><tr v-if="!accounts.length"><td colspan="5" class="table-empty">暂无账户</td></tr></tbody></table></div></section></template
        ><template v-else-if="page === '操作成功'"
          ><section class="success-screen panel">
            <span class="success-check">✓</span>
            <div class="eyebrow">ACTION COMPLETED</div>
            <h1>{{ successMessage }}</h1>
            <p>信息已保存，可以继续管理会员档案。</p>
            <div class="success-actions">
              <button class="primary-btn" @click="page = '会员管理'">
                查看会员档案 <ArrowUpRight :size="16" />
              </button>
              <button class="secondary-btn" @click="page = '总览'">
                返回首页
              </button>
            </div>
          </section></template
        ><template v-else
          ><div class="page-heading">
            <div>
              <div class="eyebrow">CLASS BOOKINGS</div>
              <h1>预约管理</h1>
              <p>跟进课程预约，让每一次训练都有准备</p>
            </div>
            <button class="primary-btn" @click="open('booking')">
              <Plus :size="17" /> 新建预约
            </button>
          </div>
          <section class="panel table-panel">
            <div class="table-toolbar">
              <div>
                <h2>
                  全部预约 <span class="count-pill">{{ bookings.length }}</span>
                </h2>
                <p>会员课程预约记录</p>
              </div>
            </div>
            <div class="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>会员</th>
                    <th>预约课程</th>
                    <th>预约时间</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="b in bookings" :key="b.id">
                    <td>
                      <b>{{ b.memberName || "会员 #" + b.memberId }}</b>
                    </td>
                    <td>{{ b.className || "课程 #" + b.classId }}</td>
                    <td>
                      {{
                        b.bookedAt
                          ? new Date(b.bookedAt).toLocaleString("zh-CN")
                          : "—"
                      }}
                    </td>
                    <td>
                      <span class="status-pill good">{{ b.status }}</span>
                    </td>
                    <td>
                      <button
                        class="row-action danger"
                        @click="remove('/bookings/' + b.id)"
                      >
                        取消预约
                      </button>
                    </td>
                  </tr>
                  <tr v-if="!bookings.length">
                    <td colspan="5" class="table-empty">
                      暂无预约记录，创建一个预约吧
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section></template
        >
        <footer>
          FITFLOW STUDIO <span>·</span> 把每一次坚持，变成更好的自己
          <span class="footer-right">MADE FOR MOVEMENT ✦</span>
        </footer>
      </div>
    </main>
    <div v-if="dialog" class="modal-backdrop" @click.self="close">
      <form
        class="modal"
        @submit.prevent="
          dialog === 'member'
            ? saveMember()
            : dialog === 'class'
              ? saveClass()
              : dialog === 'trainer'
                ? saveTrainer()
                : saveBooking()
        "
      >
        <div class="modal-head">
          <div>
            <div class="eyebrow">
              {{
                dialog === "member"
                  ? "MEMBER PROFILE"
                  : dialog === "class"
                    ? "CLASS SCHEDULE"
                    : dialog === "trainer"
                      ? "COACH PROFILE"
                      : "CLASS BOOKING"
              }}
            </div>
            <h2>
              {{
                dialog === "member"
                  ? memberForm.id
                    ? "编辑会员"
                    : "新增会员"
                  : dialog === "class"
                    ? classForm.id
                      ? "编辑课程"
                      : "创建课程"
                    : dialog === "trainer"
                      ? "添加教练"
                      : "新建预约"
              }}
            </h2>
          </div>
          <button type="button" class="icon-btn" @click="close">
            <X :size="19" />
          </button>
        </div>
        <template v-if="dialog === 'member'"
          ><label
            >姓名<input
              v-model="memberForm.name"
              required
              maxlength="80"
              placeholder="请输入会员姓名" /></label
          ><label
            >手机号码<input
              v-model="memberForm.phone"
              type="tel"
              required
              minlength="11"
              maxlength="11"
              pattern="1[3-9][0-9]{9}"
              title="请输入有效的 11 位手机号"
              inputmode="numeric"
              placeholder="请输入手机号"
          /></label>
          <div class="form-row">
            <label
              >会籍类型<select v-model="memberForm.membershipType">
                <option>月卡</option>
                <option>季卡</option>
                <option>半年卡</option>
                <option>年卡</option>
                <option>次卡</option>
              <option>未开通</option>
              </select></label
            ><label
              >会籍到期<input v-model="memberForm.expireDate" type="date"
            /></label>
          </div>
          <label
            >状态<select v-model="memberForm.status">
              <option>正常</option>
              <option>已过期</option>
              <option>暂停</option>
              <option>待激活</option>
              <option>停用</option>
            </select></label
          ></template
        ><template v-else-if="dialog === 'class'"
          ><label
            >课程名称<input
              v-model="classForm.name"
              required
              placeholder="例如：晨间瑜伽"
          /></label>
          <div class="form-row">
            <label
              >课程类型<select v-model="classForm.category">
                <option>力量训练</option>
                <option>瑜伽</option>
                <option>动感单车</option>
                <option>普拉提</option>
                <option>搏击</option>
                <option>有氧训练</option>
              </select></label
            ><label
              >授课教练<select v-model="classForm.trainer">
                <option value="">待安排</option>
                <option v-for="t in trainers" :key="t.id">{{ t.name }}</option>
              </select></label
            >
          </div>
          <div class="form-row">
            <label
              >开始时间<input
                v-model="classForm.startTime"
                type="datetime-local"
                required /></label
            ><label
              >课程时长（分钟）<input
                v-model.number="classForm.duration"
                type="number"
                min="15"
            /></label>
          </div>
          <div class="form-row">
            <label
              >课程名额<input
                v-model.number="classForm.capacity"
                type="number"
                min="1" /></label
            ><label
              >训练场地<input v-model="classForm.room" placeholder="训练室 A"
            /></label></div></template
        ><template v-else-if="dialog === 'trainer'"
          ><label
            >教练姓名<input
              v-model="trainerForm.name"
              required
              placeholder="请输入教练姓名" /></label
          ><label
            >擅长领域<input
              v-model="trainerForm.specialty"
              placeholder="例如：力量训练、瑜伽" /></label
          ><label
            >手机号码<input
              v-model="trainerForm.phone"
              placeholder="请输入手机号" /></label></template
        ><template v-else
          ><label
            >选择会员<select v-model="bookingForm.memberId" required>
              <option disabled value="">请选择会员</option>
              <option v-for="m in members" :key="m.id" :value="m.id">
                {{ m.name }} · {{ m.phone }}
              </option>
            </select></label
          ><label
            >选择课程<select v-model="bookingForm.classId" required>
              <option disabled value="">请选择课程</option>
              <option
                v-for="c in classes.filter((c) => c.booked < c.capacity)"
                :key="c.id"
                :value="c.id"
              >
                {{ c.name }} ·
                {{ new Date(c.startTime).toLocaleString("zh-CN") }}
              </option>
            </select></label
          >
          <p v-if="!members.length || !classes.length" class="form-note">
            请先添加会员与课程，再创建预约。
          </p></template
        >
        <div class="modal-actions">
          <button type="button" class="secondary-btn" @click="close">
            取消</button
          ><button type="submit" class="primary-btn" :disabled="saving">
            {{
              saving
                ? "正在保存…"
                : dialog === "member" && memberForm.id
                  ? "保存修改"
                  : dialog === "class" && classForm.id
                    ? "保存修改"
                    : "确认创建"
            }}
            <ChevronRight :size="15" />
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
