---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "Axeron Manager"
  tagline: Axeron Manager is an Android application designed to provide deeper control over apps and the system.
  image:
    light: /logo-light.svg
    dark: /logo-dark.svg
    alt: AxManager
  actions:
    - theme: brand
      text: Get Started
      link: /guide/what-is-axeron-manager
    - theme: alt
      text: View on GitHub
      link: https://github.com/fahrez182/AxManager
    - theme: alt
      text: Download
      link: https://github.com/fahrez182/AxManager/releases

features:
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M17 1.01 7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg>'
    title: Root optional
    details: Start through wireless debugging, a computer, an OTG cable or root. The same features work in every mode, with root unlocking the few that need it.
    link: /guide/user-manual
    linkText: How to start
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.89-2-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z"/></svg>'
    title: QuickShell
    details: A terminal inside the app with the server's privileges. Snippets, find in output, output of any length, and a compatibility mode for stubborn devices.
    link: /guide/app-tour#quickshell
    linkText: Learn more
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6 4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0 4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>'
    title: Run in any terminal
    details: <code>axsh</code> brings the same shell to Termux and other terminals, with nothing to install there. A real terminal gets a full interactive session.
    link: /guide/axsh
    linkText: Set up axsh
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-1 16-4-4 1.41-1.41L11 14.17l6.59-6.59L19 9l-8 8z"/></svg>'
    title: Privilege manager
    details: Decide per app whether it may use AxManager, <b>Allowed</b>, <b>Ask</b> or <b>Denied</b>, and open, clear, stop or disable apps from one menu.
    link: /guide/app-tour#privilege
    linkText: Learn more
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M17 7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h10c2.76 0 5-2.24 5-5s-2.24-5-5-5zm0 8c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z"/></svg>'
    title: Permissions & app-ops
    details: Grant runtime permissions, set special access and every app-op Android keeps, your ROM's own included, for the package or the whole uid.
    link: /guide/app-tour#app-details
    linkText: Learn more
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>'
    title: Plugins (unrooted modules)
    details: Extend AxManager with modules that don't need root. They start with the server and can come back after a reboot.
    link: /plugin/what-is-plugin
    linkText: What is a plugin?
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 0 0-1.38-3.56A8.03 8.03 0 0 1 18.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2s.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.99 7.99 0 0 1 5.08 16zm2.95-8H5.08a7.99 7.99 0 0 1 4.33-3.56A15.65 15.65 0 0 0 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2s.07-1.35.16-2h4.68c.09.65.16 1.32.16 2s-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 0 1-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2s-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>'
    title: WebUI
    details: Plugins can ship a web page that runs shell commands through AxManager, compatible with KernelSU's WebUI API, and pin it to the home screen.
    link: /plugin/webui-api
    linkText: WebUI API
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/></svg>'
    title: Settings Editor
    details: Browse and edit Global, Secure and System settings, properties and the server's environment, and keep the ones you change often as Quick Settings.
    link: /guide/app-tour#settings-editor
    linkText: Learn more
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="m22 8-4-4v3H3v2h15v3l4-4zM2 16l4 4v-3h15v-2H6v-3l-4 4z"/></svg>'
    title: Shizuku compatibility
    details: Apps built on Shizuku-API connect to AxManager instead, with the same access and AxManager's own permission requests.
    link: /guide/shizuku-compatibility
    linkText: Learn more
---
