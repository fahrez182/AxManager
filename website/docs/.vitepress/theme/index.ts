// The default theme, recoloured to match the app (see vortex.css).
import DefaultTheme from "vitepress/theme";
import type { Theme } from "vitepress";
import Icon from "./Icon.vue";
import "./vortex.css";

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component("Icon", Icon);
  },
} satisfies Theme;
