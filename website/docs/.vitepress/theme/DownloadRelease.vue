<script setup lang="ts">
// <DownloadRelease /> in markdown: the latest APKs and older releases, read from the GitHub API
// in the browser, so the download starts here instead of on GitHub's release page.
import { computed, onMounted, ref } from "vue";
import { useData } from "vitepress";
import { RELEASES_PAGE, latestOf, releases as fetchReleases, type Asset, type Release } from "./github";

// Older releases listed below the latest one.
const OLDER_SHOWN = 15;

// In the order the list shows them.
const ABIS = ["arm64-v8a", "armeabi-v7a", "x86_64", "universal"];

const STRINGS = {
  en: {
    abi: {
      "arm64-v8a": "Most phones (64-bit ARM)",
      "armeabi-v7a": "Older 32-bit ARM phones",
      "x86_64": "Emulators and x86 devices",
      universal: "Every device, larger file",
    } as Record<string, string>,
    loading: "Loading releases...",
    failed: "Can't load the releases from GitHub right now.",
    openGithub: "Open the releases on GitHub",
    latest: "Latest",
    pre: "Pre-release",
    download: "Download",
    recommended: "Recommended",
    notSure: "Not sure which one? Pick arm64-v8a, or Universal if it doesn't install.",
    extras: "Extras",
    notes: "Release notes",
    older: "Older releases",
    downloads: "downloads",
    viewOnGithub: "View on GitHub",
  },
  zh: {
    abi: {
      "arm64-v8a": "大多数手机（64 位 ARM）",
      "armeabi-v7a": "较旧的 32 位 ARM 手机",
      "x86_64": "模拟器和 x86 设备",
      universal: "所有设备，文件较大",
    } as Record<string, string>,
    loading: "正在加载版本...",
    failed: "暂时无法从 GitHub 加载版本。",
    openGithub: "在 GitHub 上查看版本",
    latest: "最新",
    pre: "预发布",
    download: "下载",
    recommended: "推荐",
    notSure: "不确定选哪个？选 arm64-v8a，安装不上再选 Universal。",
    extras: "其他文件",
    notes: "更新说明",
    older: "历史版本",
    downloads: "次下载",
    viewOnGithub: "在 GitHub 上查看",
  },
};

const { lang } = useData();
const t = computed(() => (lang.value.startsWith("zh") ? STRINGS.zh : STRINGS.en));

const releases = ref<Release[]>([]);
const state = ref<"loading" | "ready" | "failed">("loading");

const latest = computed(() => latestOf(releases.value));
const older = computed(() => releases.value.filter((r) => r !== latest.value).slice(0, OLDER_SHOWN));

// The app's own APKs are AxManager_<version>-release[-<abi>]_<build>.apk; anything else
// attached to a release (demo apps, sample plugins) goes under Extras.
function isApp(asset: Asset) {
  return asset.name.startsWith("AxManager_") && asset.name.endsWith(".apk");
}
function abiOf(asset: Asset): string {
  const m = asset.name.match(/-release-([a-z0-9_-]+?)_\d+\.apk$/);
  return m ? m[1] : "universal";
}
function appAssets(r: Release) {
  return r.assets.filter(isApp).sort((a, b) => ABIS.indexOf(abiOf(a)) - ABIS.indexOf(abiOf(b)));
}
function extraAssets(r: Release) {
  return r.assets.filter((a) => !isApp(a));
}
function recommendedOf(r: Release) {
  const apps = appAssets(r);
  return apps.find((a) => abiOf(a) === "arm64-v8a") ?? apps[0];
}

function size(bytes: number) {
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(bytes / 1024)} KB`;
}
function date(iso: string) {
  return new Date(iso).toLocaleDateString(lang.value, { year: "numeric", month: "short", day: "numeric" });
}
function count(n: number) {
  return n.toLocaleString(lang.value);
}
function totalDownloads(r: Release) {
  return r.assets.reduce((sum, a) => sum + a.download_count, 0);
}

onMounted(async () => {
  try {
    const list = await fetchReleases();
    if (!list.length) throw new Error("no releases");
    releases.value = list;
    state.value = "ready";
  } catch {
    state.value = "failed";
  }
});
</script>

<template>
  <div class="ax-dl">
    <p v-if="state === 'loading'" class="ax-dl-muted">{{ t.loading }}</p>

    <div v-else-if="state === 'failed'" class="ax-dl-card">
      <p class="ax-dl-muted">{{ t.failed }}</p>
      <a class="ax-dl-btn brand" :href="RELEASES_PAGE" target="_blank" rel="noopener">{{ t.openGithub }}</a>
    </div>

    <template v-else-if="latest">
      <section class="ax-dl-card ax-dl-latest">
        <div class="ax-dl-head">
          <div>
            <div class="ax-dl-tags">
              <span class="ax-dl-tag brand">{{ latest.prerelease ? t.pre : t.latest }}</span>
              <span class="ax-dl-muted">{{ date(latest.published_at) }}</span>
            </div>
            <h2 class="ax-dl-version">{{ latest.tag_name }}</h2>
            <p class="ax-dl-muted">{{ count(totalDownloads(latest)) }} {{ t.downloads }}</p>
          </div>
          <a
            v-if="recommendedOf(latest)"
            class="ax-dl-btn brand big"
            :href="recommendedOf(latest)!.browser_download_url"
          >
            <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" /></svg>
            {{ t.download }} {{ abiOf(recommendedOf(latest)!) }}
          </a>
        </div>

        <ul class="ax-dl-list">
          <li v-for="asset in appAssets(latest)" :key="asset.name" class="ax-dl-row">
            <div class="ax-dl-row-text">
              <span class="ax-dl-abi">
                {{ abiOf(asset) }}
                <span v-if="asset === recommendedOf(latest)" class="ax-dl-tag">{{ t.recommended }}</span>
              </span>
              <span class="ax-dl-muted">{{ t.abi[abiOf(asset)] ?? asset.name }} · {{ size(asset.size) }}</span>
            </div>
            <a class="ax-dl-btn alt" :href="asset.browser_download_url">{{ t.download }}</a>
          </li>
        </ul>
        <p v-if="appAssets(latest).length > 1" class="ax-dl-muted ax-dl-hint">{{ t.notSure }}</p>

        <template v-if="extraAssets(latest).length">
          <h3 class="ax-dl-sub">{{ t.extras }}</h3>
          <ul class="ax-dl-list">
            <li v-for="asset in extraAssets(latest)" :key="asset.name" class="ax-dl-row">
              <div class="ax-dl-row-text">
                <span class="ax-dl-file">{{ asset.name }}</span>
                <span class="ax-dl-muted">{{ size(asset.size) }}</span>
              </div>
              <a class="ax-dl-btn alt" :href="asset.browser_download_url">{{ t.download }}</a>
            </li>
          </ul>
        </template>

        <template v-if="latest.body_html">
          <h3 class="ax-dl-sub">{{ t.notes }}</h3>
          <div class="ax-dl-notes" v-html="latest.body_html" />
        </template>
      </section>

      <template v-if="older.length">
        <h3 class="ax-dl-sub">{{ t.older }}</h3>
        <details v-for="r in older" :key="r.tag_name" class="ax-dl-card ax-dl-old">
          <summary>
            <span class="ax-dl-abi">{{ r.tag_name }}</span>
            <span v-if="r.prerelease" class="ax-dl-tag">{{ t.pre }}</span>
            <span class="ax-dl-muted">{{ date(r.published_at) }}</span>
          </summary>
          <ul class="ax-dl-list">
            <li v-for="asset in r.assets" :key="asset.name" class="ax-dl-row">
              <div class="ax-dl-row-text">
                <span class="ax-dl-abi">{{ isApp(asset) ? abiOf(asset) : asset.name }}</span>
                <span class="ax-dl-muted">{{ size(asset.size) }}</span>
              </div>
              <a class="ax-dl-btn alt" :href="asset.browser_download_url">{{ t.download }}</a>
            </li>
          </ul>
          <div v-if="r.body_html" class="ax-dl-notes" v-html="r.body_html" />
          <a class="ax-dl-link" :href="r.html_url" target="_blank" rel="noopener">{{ t.viewOnGithub }}</a>
        </details>
      </template>

      <p class="ax-dl-muted ax-dl-hint">
        <a class="ax-dl-link" :href="RELEASES_PAGE" target="_blank" rel="noopener">{{ t.openGithub }}</a>
      </p>
    </template>
  </div>
</template>

<style scoped>
.ax-dl {
  margin: 16px 0;
}

.ax-dl-card {
  margin: 12px 0;
  padding: 20px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  background: var(--vp-c-bg-soft);
}
.ax-dl-latest {
  border-color: var(--vp-c-brand-soft);
}

.ax-dl-muted {
  margin: 0;
  color: var(--vp-c-text-2);
  font-size: 14px;
}

.ax-dl-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.ax-dl-tags {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ax-dl-version {
  margin: 6px 0 2px;
  padding: 0;
  border: none;
  font-size: 32px;
  line-height: 1.2;
}

.ax-dl-tag {
  padding: 0 8px;
  border-radius: 10px;
  background: var(--vp-c-default-soft);
  color: var(--vp-c-text-2);
  font-size: 12px;
  font-weight: 600;
  line-height: 20px;
}
.ax-dl-tag.brand {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
}

.ax-dl-sub {
  margin: 24px 0 8px;
  font-size: 16px;
}

.ax-dl-list {
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}
.ax-dl-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  padding: 10px 0;
  border-top: 1px solid var(--vp-c-divider);
}
.ax-dl-row-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.ax-dl-abi {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  white-space: nowrap;
  font-weight: 600;
}
.ax-dl-file {
  font-weight: 600;
  overflow-wrap: anywhere;
}
.ax-dl-hint {
  margin-top: 8px;
}

.ax-dl-btn {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  line-height: 38px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none !important;
  transition: background-color 0.25s;
}
.ax-dl-btn svg {
  width: 20px;
  height: 20px;
}
.ax-dl-btn.big {
  padding: 0 24px;
  line-height: 48px;
  border-radius: 24px;
  font-size: 16px;
}
.ax-dl-btn.brand {
  background: var(--vp-button-brand-bg);
  color: var(--vp-button-brand-text);
}
.ax-dl-btn.brand:hover {
  background: var(--vp-button-brand-hover-bg);
}
/* Tonal: the alt button's own color is the card's surface in dark mode. */
.ax-dl-btn.alt {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
}
.ax-dl-btn.alt:hover {
  background: var(--vp-button-alt-hover-bg);
  color: var(--vp-button-alt-hover-text);
}

.ax-dl-notes {
  font-size: 14px;
}
.ax-dl-notes :deep(h1),
.ax-dl-notes :deep(h2),
.ax-dl-notes :deep(h3) {
  margin: 12px 0 8px;
  padding: 0;
  border: none;
  font-size: 15px;
}
.ax-dl-notes :deep(p) {
  margin: 8px 0;
}

.ax-dl-old {
  padding: 0 20px;
}
.ax-dl-old summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0;
  cursor: pointer;
}
.ax-dl-old[open] {
  padding-bottom: 16px;
}
.ax-dl-old .ax-dl-list {
  margin-top: 0;
}

.ax-dl-link {
  font-size: 14px;
  font-weight: 500;
}
</style>
