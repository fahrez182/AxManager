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
          { text: "Activate", link: "/activate" },
          {
            text: "Download",
            link: "/download",
          },
        ],
        sidebar: [
          {
            text: "Activation",
            items: [
              { text: withIcon("usb", "Activate from the Browser"), link: "/activate" },
            ],
          },
          {
            text: "Getting started",
            items: [
              {
                text: withIcon("info", "What is Axeron Manager?"),
                link: "/guide/what-is-axeron-manager",
              },
              { text: withIcon("shield", "Security in 3.0"), link: "/guide/security" },
              { text: withIcon("start", "User Manual"), link: "/guide/user-manual" },
              {
                text: withIcon("start", "Start with Wireless Debugging"),
                link: "/guide/start-wireless",
              },
              { text: withIcon("usb", "Start with a Computer (adb)"), link: "/guide/start-computer" },
            ],
          },
          {
            text: "Using the app",
            items: [
              { text: withIcon("tour", "App Tour"), link: "/guide/app-tour" },
              { text: withIcon("key", "Privilege & Permissions"), link: "/guide/privilege" },
              { text: withIcon("code", "Run in Any Terminal (axsh)"), link: "/guide/axsh" },
              {
                text: withIcon("swap", "Shizuku Compatibility"),
                link: "/guide/shizuku-compatibility",
              },
              { text: withIcon("help", "FAQ"), link: "/guide/faq" },
            ],
          },
          {
            text: "For developers",
            items: [
              { text: withIcon("link", "Connect with an App"), link: "/guide/connect-app" },
              { text: withIcon("service", "AxShellService"), link: "/guide/shell-service" },
              { text: withIcon("tips", "Tips & Demo App"), link: "/guide/connect-tips" },
            ],
          },
          {
            text: "Plugin",
            items: [
              { text: withIcon("plugin", "What is Plugin?"), link: "/plugin/what-is-plugin" },
              { text: withIcon("plugin", "Plugin Installer"), link: "/plugin/plugin-installer" },
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
            link: "/zh/download",
          },
        ],
        sidebar: [
          {
            text: "入门",
            items: [
              {
                text: withIcon("info", "什么是 Axeron Manager？"),
                link: "/zh/guide/what-is-axeron-manager",
              },
              { text: withIcon("shield", "3.0 的安全更新"), link: "/zh/guide/security" },
              { text: withIcon("start", "用户手册"), link: "/zh/guide/user-manual" },
              { text: withIcon("start", "使用无线调试启动"), link: "/zh/guide/start-wireless" },
              { text: withIcon("usb", "使用电脑启动（adb）"), link: "/zh/guide/start-computer" },
            ],
          },
          {
            text: "使用应用",
            items: [
              { text: withIcon("tour", "应用导览"), link: "/zh/guide/app-tour" },
              { text: withIcon("key", "特权与权限"), link: "/zh/guide/privilege" },
              { text: withIcon("code", "在任意终端中运行（axsh）"), link: "/zh/guide/axsh" },
              {
                text: withIcon("swap", "Shizuku 兼容"),
                link: "/zh/guide/shizuku-compatibility",
              },
              { text: withIcon("help", "常见问题"), link: "/zh/guide/faq" },
            ],
          },
          {
            text: "面向开发者",
            items: [
              { text: withIcon("link", "与应用连接"), link: "/zh/guide/connect-app" },
              { text: withIcon("service", "AxShellService"), link: "/zh/guide/shell-service" },
              { text: withIcon("tips", "技巧与演示应用"), link: "/zh/guide/connect-tips" },
            ],
          },
          {
            text: "插件",
            items: [
              { text: withIcon("plugin", "什么是插件？"), link: "/zh/plugin/what-is-plugin" },
              { text: withIcon("plugin", "插件安装程序"), link: "/zh/plugin/plugin-installer" },
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
