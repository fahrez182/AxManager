import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  base: "/AxManager/",
  title: "AxManager",
  description:
    "AxManager is an Android application designed to provide deeper control over apps and the system.",

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
                text: "What is Axeron Manager?",
                link: "/guide/what-is-axeron-manager",
              },
              { text: "User Manual", link: "/guide/user-manual" },
              { text: "FAQ", link: "/guide/faq" },
            ],
          },
          {
            text: "Plugin",
            items: [
              { text: "What is Plugin?", link: "/plugin/what-is-plugin" },
              {
                text: "Re-ignite (Reboot System)",
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
                text: "什么是 Axeron Manager？",
                link: "/zh/guide/what-is-axeron-manager",
              },
              { text: "用户手册", link: "/zh/guide/user-manual" },
              { text: "常见问题", link: "/zh/guide/faq" },
            ],
          },
          {
            text: "插件",
            items: [
              { text: "什么是插件？", link: "/zh/plugin/what-is-plugin" },
              { text: "Re-ignite（重启系统）", link: "/zh/plugin/re-ignite" },
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
