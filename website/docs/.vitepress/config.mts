import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  base: "/AxManager/",
  title: "AxManager",
  description:
    "AxManager is an Android application designed to provide deeper control over apps and the system.",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: "Guide", link: "/guide/what-is-axeron-manager" },
      { text: "Plugin", link: "/plugin/what-is-plugin" },
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
        items: [{ text: "What is Plugin?", link: "/plugin/what-is-plugin" }],
      },
    ],

    socialLinks: [
      { icon: "github", link: "https://github.com/fahrez182/AxManager" },
    ],
  },
});
