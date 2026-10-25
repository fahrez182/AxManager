// The default theme, recoloured to match the app (see vortex.css).
import { h } from "vue";
import DefaultTheme from "vitepress/theme";
import type { Theme } from "vitepress";
import Icon from "./Icon.vue";
import WebActivate from "./WebActivate.vue";
import DownloadRelease from "./DownloadRelease.vue";
import HomeExtras from "./HomeExtras.vue";
import "./vortex.css";

export default {
  extends: DefaultTheme,
  // The home page's version badge, stats and credit, in the default layout's home slots.
  Layout: () =>
    h(DefaultTheme.Layout, null, {
      "home-hero-info-before": () => h(HomeExtras, { part: "badge" }),
      "home-hero-actions-after": () => h(HomeExtras, { part: "stats" }),
      "home-features-after": () => h(HomeExtras, { part: "credit" }),
    }),
  enhanceApp({ app }) {
    app.component("Icon", Icon);
    app.component("WebActivate", WebActivate);
    app.component("DownloadRelease", DownloadRelease);
  },
} satisfies Theme;
