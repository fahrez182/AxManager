/**
 * 云手机 Electron 主进程
 */

import { app, BrowserWindow, powerMonitor } from 'electron'
import path from 'path'
import fs from 'fs'

import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import { Request } from '@shared/api/request'

import { initMainWindow, mainWindowManager } from './core/window/MainWindow'
import { trayManager } from './core/window/TrayManager'
import { HostScannerQueue } from './core/scheduler/deviceScheduler'
import {
  configManager,
  mediaMtxManager,
  sharedFolderManager,
  batchTaskManager,
  frpManager
} from './core/store/managers'
import { logger } from './core/logger/Logger'
import { CONFIG_KEYS } from '@shared/constant'
import { destroyProxyCheckWorkerManager } from './core/workers/ProxyCheckWorkerManager'
import { destroyAgentWorkerManager } from './core/workers/AgentWorkerManager'
import { SQLiteDB } from './core/db/SQLiteDB'

app.commandLine.appendSwitch('no-proxy-server')

const initHardwareAcceleration = () => {
  try {
    const config = configManager.getValue(CONFIG_KEYS.DISABLE_HARDWARE_ACCELERATION)
    if (config === '1') {
      app.disableHardwareAcceleration()
    }
  } catch (error) {
    logger.error('[Init] Failed to initialize hardware acceleration:', error)
  }
}

initHardwareAcceleration()

process.on('uncaughtException', (error) => {
  logger.error('[App] Uncaught Exception:', error)
  console.error('[App] Uncaught Exception:', error)
})

process.on('unhandledRejection', (reason) => {
  logger.error('[App] Unhandled Rejection:', reason)
  console.error('[App] Unhandled Rejection:', reason)
})

app.on('child-process-gone', (_e, details) => {
  logger.error('[Main] child-process-gone:', details)
  console.error('[Main] child-process-gone:', details)
})

app.on('render-process-gone', (_e, _wc, details) => {
  logger.error('[Main] render-process-gone:', details)
  console.error('[Main] render-process-gone:', details)
})

const hostScannerQueue = new HostScannerQueue(10)

/* -----------------------------------------------------
 * Electron 启动优化参数
 * ---------------------------------------------------*/

// 设置应用名称
app.setName('VMOS Edge')

/* -----------------------------------------------------
 * 创建窗口
 * ---------------------------------------------------*/
function createWindow(): void {
  initMainWindow()

  hostScannerQueue.start()
}

/* -----------------------------------------------------
 * 初始化默认配置
 * ---------------------------------------------------*/
function initDefaultConfigs(): void {
  try {
    const userDataDir = app.getPath('userData')
    const imagesDir = path.join(userDataDir, 'vmosedge', 'images')
    const screenshotStoragePath = path.join(userDataDir, 'vmosedge', 'screenshots')

    if (!fs.existsSync(imagesDir)) {
      fs.mkdirSync(imagesDir, { recursive: true })
      logger.info(`[Init] Created images directory: ${imagesDir}`)
    }

    if (!fs.existsSync(screenshotStoragePath)) {
      fs.mkdirSync(screenshotStoragePath, { recursive: true })
      logger.info(`[Init] Created screenshots directory: ${screenshotStoragePath}`)
    }

    configManager.initDefaults({
      [CONFIG_KEYS.IMAGE_STORAGE_PATH]: imagesDir,
      [CONFIG_KEYS.SHARED_FOLDER_PATH]: '',
      [CONFIG_KEYS.SHARED_FOLDER_ENABLED]: '0',
      [CONFIG_KEYS.MAX_DISPLAY_SIDE]: '600',
      [CONFIG_KEYS.PROXY_CHECK_TIMEOUT]: '10000',
      [CONFIG_KEYS.PROXY_CHECK_API_KEY]: '',
      [CONFIG_KEYS.PROXY_CHECK_PROVIDER_TYPE]: 'ipmap',
      [CONFIG_KEYS.SCREENSHOT_STORAGE_PATH]: screenshotStoragePath,
      [CONFIG_KEYS.VIDEO_CODEC_PREFERENCE]: 'no-preference',
      [CONFIG_KEYS.RENDER_PREFERENCE]: 'low-power',
      [CONFIG_KEYS.DISABLE_HARDWARE_ACCELERATION]: '0',
      [CONFIG_KEYS.MAIN_WINDOW_SIZE]: 'standard',
      [CONFIG_KEYS.WHEEL_SPEED]: '100',
      [CONFIG_KEYS.KEEP_HOVER_MOVE]: '0',
      [CONFIG_KEYS.APP_LANGUAGE]: app.getLocale() || 'zh-CN',
      [CONFIG_KEYS.IS_STREAMING]: '0',
      [CONFIG_KEYS.THEME_MODE]: 'light',
      [CONFIG_KEYS.THEME_COLOR]: '#409eff',
      [CONFIG_KEYS.STREAM_FPS]: '60',
      [CONFIG_KEYS.STREAM_BITRATE]: '8'
    })

    // 显式重置推流状态，确保客户端重启后必定关闭
    configManager.setValue(CONFIG_KEYS.IS_STREAMING, '0')

    logger.info('[Init] Default configs initialized')
  } catch (error) {
    logger.error('[Init] Failed to initialize default configs:', error)
  }
}

/* -----------------------------------------------------
 * App Ready
 * ---------------------------------------------------*/
app.whenReady().then(async () => {
  logger.info('[App] Application is ready')

  electronApp.setAppUserModelId('com.vmos.edge.desktop')

  // 配置主进程请求语言头 - 从数据库获取
  Request.languageGetter = () => {
    return configManager.getValue(CONFIG_KEYS.APP_LANGUAGE) || app.getLocale() || 'zh-CN'
  }

  logger.info('[App] Initializing default configs...')
  initDefaultConfigs()

  try {
    await sharedFolderManager.restoreFromConfig()
  } catch (error) {
    logger.error('[App] Failed to restore shared folder service:', error)
  }

  batchTaskManager.recover().catch((error) => {
    logger.error('[App] Failed to recover batch tasks:', error)
  })

  frpManager.autoStart().catch((error) => {
    logger.error('[App] Failed to auto-start FRP:', error)
  })

  app.on('browser-window-created', (_, window) => {
    // Electron-toolkit 默认快捷键管理
    optimizer.watchWindowShortcuts(window)

    /**
     * 生产环境快捷键拦截 + 隐藏调试入口
     */
    if (!is.dev) {
      window.webContents.on('before-input-event', (event, input) => {
        if (input.type !== 'keyDown') return

        const mod = input.control || input.meta
        const code = input.code

        // ✅ 隐藏调试快捷键（Ctrl/Cmd + Shift + Alt + I）
        if (mod && input.shift && input.alt && code === 'KeyI') {
          event.preventDefault()
          logger.info('[Debug] Hidden devtools shortcut triggered')
          window.webContents.toggleDevTools()
          return
        }

        // ❌ 拦截默认 DevTools（F12 / Ctrl+Shift+I / Ctrl+Alt+I）
        if (code === 'F12' || (mod && (input.shift || input.alt) && code === 'KeyI')) {
          event.preventDefault()
          return
        }

        // ❌ 拦截刷新（F5 / Ctrl+R）
        if (code === 'F5' || (mod && code === 'KeyR')) {
          event.preventDefault()
        }
      })
    }
  })

  logger.info('[App] Creating main window...')
  trayManager.init() // 初始化托盘管理器
  createWindow()
  logger.info('[App] Main window creation initiated')

  // 监听系统休眠唤醒
  powerMonitor.on('resume', () => {
    logger.info('[App] System resume detected, forcing network discovery and host scan')

    hostScannerQueue.forceScan()
    frpManager.reconnectSsh()
    frpManager.scheduleReconcile()
  })

  // 监听系统解锁
  powerMonitor.on('unlock-screen', () => {
    logger.info('[App] System unlock detected, forcing network discovery and host scan')

    hostScannerQueue.forceScan()
    frpManager.reconnectSsh()
    frpManager.scheduleReconcile()
  })

  // 监听窗口聚焦（从任务栏点击打开也触发此事件）
  app.on('browser-window-focus', (_event, window) => {
    // 只有主窗口聚焦时才触发刷新，避免子窗口（如独立云机窗口）聚焦也触发大量请求
    if (window === mainWindowManager.getWindow()) {
      logger.info('[App] Main window focus detected, forcing host scan')
      hostScannerQueue.forceScan()
    }
  })

  app.on('activate', () => {
    const allWindows = BrowserWindow.getAllWindows()
    if (allWindows.length === 0) {
      createWindow()
    } else {
      const win = allWindows.find((w) => !w.isDestroyed())
      if (win) {
        win.show()
        win.focus()
      }
    }
  })
})

/* -----------------------------------------------------
 * 单实例锁
 * ---------------------------------------------------*/
const gotTheLock = app.requestSingleInstanceLock()

if (!gotTheLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    const win = BrowserWindow.getAllWindows().find((w) => !w.isDestroyed())
    if (win) {
      if (win.isMinimized()) win.restore()
      if (!win.isVisible()) win.show()
      win.focus()
    }
  })
}

/* -----------------------------------------------------
 * 生命周期 & 错误处理
 * ---------------------------------------------------*/
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  ;(app as any).isQuitting = true
  logger.info('[App] Shutting down...')

  try {
    mediaMtxManager.stopServer()
  } catch (error) {
    logger.error('[App] Failed to stop MediaMtx:', error)
  }

  void sharedFolderManager.stopSharing({ persistEnabled: false }).catch((error) => {
    logger.error('[App] Failed to stop shared folder service:', error)
  })

  try {
    hostScannerQueue.stop()
  } catch (error) {
    logger.error('[App] Failed to stop host scanner:', error)
  }

  try {
    frpManager.stopSync()
  } catch (error) {
    logger.error('[App] Failed to stop FRP:', error)
  }

  try {
    // 清理代理检测 Worker
    destroyProxyCheckWorkerManager()
  } catch (error) {
    logger.error('[App] Failed to destroy proxy worker manager:', error)
  }

  void destroyAgentWorkerManager().catch((error) => {
    console.error('[App] Failed to destroy agent runtime manager:', error)
  })

  try {
    batchTaskManager.destroy()
  } catch (error) {
    logger.error('[App] Failed to stop batch task poller:', error)
  }

  try {
    // 关闭数据库连接 (执行 WAL Checkpoint)
    SQLiteDB.getInstance().close()
  } catch (error) {
    console.error('[App] Failed to close database:', error)
  }

  logger.destroy()
})
