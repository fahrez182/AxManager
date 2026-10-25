// The default theme, recoloured to match the app (see vortex.css).
import DefaultTheme from "vitepress/theme";
import type { Theme } from "vitepress";
import Icon from "./Icon.vue";
import WebActivate from "./WebActivate.vue";
import DownloadRelease from "./DownloadRelease.vue";
import "./vortex.css";

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component("Icon", Icon);
    app.component("WebActivate", WebActivate);
    app.component("DownloadRelease", DownloadRelease);
  },
} satisfies Theme;
