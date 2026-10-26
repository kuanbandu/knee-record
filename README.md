# 膝盖锻炼记录（微信小程序）

只做**记录与展示**的膝盖锻炼日志：用户自己录入动作、身体感受评分、时长，小程序保存并画趋势图。
**不提供动作推荐、不提供锻炼指导、不做任何自动判断。**

## 运行

仓库里是同一套产品的两个端，功能一致：

| 端 | 目录 | 怎么跑 |
|---|---|---|
| 微信小程序 | `miniprogram/` | 微信开发者工具 → 导入项目 → 目录选仓库根目录（`project.config.json` 已指定 `miniprogramRoot: miniprogram/`），AppID 选「测试号」即可预览。无 npm 依赖、无需构建 |
| Android | `android/` | Android Studio（Koala 2024.1.1 或更新）→ Open → 选 `android` 目录 → Sync → Run。JDK 17，最低 Android 8.0（API 26） |

小程序端开发自检：

```bash
npm run smoke   # 用 wx 桩在 Node 里跑通页面流程（不进包体）
```

> Android 工程没有提交 `gradle-wrapper.jar`（二进制）。首次 Sync 若提示 wrapper 缺失，在 `android/` 目录执行 `gradle wrapper` 生成，或让 Android Studio 自动修复。

## 页面

| 页面 | 类型 | 说明 |
|---|---|---|
| 我的记录 `pages/records` | tab（首页） | 锻炼记录列表 + 身体感受变化趋势图 + 「＋ 记一次」入口，支持删除记录 |
| 记一次 `pages/entry` | `navigateTo` | 手动录入：动作名称（可加多行，带可选组数）、身体感受评分 0-10、时长（分钟）、备注 |
| 我的情况 `pages/assess` | tab | 个人资料存档：年龄、当前阶段、膝盖弯曲角度自评。**仅保存，不参与任何功能** |
| 设置 `pages/settings` | tab | 提醒开关与时间、使用须知（静态展示）、清除数据 |

## 术语约定

产品内统一用词，新增文案请沿用：

| 用 | 不用 |
|---|---|
| 锻炼记录 | 康复训练 |
| 我的记录 | 康复计划 |
| 身体感受评分 | 疼痛评分 |
| 起步期 / 进阶期 / 强化期 / 日常保养 | 术后早期 / 术后中期 / 术后后期 / 预防强化 |
| 膝盖弯曲角度（自评） | 膝盖活动度自评 |
| 使用须知 | 免责声明 |

## 已移除的内容

上一版的推荐型逻辑已全部删除：

- `pages/plan`（自动生成的清单）与 `pages/workout`（跟练计时器）两个页面
- 开场前的强制勾选与 12 小时失效机制
- 身体感受上报的自动处置（停止/替代动作/就医提示）
- 趋势图上的警戒线与一切"建议/应该"类文案

上一版遗留的规则引擎 `engine/`、演示脚本 `demo.js` 与引擎测试 `test/` 也已删除，仓库内不再有任何推荐逻辑。

## 本地数据

`miniprogram/utils/store.js` 是唯一读写入口，页面禁止直接调用 `wx.setStorageSync`。

| key | 内容 |
|---|---|
| `knee:records` | 锻炼记录数组，最多 200 条 |
| `knee:assessment` | 个人资料 |
| `knee:settings` | 提醒开关与时间 |

记录结构：

```js
{
  id, date, timestamp,
  feelingScore,     // 0-10 身体感受
  durationMin,      // 分钟
  note,
  exercises: [{ name, sets }]
}
```

个人资料结构：`{ age, stage, bendAngle, straightenOk, updatedAt }`

## 目录

```
miniprogram/
├─ app.js / app.json / app.wxss
├─ utils/store.js
└─ pages/records|entry|assess|settings
android/
└─ app/src/main/java/com/kuanbandu/kneerecord/
   ├─ MainActivity.kt
   ├─ model/Models.kt          数据类与阶段选项
   ├─ data/LocalStore.kt       存储层（三个 JSON 文件）
   ├─ reminder/                每日提醒（AlarmManager + 本地通知）
   └─ ui/                      四个界面 + 趋势图
tools/smoke.js        小程序页面流程冒烟（wx 桩，不进包体）
```

## Android 版

- Kotlin + Jetpack Compose + Material3，除 AndroidX 外**无第三方依赖**，趋势图用 Compose Canvas 手绘。
- 数据与小程序端结构完全一致，存在应用私有目录：`knee_records.json` / `knee_assessment.json` / `knee_settings.json`。
- **每日提醒是真提醒**：本机 AlarmManager + 本地通知，不需要服务端（小程序端做不到，只能"打开时提示"）。Android 13+ 首次开启会请求通知权限；重启手机后需先打开一次应用才会重新排期。

## 已知限制

- **小程序端提醒不是后台推送**：无服务端无法定时推送，只能是「打开小程序时提示」。Android 端是真实的每日本地通知。
