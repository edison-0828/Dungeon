# Solana Mobile dApp Store 填写稿

页面：https://publish.solanamobile.com/dashboard/new-dapp

这一步只填商店页资料。APK 在下一步上传。下面「可直接粘贴」的文字按当前工程写好。邮箱、钱包和三张图需要你自己准备。

Release 包名是 `com.cindervault.dungeon`。Debug 包名会变成 `com.cindervault.dungeon.indev`，不要把 debug 包填进来。

## 可直接粘贴

| 字段 | 限制 | 填写 |
| --- | --- | --- |
| App Name | 25 字 | `Cinder Vault`（12 字） |
| Package Name | 与 APK 完全一致，区分大小写 | `com.cindervault.dungeon` |
| Subtitle | 50 字 | `Single-player dungeon crawler. Optional SOL tips.`（49 字） |
| Description | 表单要求一段短文，输入框没有字数上限 | 见下方 |
| Language | 多选，按 APK 里实际带的语言各选一条 | 见下方清单 |
| Countries | 默认即可 | `All countries` |
| App Website | 公开网址，页面上要能看到这只应用、条款和隐私政策 | `https://edison-0828.github.io/Dungeon/` |
| Support Email | 接收用户支持邮件 | 你自己的邮箱 |
| Contact Email | 给 Solana dApp Store 的联系邮箱，可以和上面同一个 | 你自己的邮箱 |
| Terms of Use | 公开链接 | `https://edison-0828.github.io/Dungeon/terms.html` |
| Privacy Policy | 公开链接 | `https://edison-0828.github.io/Dungeon/privacy.html` |

Description：

```
Cinder Vault is a single-player roguelike. Each run builds a new dungeon of floors, enemies, and items, and you can finish it with no account and no wallet. It is an independent modification of Shattered Pixel Dungeon, itself based on Pixel Dungeon, and it is not an official or endorsed release. An optional screen can tip 0.01, 0.05, or 0.1 SOL. A tip buys nothing, your own wallet must approve it, and this app never holds your keys. Saves stay on the device.
```

462 个字符。

## 语言

在 Language 里搜索并勾选这些。每一种语言只选一条，对应 `android/build.gradle` 里打进 APK 的语言：

- English (United States)
- Chinese (Simplified, China)
- Chinese (Traditional, Taiwan)
- Korean (South Korea)
- Japanese (Japan)
- Russian (Russia)
- Spanish (Spain)
- Portuguese (Portugal)
- French (France)
- German (Germany)
- Polish (Poland)
- Italian (Italy)
- Turkish (Türkiye)
- Vietnamese (Vietnam)
- Ukrainian (Ukraine)
- Indonesian (Indonesia)
- Czech (Czechia)
- Dutch (Netherlands)
- Swedish (Sweden)
- Hungarian (Hungary)
- Finnish (Finland)
- Greek (Greece)
- Belarusian (Belarus)
- Esperanto (world)

国家保持 **All countries**。不要改成排除某些国家。

## 钱包

点页面上的 **Connect**，连一个你自己控制的 Solana 钱包。商店页不会显示这个地址。可以先存草稿，最后提交前必须连上。

应用内打赏的收款地址写在签名配置里，这张表没有这个输入框。

## 图片

| 素材 | 要求 | 现状 |
| --- | --- | --- |
| dApp Icon | 512×512，JPG / PNG / WEBP，必须有底色，不能透明 | `docs/dapp-icon-512.png`（512×512，RGB，无透明） |
| Banner | 1200×600，JPG / PNG / WEBP。显示在详情页顶部、图标后面 | `docs/dapp-banner-1200x600.png`（1200×600，RGB） |
| dApp Preview | 4 到 8 张。选 **Portrait**，比例 **9:16**。JPG、PNG、GIF、WEBP 或 MP4。图片单张最多 3MB，视频最多 30MB | 需要手机竖屏截图 |

方向选 Portrait。这只游戏在手机上是竖屏。

## 可以先跳过

**Editor's Choice** 的 Apply 是申请首页推荐，不是上架必填。商店资料填完再决定即可。

## 上传 APK：What's New

页面：Upload your dApp Release。APK 用已选的 `android-release.apk`（release，包名 `com.cindervault.dungeon`，版本 `1.0.0`，versionCode `896`）。

What's New 是给用户看的本版说明。这是第一次上架，输入框里的示例条目删掉，改贴下面这段：

```
First release of Cinder Vault.

- Single-player roguelike with randomized floors, enemies, and items
- A full run needs no account and no wallet
- Optional tips of 0.01, 0.05, or 0.1 SOL from the support screen
- A tip buys nothing, and your own wallet must approve it
```
