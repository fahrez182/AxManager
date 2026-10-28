# <Icon name="shield" /> 3.0 的安全更新

AxManager 以 ADB（或 root）的权限运行。3.0 改变了获取这些权限的方式：你从未允许过的应用无法触及它们，甚至无法得知 AxManager 的存在。请同时更新 AxManager 以及所有基于其 API 构建的应用。

::: warning 3.0 之前基于 Axeron API 构建的应用无法连接
3.0 改变了应用连接服务的方式。使用 2.x `axsh` 库的应用找不到服务，需要使用 `3.0.0` 或更高版本。基于 Shizuku-API 构建的应用不受影响。
:::

## 修复了什么 {#what-was-fixed}

以下每一项都可能让某个应用或网站在你未允许的情况下触及 AxManager 的 shell，而且都不需要 root 或电脑。

| 3.0 之前 | 现在 |
|---|---|
| 应用可以冒用 Shizuku 应用的包名，被当作 AxManager 的管理器。 | 只有通过 [Shizuku 兼容](./shizuku-compatibility.md)连接服务的 Shizuku 应用才拥有 Shizuku 应用的权限，而且只在那里有效。 |
| 获得已卸载应用 uid 的应用可能继承它的权限。 | 权限属于应用的签名证书，而不是 uid。其他应用，或由他人签名的同一应用，都会被重新询问。 |
| 从插件 WebUI 打开的网站可以运行命令。 | WebUI 页面只能是插件自己的；其他网站会在浏览器中打开，且无法被嵌入框架或接收表单提交。参见 [WebUI API](../plugin/webui-api.md#what-a-page-can-reach)。 |
| 应用可以抢在服务之前监听 axsh 的 socket，接收终端的输入。 | axsh 只连接由 root 或 shell 持有的 socket，而且它的名称只告诉已获准的应用。 |
| 应用可以向其他应用冒充服务。 | 应用只接受能从 root 或 shell 证明自己、且只为自己这次连接作答的服务。 |

## 对你的影响 {#what-changes-for-you}

- **每个应用都有自己的访问。** 服务从不把自己的连接交给其他应用。每个获准的应用得到只属于自己的连接，你撤销权限后立即失效。
- **重新安装的应用可能会再次请求。** 如果应用以其他密钥签名重新安装（例如用调试版替换正式版），AxManager 会把它当作新应用。
- **应用通过 AxManager 的对话框请求。** 尚未获准的应用可以打开 AxManager 的权限对话框；在你回答之前，它什么也得不到。
- **你可以隐藏 AxManager。** 开启 [AX-Stealth Mode](./app-tour.md#environment) 后，你未允许的应用既看不到对话框也得不到任何回应，就像 AxManager 未安装一样，Shizuku 客户端也包括在内。新应用改为在应用列表中允许。

AxManager 本身仍会显示在启动器中，也能被可列出所有已安装应用的应用看到：这些来自 Android，而不是 AxManager。

## 对应用开发者的影响 {#what-changes-for-app-developers}

- 使用 `3.0.0` 版本的 `axsh` 库。
- 你的应用不再从广播中读取服务。获准之后，服务会把只属于你的应用的连接交给它；`AxBroker` 会在第一条命令时等待它。
- 用 `AxBroker.requestAccess(activity, requestCode)` 请求访问，用 `AxBroker.findManager(context)` 找到 AxManager：任何地方都不需要包名。

详情参见 **[与应用连接](./connect-app.md)**。
