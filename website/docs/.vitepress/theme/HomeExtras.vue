<script setup lang="ts">
// The home page's live parts, placed into the default theme's home slots (see index.ts):
//   badge  - the latest version, above the name
//   stats  - stars, downloads, forks and license, under the hero buttons
//   credit - who makes AxManager, and a nudge to star it, after the features
// Each shows what it can before (or without) the GitHub API answering.
import { computed, onMounted, ref } from "vue";
import { useData, withBase } from "vitepress";
import { OWNER, REPO_PAGE, author, latestOf, releases, repo, type Release, type Repo, type User } from "./github";

defineProps<{ part: "badge" | "stats" | "credit" }>();

const PROFILE = `https://github.com/${OWNER}`;
const AVATAR = `${PROFILE}.png?size=160`;
const AUTHOR_NAME = "FahrezRB";

const STRINGS = {
  en: {
    download: "/download",
    isOut: "is out",
    whatsNew: "See what's new",
    stars: "stars",
    downloads: "downloads",
    forks: "forks",
    madeBy: "Made with ♥ by",
    about: "AxManager is designed, built and maintained by one developer. Every feature, the server, the app and this site, comes from the same hands.",
    followers: "followers",
    repos: "repositories",
    follow: "Follow on GitHub",
    enjoying: "Enjoying AxManager?",
    starAsk: "A star on GitHub takes one click, costs nothing, and helps more people find the project. It also tells me the work is worth continuing.",
    star: "Star on GitHub",
  },
  zh: {
    download: "/zh/download",
    isOut: "已发布",
    whatsNew: "查看更新内容",
    stars: "星标",
    downloads: "次下载",
    forks: "复刻",
    madeBy: "用 ♥ 打造 · 作者",
    about: "AxManager 由一位开发者独立设计、开发和维护。服务端、应用和这个网站，全都出自同一双手。",
    followers: "关注者",
    repos: "仓库",
    follow: "在 GitHub 上关注",
    enjoying: "喜欢 AxManager 吗？",
    starAsk: "在 GitHub 上点一个星标只需一下，不花任何成本，却能让更多人发现这个项目，也让我知道这份工作值得继续。",
    star: "在 GitHub 上点星标",
  },
};

const { lang } = useData();
const t = computed(() => (lang.value.startsWith("zh") ? STRINGS.zh : STRINGS.en));

const latest = ref<Release>();
const totalDownloads = ref<number>();
const repoInfo = ref<Repo>();
const user = ref<User>();

function compact(n: number) {
  return new Intl.NumberFormat(lang.value, { notation: "compact", maximumFractionDigits: 1 }).format(n);
}

onMounted(() => {
  // Independent: a failed call only hides the numbers it would have filled.
  releases()
    .then((list) => {
      latest.value = latestOf(list);
      totalDownloads.value = list.reduce((sum, r) => sum + r.assets.reduce((s, a) => s + a.download_count, 0), 0);
    })
    .catch(() => {});
  repo().then((r) => (repoInfo.value = r)).catch(() => {});
  author().then((u) => (user.value = u)).catch(() => {});
});
</script>

<template>
  <a v-if="part === 'badge' && latest" class="ax-badge" :href="withBase(t.download)">
    <span class="ax-badge-dot" />
    <span><b>{{ latest.tag_name }}</b> {{ t.isOut }}</span>
    <span class="ax-badge-more">{{ t.whatsNew }} →</span>
  </a>

  <div v-else-if="part === 'stats'" class="ax-stats">
    <a class="ax-stat" :href="REPO_PAGE" target="_blank" rel="noopener">
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" /></svg>
      <b v-if="repoInfo">{{ compact(repoInfo.stargazers_count) }}</b> {{ t.stars }}
    </a>
    <a v-if="totalDownloads" class="ax-stat" :href="withBase(t.download)">
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" /></svg>
      <b>{{ compact(totalDownloads) }}</b> {{ t.downloads }}
    </a>
    <a v-if="repoInfo" class="ax-stat" :href="`${REPO_PAGE}/forks`" target="_blank" rel="noopener">
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M14 4l2.29 2.29-2.88 2.88 1.42 1.42 2.88-2.88L20 10V4h-6zm-4 0H4v6l2.29-2.29 4.71 4.7V20h2v-8.41l-5.29-5.3L10 4z" /></svg>
      <b>{{ compact(repoInfo.forks_count) }}</b> {{ t.forks }}
    </a>
    <a v-if="repoInfo?.license" class="ax-stat" :href="`${REPO_PAGE}/blob/main/LICENSE`" target="_blank" rel="noopener">
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" /></svg>
      {{ repoInfo.license.spdx_id }}
    </a>
  </div>

  <section v-else-if="part === 'credit'" class="ax-credit">
    <div class="ax-credit-container">
      <div class="ax-credit-card">
        <a class="ax-avatar" :href="PROFILE" target="_blank" rel="noopener">
          <img :src="AVATAR" :alt="AUTHOR_NAME" width="96" height="96" loading="lazy" />
        </a>
        <div class="ax-credit-text">
          <p class="ax-credit-label">{{ t.madeBy }}</p>
          <h2 class="ax-credit-name">{{ user?.name || AUTHOR_NAME }}</h2>
          <a class="ax-credit-handle" :href="PROFILE" target="_blank" rel="noopener">@{{ OWNER }}</a>
          <p class="ax-credit-about">{{ t.about }}</p>
          <div class="ax-credit-meta">
            <span v-if="user"><b>{{ compact(user.followers) }}</b> {{ t.followers }}</span>
            <span v-if="user"><b>{{ user.public_repos }}</b> {{ t.repos }}</span>
          </div>
          <a class="ax-btn alt" :href="PROFILE" target="_blank" rel="noopener">{{ t.follow }}</a>
        </div>
      </div>

      <div class="ax-credit-card ax-star-card">
        <svg class="ax-star-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" /></svg>
        <h2 class="ax-credit-name">{{ t.enjoying }}</h2>
        <p class="ax-credit-about">{{ t.starAsk }}</p>
        <a class="ax-btn brand" :href="REPO_PAGE" target="_blank" rel="noopener">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" /></svg>
          {{ t.star }}
          <span v-if="repoInfo" class="ax-btn-count">{{ repoInfo.stargazers_count.toLocaleString(lang) }}</span>
        </a>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* ---- badge ---- */
.ax-badge {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px 8px;
  margin-bottom: 20px;
  padding: 4px 14px;
  border: 1px solid var(--vp-c-brand-soft);
  border-radius: 20px;
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-text-1);
  font-size: 13px;
  line-height: 22px;
  text-decoration: none;
  transition: border-color 0.25s;
}
.ax-badge:hover {
  border-color: var(--vp-c-brand-1);
}
.ax-badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--vp-c-brand-1);
  box-shadow: 0 0 0 3px var(--vp-c-brand-soft);
}
.ax-badge-more {
  color: var(--vp-c-brand-1);
  font-weight: 600;
}

/* ---- stats ---- */
.ax-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 20px;
  margin-top: 24px;
}
@media (min-width: 960px) {
  .ax-stats {
    justify-content: flex-start;
  }
}
.ax-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--vp-c-text-2);
  font-size: 14px;
  text-decoration: none;
  transition: color 0.25s;
}
.ax-stat:hover {
  color: var(--vp-c-brand-1);
}
.ax-stat b {
  color: var(--vp-c-text-1);
}
.ax-stat svg {
  width: 18px;
  height: 18px;
  color: var(--vp-c-brand-1);
}

/* ---- credit, laid out like VPHomeFeatures ---- */
.ax-credit {
  padding: 48px 24px 0;
}
@media (min-width: 640px) {
  .ax-credit {
    padding: 64px 48px 0;
  }
}
@media (min-width: 960px) {
  .ax-credit {
    padding: 64px 64px 0;
  }
}
.ax-credit-container {
  display: grid;
  gap: 16px;
  max-width: 1152px;
  margin: 0 auto;
}
@media (min-width: 768px) {
  .ax-credit-container {
    grid-template-columns: 3fr 2fr;
  }
}

.ax-credit-card {
  display: flex;
  gap: 24px;
  padding: 28px;
  border: 1px solid var(--vp-c-bg-soft);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
}
@media (max-width: 639px) {
  .ax-credit-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}

.ax-avatar {
  flex-shrink: 0;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, var(--vp-c-brand-1), #ffb487);
}
.ax-avatar img {
  width: 100%;
  height: 100%;
  border: 3px solid var(--vp-c-bg-soft);
  border-radius: 50%;
  object-fit: cover;
}

.ax-credit-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}
@media (max-width: 639px) {
  .ax-credit-text {
    align-items: center;
  }
}
.ax-credit-label {
  margin: 0;
  color: var(--vp-c-text-2);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
.ax-credit-name {
  margin: 4px 0 0;
  padding: 0;
  border: none;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  background: linear-gradient(120deg, var(--vp-c-brand-1) 20%, #ffb487);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.ax-credit-handle {
  color: var(--vp-c-text-2);
  font-size: 14px;
  text-decoration: none;
}
.ax-credit-handle:hover {
  color: var(--vp-c-brand-1);
}
.ax-credit-about {
  margin: 12px 0 0;
  color: var(--vp-c-text-2);
  font-size: 14px;
  line-height: 1.6;
}
.ax-credit-meta {
  display: flex;
  gap: 16px;
  margin: 12px 0 16px;
  color: var(--vp-c-text-2);
  font-size: 14px;
}
.ax-credit-meta b {
  color: var(--vp-c-text-1);
}

.ax-star-card {
  flex-direction: column;
  align-items: flex-start;
  gap: 0;
  border-color: var(--vp-c-brand-soft);
}
@media (max-width: 639px) {
  .ax-star-card {
    align-items: center;
  }
}
.ax-star-icon {
  width: 36px;
  height: 36px;
  color: var(--vp-c-brand-1);
}
.ax-star-card .ax-btn {
  margin-top: 20px;
}

.ax-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  line-height: 40px;
  text-decoration: none;
  transition: background-color 0.25s;
}
.ax-btn svg {
  width: 18px;
  height: 18px;
}
.ax-btn.brand {
  background: var(--vp-button-brand-bg);
  color: var(--vp-button-brand-text);
}
.ax-btn.brand:hover {
  background: var(--vp-button-brand-hover-bg);
}
.ax-btn.alt {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
}
.ax-btn.alt:hover {
  background: var(--vp-button-alt-hover-bg);
  color: var(--vp-button-alt-hover-text);
}
.ax-btn-count {
  margin-left: 4px;
  padding-left: 12px;
  border-left: 1px solid currentColor;
  opacity: 0.85;
}
</style>
