# BuyWise 继续开发方案：评估历史 + 观望清单（Room 落地）

## 项目现状摘要
- **应用定位**：购买决策助手（中文），核心算法"三维量化决策模型"已在 `domain/BuyWiseEngine.kt` 完整实现（纯 Kotlin、无 Android 依赖），四个步骤（财务杠杆设置 → 三维评分卡 → 精算 → 限时决策协议）UI 全部完成，APK 可编译。
- **主要问题**：git 仓库零提交；`app/` 根目录下有 4 个放错位置的重复源文件（`app/data/`、`app/domain/`）；Room 依赖 + KSP 已声明但零使用；`app/src` 下无 test 目录（零测试）；评估结果无法保存；冷启动永远先进设置页；无应用图标。

## 阶段 0：工程清理与基线
1. **删除游离重复文件**（内容与 `src/main/java` 下几乎一致，仅格式差异）：
   - `app/data/local/PreferencesManager.kt`、`app/domain/BuyWiseEngine.kt`、`app/domain/model/AssessmentModels.kt`、`app/domain/ScoreDescriptors.kt`
2. **git 基线提交**：先提交现有可编译状态（`.gitignore` 补充 `.gradle/`、`build/`、`local.properties`、`.codebuddy/`），再按阶段分批提交。
3. **最小自适应图标**：手写 adaptive icon（vector 前景 + `@color` 背景），补上 Manifest 的 `android:icon`/`android:roundIcon`（无需 Android Studio）。

## 阶段 1：Room 数据层（把已声明的 Room 用起来）
新增文件（包结构沿用现有 MVVM 约定）：
- `data/local/AssessmentRecordEntity.kt` — 一张表存完整评估快照：id、createdAt、itemName、price、r/e/f、score、baseDecision、finalDecision、精算全字段（resaleValue、estimatedUses、annualUtilityValue、netCost、realUnitCost、unitCostThreshold、unitCostPass、opportunityGain、opportunityPass、completed、passed）、冷静期字段、限时协议摘要（nullable：netValue、maxDecisionHours、decision）、`status`（HISTORY / WATCHLIST）。枚举以 String 存储；`exportSchema = false`。
- `data/local/AssessmentRecordDao.kt` — `insert`、`deleteById`、`observeAll(): Flow<List<...>>`（按 createdAt DESC）、`updateStatus(id, status)`、`getById(id)`。
- `data/local/AppDatabase.kt` — RoomDatabase（version 1）。
- `data/repository/AssessmentRepository.kt` — 包 DAO，暴露 Flow 与 suspend 方法，负责 entity ↔ domain 模型互转（detail 页复用 `ResultCard` 渲染）。

## 阶段 2：保存动作 + 历史/观望清单 UI
- `AssessmentViewModel` 注入 repository，新增 `saveToHistory()` / `saveToWatchlist()`（GIVE_UP 结果卡片对应文案已承诺"放入观望清单"）；保存后 Snackbar 反馈。
- `AssessmentComponents.kt` 的 `ResultCard` 底部加"存入历史"按钮；决策为"不买"时加"加入观望清单"按钮；限时协议结果同样可保存。
- 新增 `ui/history/`：
  - `HistoryScreen.kt` — 顶部 Tab（历史 / 观望清单），列表项显示物品名、价格、总分、决策徽章、时间；支持删除（滑动或菜单）、观望项可"移回历史/转正为已购"（仅状态切换）。
  - `HistoryDetailScreen.kt` — 路由 `history/{id}`，entity 还原为 `AssessmentResult` 后复用 `ResultCard` + 精算明细文案。
  - `HistoryViewModel.kt` — 列表 StateFlow + 删除/状态切换操作。

## 阶段 3：导航重构
- `Screen.kt` 扩展为四个路由：`assessment`、`history`、`history/{id}`、`settings`（带图标）。
- `BuyWiseNavGraph.kt` 改为底部 `NavigationBar` 三 Tab（评估 / 历史 / 设置）+ Scaffold。
- **智能启动页**：`MainActivity` 先取 `preferencesManager.profileFlow.first()`，已配置（月薪 > 0）→ startDestination = 评估页，否则 = 设置页。设置页"保存并进入评估"改为切 Tab。

## 阶段 4：单元测试 + 构建验证
- 新建 `app/src/test/java/com/buywise/app/domain/BuyWiseEngineTest.kt`：覆盖 `baseScore`、`suggestF`（2H/5H/20H 锚点与插值）、`assess` 三档分支与冷静期触发、`buildRefine`（单次成本通过/未通过/未填写、机会成本 null 语义）、`evaluateLimitedTime`（V≤0 陷阱、三问拦截、T_max 取 min）。
- `FinanceProfileTest.kt`：时薪/日沉没成本公式。
- 构建验证（本机为 TLS 拦截代理网络，按项目记忆需带 Windows-ROOT 信任库参数）：
  ```
  export GRADLE_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL"
  export JAVA_OPTS="$GRADLE_OPTS"
  ./gradlew.bat :app:assembleDebug :app:testDebugUnitTest
  ```

## 明确不在本轮范围（后续阶段）
- 冷静期 12h 倒计时与本地通知、观望清单目标价降价提醒（观望清单表结构本轮已就绪，加 `targetPrice` 字段即可扩展）。
- Hilt 引入（保持手动工厂，`BuyWiseViewModelFactory` 现状够用）、strings.xml 国际化、深色主题打磨。

## 交付验收标准
1. `assembleDebug` 与 `testDebugUnitTest` 全部通过。
2. 评估 → 保存 → 历史/观望清单可见 → 详情正确还原 → 删除/状态切换正常。
3. 已配置用户冷启动直达评估页，三 Tab 导航切换正常。
4. git 提交按阶段划分，工作区干净。