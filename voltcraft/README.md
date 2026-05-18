# VoltCraft

真实电力传输与保护层 Mod，目标是在 Minecraft 1.21.1 / NeoForge / Java 21 上为科技整合包提供电压等级、电流推算、漏保、空开、接地等真实电气特性。设计文档见 [`../MC电力Mod设计文档.md`](../MC电力Mod设计文档.md)。

## 当前状态

第一阶段脚手架。已注册：
- 主类 `com.voltcraft.VoltCraft`
- 占位方块 `voltcraft:low_voltage_cable`（220V 低压电缆，纯外观，未接入电力逻辑）
- 创造模式标签页 `itemGroup.voltcraft`
- 中英文本地化

## 目录结构

```
voltcraft/
├── build.gradle              Gradle 构建脚本（NeoForge moddev 插件）
├── settings.gradle           NeoForge 仓库配置
├── gradle.properties         版本/依赖范围（修改 Mod 元信息从这里入手）
├── gradle/wrapper/           Gradle Wrapper 配置（首次运行 gradlew 会自动下载）
└── src/main/
    ├── java/com/voltcraft/
    │   ├── VoltCraft.java                主类
    │   ├── block/                         方块实现
    │   └── registry/                      DeferredRegister 注册容器
    ├── resources/
    │   ├── pack.mcmeta
    │   └── assets/voltcraft/              贴图、模型、blockstate、本地化
    └── templates/META-INF/
        └── neoforge.mods.toml             Mod 元信息模板（构建时展开）
```

## 首次运行

> 需要本地已安装 JDK 21 并配置 `JAVA_HOME`。

```powershell
# 在 voltcraft 目录下
./gradlew runClient        # 启动客户端，进入创造模式可在 VoltCraft 标签页找到低压电缆
./gradlew runServer        # 启动专用服务端
./gradlew build            # 产物输出到 build/libs/
```

如果是第一次启动 Gradle Wrapper：仓库未提供 `gradlew` / `gradlew.bat` 二进制，可在任意已有 Gradle 8.8 的环境运行 `gradle wrapper --gradle-version 8.8` 生成；或直接用 IntelliJ IDEA 打开本目录，IDEA 会自动补齐 wrapper。

## 第一阶段路线（参考设计文档第十一章）

- [x] Mod 脚手架与构建配置
- [x] 占位电缆方块（仅外观）
- [ ] 电压标签数据结构（NBT）
- [ ] 电流自动推算逻辑
- [ ] 变压器（写入电压标签 + 功率守恒）
- [ ] FE 传输集成（NeoForgeCapabilities.ENERGY）
- [ ] 线路（EnergyNetwork）连通图扫描

## 占位贴图

`src/main/resources/assets/voltcraft/textures/block/` 下还没有 `low_voltage_cable.png`。游戏内会显示紫黑棋盘格的 "missing texture"，不影响功能验证。
