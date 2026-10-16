import { defineConfig } from "vitepress";
import { withIcon } from "./theme/icons";

// Link previews need absolute URLs.
const SITE = "https://axmanager.app";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  base: "/",
  title: "AxManager",
  description:
    "AxManager is an Android application designed to provide deeper control over apps and the system.",

  head: [
    ["link", { rel: "icon", type: "image/png", href: "/favicon.png" }],
    ["link", { rel: "apple-touch-icon", href: "/favicon.png" }],
    // The browser bar takes the app's surface colors.
    ["meta", { name: "theme-color", media: "(prefers-color-scheme: light)", content: "#ffffff" }],
    ["meta", { name: "theme-color", media: "(prefers-color-scheme: dark)", content: "#15120f" }],
    // The card a shared link shows (Open Graph, read by WhatsApp, Telegram, Discord, X...).
    ["meta", { property: "og:type", content: "website" }],
    ["meta", { property: "og:site_name", content: "AxManager" }],
    ["meta", { property: "og:image", content: `${SITE}/og-image.png` }],
    ["meta", { property: "og:image:width", content: "1200" }],
    ["meta", { property: "og:image:height", content: "630" }],
    ["meta", { property: "og:image:alt", content: "Axeron Manager" }],
    ["meta", { name: "twitter:card", content: "summary_large_image" }],
    ["meta", { name: "twitter:image", content: `${SITE}/og-image.png` }],
  ],

  // Each page's own title, description and address on its card.
  transformHead({ pageData, siteData }) {
    const title = pageData.title ? `${pageData.title} | ${siteData.title}` : siteData.title;
    const locale = pageData.relativePath.startsWith("zh/") ? siteData.locales.zh : undefined;
    const description = pageData.description || locale?.description || siteData.description;
    const path = pageData.relativePath.replace(/(^|\/)index\.md$/, "$1").replace(/\.md$/, "");
    return [
      ["meta", { property: "og:title", content: title }],
      ["meta", { property: "og:description", content: description }],
      ["meta", { property: "og:url", content: `${SITE}/${path}` }],
      ["meta", { name: "twitter:title", content: title }],
      ["meta", { name: "twitter:description", content: description }],
    ];
  },

  // Shared by every locale.
  themeConfig: {
    logo: { light: "/logo-light.svg", dark: "/logo-dark.svg", alt: "AxManager" },
  },

  // 国际化配置
  locales: {
    root: {
      label: "English",
      lang: "en-US",
      themeConfig: {
        nav: [
          { text: "Guide", link: "/guide/what-is-axeron-manager" },
          { text: "Plugin", link: "/plugin/what-is-plugin" },
          {
            text: "Download",
            link: "https://github.com/fahrez182/AxManager/releases",
          },
        ],
        sidebar: [
          {
            text: "Guide",
            items: [
              {
                text: withIcon("info", "What is Axeron Manager?"),
                link: "/guide/what-is-axeron-manager",
              },
              { text: withIcon("start", "User Manual"), link: "/guide/user-manual" },
              { text: withIcon("tour", "App Tour"), link: "/guide/app-tour" },
              { text: withIcon("code", "Run in Any Terminal (axsh)"), link: "/guide/axsh" },
              {
                text: withIcon("swap", "Shizuku Compatibility"),
                link: "/guide/shizuku-compatibility",
              },
              { text: withIcon("help", "FAQ"), link: "/guide/faq" },
            ],
          },
          {
            text: "Plugin",
            items: [
              { text: withIcon("plugin", "What is Plugin?"), link: "/plugin/what-is-plugin" },
              { text: withIcon("folder", "Environment & Paths"), link: "/plugin/environment" },
              { text: withIcon("web", "WebUI API"), link: "/plugin/webui-api" },
              {
                text: withIcon("reboot", "Re-ignite (Reboot System)"),
                link: "/plugin/re-ignite",
              },
            ],
          },
        ],
        socialLinks: [
          { icon: "github", link: "https://github.com/fahrez182/AxManager" },
        ],
      }
    },
    zh: {
      label: "简体中文",
      lang: "zh-CN",
      link: "/zh/",
      description: "AxManager 是一款 Android 应用，让你更深入地控制应用和系统。",
      themeConfig: {
        nav: [
          { text: "指南", link: "/zh/guide/what-is-axeron-manager" },
          { text: "插件", link: "/zh/plugin/what-is-plugin" },
          {
            text: "下载",
            link: "https://github.com/fahrez182/AxManager/releases",
          },
        ],
        sidebar: [
          {
            text: "指南",
            items: [
              {
                text: withIcon("info", "什么是 Axeron Manager？"),
                link: "/zh/guide/what-is-axeron-manager",
              },
              { text: withIcon("start", "用户手册"), link: "/zh/guide/user-manual" },
              { text: withIcon("tour", "应用导览"), link: "/zh/guide/app-tour" },
              { text: withIcon("code", "在任意终端中运行（axsh）"), link: "/zh/guide/axsh" },
              {
                text: withIcon("swap", "Shizuku 兼容"),
                link: "/zh/guide/shizuku-compatibility",
              },
              { text: withIcon("help", "常见问题"), link: "/zh/guide/faq" },
            ],
          },
          {
            text: "插件",
            items: [
              { text: withIcon("plugin", "什么是插件？"), link: "/zh/plugin/what-is-plugin" },
              { text: withIcon("folder", "环境与路径"), link: "/zh/plugin/environment" },
              { text: withIcon("web", "WebUI API"), link: "/zh/plugin/webui-api" },
              { text: withIcon("reboot", "Re-ignite（重启系统）"), link: "/zh/plugin/re-ignite" },
            ],
          },
        ],
        socialLinks: [
          { icon: "github", link: "https://github.com/fahrez182/AxManager" },
        ],
      }
    },
  },
});
