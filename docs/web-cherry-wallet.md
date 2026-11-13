# 网页版钱包：交给另一个分支做

当前分支 `web-cherry` 上，游戏已经能在浏览器里玩。这一步只改网页钱包，让地牢里现有的「连接 / 打赏 / 买装备包」走到 Cherry（在 Cherry 里打开时）或浏览器钱包（本地直接打开时）。

Cherry 原文在：

`C:\Users\Administrator\.cursor\projects\d-code-Dungeon\attachments\4159bff1-652a-4c01-9624-0f01f093d50d\cherry-4b472823-9404-4550-a58e-a8f985bae418-integration.md`

官方全文：https://portal.cherry.fun/llms-full.txt

## 不要动这些

- `android/` 整目录，尤其是 `SolanaTipActivity.kt`、`SolanaChainRecord.kt`、`AndroidPlatformSupport.java`。Android 继续用 Mobile Wallet Adapter。
- `core/` 里的 `GateMerchant`、`WndGateMerchant`、`SupporterScene`。界面已经按下面的接口在调，钱包结果对了，按钮文案和发道具都会自己走。
- 不要把工程的 libGDX 从 1.14.0 升上去。网页后端是 gdx-teavm 1.5.6，1.6 要 1.14.2。
- 不要跑 `gradlew --refresh-dependencies`。这台机器访问 `dl.google.com` 会超时。`build.gradle` 里 `google()` 已经按组过滤，保持那样。
- 不要把收款地址写进 git。地址在仓库根目录的 `local.properties`（已 gitignore），键是 `tip.recipient.pubkey`。可选：`tip.rpc.url`、`tip.lamports`（默认 `10000000,50000000,100000000`）。

从 `web-cherry` 再拉分支改。改完用 `.\gradlew.bat :html:distWeb` 打包。产物在 `html/build/dist/webapp`。本地用静态服务器打开，不要双击 `index.html`。现成的服务是 http://127.0.0.1:8090 （工作目录就是那个 `webapp`）。

## 为什么不能按 Cherry 文档的 React 例子装

游戏是 Java，TeaVM 编成 `game.js`。没有 React，也没有 Vite。`npm install @cherrydotfun/miniapp-sdk` 不能装进 Java 源码里。

做法：另打一个小的浏览器脚本，在 `game.js` 之前加载（`BuildWeb.java` 已经用同样的办法插入 `scripts/freetype.js`）。脚本对外只留一个 `window.cinderWallet`。Java 的 `WebPlatformSupport` 用 TeaVM 的 JS 互调去调它。

建议目录：

- `html/wallet/`：npm 包，依赖 `@cherrydotfun/miniapp-sdk@^0.1.24`、`@solana/web3.js`、`@solana/wallet-adapter-base`。用 esbuild 打成单文件 `scripts/cinder-wallet.js`。
- `html/src/main/java/.../html/WebPlatformSupport.java`：实现下面的 `PlatformSupport` 方法。
- `BuildWeb.java`：把脚本拷进 `html/build/dist/webapp/scripts/`，并插到 `index.html` 里、`freetype.js` 之后、`game.js` 之前。同时写出 `/.well-known/cherry-app.json`。
- 构建时从 `local.properties` 读收款地址和 RPC，写进 `scripts/wallet-config.js`，不要写死在仓库里。

`window.cinderWallet` 需要这些异步方法（都返回 Promise）：

| 方法 | 作用 |
|---|---|
| `ready()` | SDK 是否加载完 |
| `insideCherry()` | 等同 Cherry 的 `isInsideCherry()` |
| `connect()` | 返回 base58 公钥 |
| `disconnect()` | 断开本地会话 |
| `transfer(lamports, memo)` | 签名并发送，返回 base58 签名；已买过抛一个可识别的 already 错误 |
| `packRecord(owner)` | 返回该地址已经付过的装备包 memo，逗号拼接 |

Java 侧收到结果后，用 `Gdx.app.postRunnable` 再回调 `SolanaTipListener`。不要在 GL 绘制过程中直接改 Noosa 界面。`request*` 先返回 `true`（表示已经开始），结果稍后回调。这和 Android 一致。`WndGateMerchant.startConnect` 只有在返回 `false` 时才会立刻把按钮从「处理中」恢复。

## 游戏已经在等的结果

定义在 `SPD-classes/.../PlatformSupport.java` 的 `SolanaTip`。网页必须用这些整数，不要新造一套。

| 值 | 名字 | `detail` |
|---|---|---|
| 1 | `SUBMITTED` | 交易签名的 base58。打赏和买包成功都用这个 |
| 2 | `CANCELLED` | 用户拒绝。Cherry 的 `USER_REJECTED` 走这里 |
| 3 | `NO_WALLET` | 外面打开、又没有浏览器钱包 |
| 4 | `FAILED` | 其它失败。余额不够时 `detail` 用 `"short"`；发出去但没确认用 `"unconfirmed"` |
| 5 | `NOT_CONFIGURED` | `local.properties` 里没有合法收款地址 |
| 6 | `OFFLINE` | `navigator.onLine === false` |
| 7 | `RECORD` | `公钥\nmemo,memo`。读链失败时 memo 部分用 `"unread"` |
| 8 | `ALREADY` | 这条 memo 链上已经付过，不要再转一笔 |
| 9 | `CONNECTED` | 公钥 base58 |
| 10 | `DISCONNECTED` | 空字符串 |

连接成功后平台层自己要做两件事，界面不会做：

1. `GateMerchant.noteSession(公钥, "")`。Cherry 没有 Mobile Wallet Adapter 的 auth token，第二个参数留空。
2. 紧接着再回调一次 `RECORD`，`detail` 是 `公钥 + "\n" + memo列表`。`WndGateMerchant.applyRecord` 靠这个把已买的包记进存档。

断开时调用 `GateMerchant.forgetWallet()`，再回调 `DISCONNECTED`。

`supportsSolanaTips()` 在网页返回 `true`，否则支持者页和闸门商人会把钱包入口藏掉。`solanaTipsConfigured()` 仅在收款地址像 Solana 公钥时返回 `true`。`solanaTipRecipient()` 返回该地址。`solanaNetworkName()` 与 Android 发布包一致，用 `mainnet-beta`。`solanaTipLamports()` 用 `tip.lamports`，缺省 `10000000,50000000,100000000`。装备包不走这个数组，它在 `GateMerchant.Offer.WEAPON`：`50000000` lamports（0.05 SOL），memo 固定 `cindervault:weapon`。

## 交易要和 Android 读得回来

`SolanaChainRecord.ownedPackMemos` 会扫付款人地址上的签名，找出 memo 里包含 `cindervault:weapon`、并且确实转给了收款地址的交易。网页发出去的交易必须满足：

1. 一条 `SystemProgram.transfer`，从付款人到 `tip.recipient.pubkey`，金额是调用方传入的 lamports。
2. 有 memo 时再加一条 Memo 程序指令，内容就是那个字符串（装备包是 `cindervault:weapon`）。打赏的 memo 为空，不要造一条空 memo。
3. 在 Cherry 里，给其中一条指令加上 Cherry 的 app reference。用 SDK 的 `withAppReference(ix, "4b472823-9404-4550-a58e-a8f985bae418")`。这是只读、非签名账户，用来让 Cherry 记账。不要另转一笔「给 Cherry 的手续费」，文档写明费用由 Cherry 月底向开发者收。
4. 签名前的说明必须和实际金额一致，最多 80 字。装备包用 `Buy the weapon pack` 这类一句人话。打赏用 `Tip the developer`。金额让 Cherry 自己从交易里读，标签里不要写一个不同的数。
5. 发之前用 `getBalance` 看余额是否够 `lamports + 5000`。不够就回调 `FAILED` / `"short"`，不要把交易丢给钱包。
6. 发之前用和 Android 相同的规则查这个付款人是否已经付过该 memo。付过就回调 `ALREADY`，不要再签。
7. 签名发出后等 `confirmed`。确认了回调 `SUBMITTED` 加签名；用户关掉钱包回调 `CANCELLED`；Cherry `POLICY_DENIED` 回调 `FAILED`，`detail` 里带上 `policyCode`（`TX_LIMIT`、`DAILY_LIMIT` 等），不要自动重试。

未认证档上限是单笔 50 美元、每钱包每天 50 美元。0.05 SOL 的装备包和现有打赏档在正常币价下低于这个上限。不要做授权、委托、改权限、durable nonce、订阅。

## Cherry 里和外面不一样

用 `isInsideCherry()` 判断。Cherry 网页宿主的地址带 `?cherry_embed=1`，手机宿主会在页面加载前设置 `window.__cherry`。

在 Cherry 里：

- 钱包已经连上。`requestSolanaConnect` 直接取 Cherry 的付款钱包（有 `paymentWallet` 用它，否则用已连接钱包），回调 `CONNECTED`，再回调 `RECORD`。不要弹出自己的「连接钱包」，也不要为了登录去 `signMessage`。
- 付款钱包变了（`WALLET_CHANGED`、`NOT_A_SIGNER`）就按新地址重做这笔交易，不要用旧地址硬签。
- 这个游戏没有自己的后端。不要为了过文档去造一个 `/api/auth/cherry`。道具是否发放只看用户签过的转账和链上 memo，和 Android 一样。启动令牌可以读来显示名字，不能当成「这个人已经付过钱」。
- 用户主键如果以后要存，用 `cherry_id ?? sub`，不要用钱包地址当人。现在的 `GateMerchant` 仍按钱包地址记购买，这是和 Android 共用的存档字段，这一轮不要改。

在外面（本地 8090、以后自己的网站）：

- `requestSolanaConnect` 走浏览器钱包（Phantom 这一类）。没有钱包就回调 `NO_WALLET`。游戏会显示安装链接，`solanaWalletInstallUris()` 可以返回和 Android 相同的那一组下载页。
- 交易同样是 transfer + memo。外面没有 Cherry 宿主时，不要调用只在 iframe 里存在的签名接口。

## 页面上 Cherry 还要的几样

这些是静态资源，不是游戏逻辑：

- `html/build/dist/webapp/.well-known/cherry-app.json` 内容只有 `{"appId":"4b472823-9404-4550-a58e-a8f985bae418"}`，HTTP 200，不能被 SPA 回退成 `index.html`。`BuildWeb` 每次打包都写这个文件。
- 上线时的 HTML 响应要带 `Content-Security-Policy: frame-ancestors 'self' https://chat.cherry.fun https://cherry.fun https://*.cherry.fun`，并且不能有 `X-Frame-Options`。本地 `python -m http.server` 加不了这个头，写进部署说明即可，不要为它改游戏。
- 画布现在是 480×800，居中。Cherry 要求 360 宽、不要横向滚动。用 CSS 让画布按视口缩小，不要改地牢的虚拟分辨率。
- 条款和隐私只放链接，不要把正文抄进仓库：
  - https://portal.cherry.fun/legal/app_ca36265cba69/4b472823-9404-4550-a58e-a8f985bae418/terms
  - https://portal.cherry.fun/legal/app_ca36265cba69/4b472823-9404-4550-a58e-a8f985bae418/privacy
  放在游戏里已有的支持者页或关于页，用 `PlatformSupport.openURI`。
- 屏蔽国家 CU、IR、KP、SD 由 Cherry 上架名单负责。这个客户端没有可靠的国家接口，不要再接一个 IP 定位服务。

App ID `4b472823-9404-4550-a58e-a8f985bae418`。Project ID `app_ca36265cba69`。链是 Solana。收入模式是销售，Cherry 抽成交额的 10%，应用内不要再转一笔给 Cherry。

## 上线时的响应头

每个 HTML 响应带上下面这个头，并且不要再发 `X-Frame-Options`。本地 `python -m http.server` 加不了，所以它只写在这里。

```nginx
add_header Content-Security-Policy "frame-ancestors 'self' https://chat.cherry.fun https://cherry.fun https://*.cherry.fun" always;
```

`/.well-known/cherry-app.json` 要直接返回 HTTP 200 的 JSON。若前面有一层会把未知路径回退成 `index.html`，给这个路径单独一条 location，不要走那段回退。

## 做完怎么验

1. `.\gradlew.bat :html:distWeb` 成功。`index.html` 里 `cinder-wallet.js` 在 `game.js` 之前。
2. 打开 http://127.0.0.1:8090 。不在 Cherry 里时，支持者页或闸门商人能点「连接」。没装钱包时应看到「没有钱包」，而不是按钮毫无反应。
3. 装了 Phantom、并且 `local.properties` 配了 devnet 收款地址时：连接后按钮变成断开；买武器包会弹出钱包，金额 0.05 SOL，memo 含 `cindervault:weapon`；确认后界面变成已购买，重新连接仍显示已购买。用户拒绝显示取消，不要连着弹第二次。
4. 用 `?cherry_embed=1` 打开时，进商店不要再要求签名登录。没有 Cherry 宿主时连接会失败，这是预期的，把失败显示出来即可。真正的 Cherry 内测试要等开发者在 Cherry 门户填上网址后，用他们的上架检查跑。
5. Android 工程仍能编过，且 diff 里没有 `android/` 的改动。
