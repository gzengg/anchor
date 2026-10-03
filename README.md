# 磐石 Anchor

自控力恢复的辅助 Android 应用：本地记录打卡 / 渴求 / 破戒，配合分级科普知识库、问卷评估与里程碑激励。
所有数据只存在本机（SQLCipher 加密的 Room 数据库），不联网、不上传。

界面按 iOS 设计语言实现：分组列表、分段控件、底部标签栏，完整支持深色模式。

## 模块

| 模块 | 内容 |
|---|---|
| `:core` | 纯 Kotlin 领域逻辑：戒断阶段、里程碑、提醒策略、内容索引（162 个单元测试） |
| `:app` | Android + Jetpack Compose：UI、Room + SQLCipher、WorkManager 提醒（84 个单元测试） |

## 构建

```bash
./gradlew :app:assembleDebug                   # 调试包
./gradlew :app:assembleRelease                 # 发布包（默认未签名，需自备 keystore）
./gradlew :core:test :app:testDebugUnitTest    # 单元测试
./gradlew :app:verifyContentAssets             # 知识库内容自检
```

工具链：JDK 20、Kotlin 2.2.20、AGP 8.12.1、compileSdk 35、minSdk 26。

release 包的签名参数从本机 `local.properties` 读取（该文件不入 git）：

```properties
anchor.release.storeFile=/path/to/your.jks
anchor.release.storePassword=***
anchor.release.keyAlias=***
anchor.release.keyPassword=***
```

四项缺任一就不创建签名配置，`assembleRelease` 产出未签名包，不会报错；想直接装包的话必须配齐这四项。

⚠️ `gradle.properties` 里带有作者本机的 JDK 路径（`org.gradle.java.home=E:/JAVA/20`）。
在其他机器上构建前请删除该行或改成本机路径（需 JDK 20），否则 Gradle 会直接报错。

## 知识库内容

`anchor-reference-docs/` 是 71 篇文献与公开资料的整理稿（每篇带原始来源链接），由
`content-tools/build_content.py`（离线脚本，零第三方依赖）编译成
`app/src/main/assets/content/*.json`，构建期由 `:app:verifyContentAssets` 校验
文章数、分类计数与 id 引用一致性。

打包字体为 HarmonyOS Sans SC，许可证见 `app/src/main/assets/licenses/HarmonyOS-Sans.txt`。

## 许可证

代码按 [MIT](LICENSE) 发布。`anchor-reference-docs/` 与知识库正文是公开文献与资料的整理稿，
版权归原作者与机构，每篇均标注来源链接，不适用 MIT 许可。

## 免责声明

本项目是自助记录与科普工具，不提供医疗建议、诊断或治疗。知识库内容为公开文献整理，
不能替代专业帮助；如有成瘾、情绪或健康方面的困扰，请咨询医生或心理专业人员。

开发计划、验收清单与验证记录见 `磐石Anchor-交付说明.md`。
