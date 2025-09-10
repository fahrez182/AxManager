---
# https://vitepress.dev/reference/default-theme-home-page
layout: home

hero:
  name: "Axeron Manager"
  tagline: Axeron Manager is an Android application designed to provide deeper control over apps and the system.
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
  - icon: 🖥️
    title: Shell Executor
    details: Run shell commands directly from the app with support for <b>ADB/Non-Root</b> and optional Root execution.
  - icon: ⚡
    title: Plugin (Unrooted Module)
    details: Extend AxManager with third-party modules that don’t require root access.
    link: ./plugin/what-is-plugin
  - icon: 🌐
    title: WebUI (Unrooted Version)
    details: Execute shell commands via a web-based interactive interface.
    link: ./plugin/what-is-plugin/#webui
---
