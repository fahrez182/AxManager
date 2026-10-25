---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "Axeron Manager"
  tagline: Axeron Manager 是一个 Android 应用程序，旨在为应用和系统提供更深层次的控制。
  image:
    light: /logo-light.svg
    dark: /logo-dark.svg
    alt: AxManager
  actions:
    - theme: brand
      text: 开始使用
      link: /zh/guide/what-is-axeron-manager
    - theme: alt
      text: 查看 GitHub
      link: https://github.com/fahrez182/AxManager
    - theme: alt
      text: 下载
      link: /zh/download

features:
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M17 1.01 7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg>'
    title: Root 可选
    details: 通过无线调试、电脑、OTG 线或 root 启动。所有模式下功能都相同，root 只解锁少数需要它的功能。
    link: /zh/guide/user-manual
    linkText: 如何启动
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M20 4H4c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.89-2-2-2zm0 14H4V8h16v10zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z"/></svg>'
    title: 快速终端
    details: 应用内的终端，拥有服务的权限。支持代码片段、在输出中查找、不限长度的输出，以及针对特殊设备的兼容模式。
    link: /zh/guide/app-tour#quickshell
    linkText: 了解更多
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6 4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0 4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>'
    title: 在任意终端中运行
    details: <code>axsh</code> 把同一个 shell 带到 Termux 等终端中，无需在其中安装任何东西。在真正的终端中会得到完整的交互式会话。
    link: /zh/guide/axsh
    linkText: 设置 axsh
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-1 16-4-4 1.41-1.41L11 14.17l6.59-6.59L19 9l-8 8z"/></svg>'
    title: 特权管理
    details: 按应用决定是否允许使用 AxManager：<b>Allowed</b>、<b>Ask</b> 或 <b>Denied</b>，并在同一个菜单中打开、清理、停止或停用应用。
    link: /zh/guide/app-tour#privilege
    linkText: 了解更多
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M17 7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h10c2.76 0 5-2.24 5-5s-2.24-5-5-5zm0 8c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z"/></svg>'
    title: 权限与 app-op
    details: 授予运行时权限，设置特殊访问权限以及 Android 记录的所有 app-op（包括 ROM 自带的），可作用于包或整个 uid。
    link: /zh/guide/app-tour#app-details
    linkText: 了解更多
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-1.99.9-1.99 2v3.8H3.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>'
    title: 插件（非 Root 模块）
    details: 使用不需要 root 的模块扩展 AxManager。它们随服务启动，重启后也能恢复。
    link: /zh/plugin/what-is-plugin
    linkText: 什么是插件？
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 0 0-1.38-3.56A8.03 8.03 0 0 1 18.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2s.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.99 7.99 0 0 1 5.08 16zm2.95-8H5.08a7.99 7.99 0 0 1 4.33-3.56A15.65 15.65 0 0 0 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2s.07-1.35.16-2h4.68c.09.65.16 1.32.16 2s-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 0 1-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2s-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>'
    title: WebUI
    details: 插件可以附带网页，通过 AxManager 运行 shell 命令，兼容 KernelSU 的 WebUI API，并可固定到桌面。
    link: /zh/plugin/webui-api
    linkText: WebUI API
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/></svg>'
    title: 设置编辑器
    details: 浏览和编辑 Global、Secure、System 设置、系统属性以及服务的环境变量，并把常改的项保存为快捷设置。
    link: /zh/guide/app-tour#settings-editor
    linkText: 了解更多
  - icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="m22 8-4-4v3H3v2h15v3l4-4zM2 16l4 4v-3h15v-2H6v-3l-4 4z"/></svg>'
    title: Shizuku 兼容
    details: 基于 Shizuku-API 构建的应用改为连接 AxManager，获得同样的权限，并使用 AxManager 自己的授权请求。
    link: /zh/guide/shizuku-compatibility
    linkText: 了解更多
---
