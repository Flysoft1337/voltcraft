# VoltCraft

> Minecraft 1.21.1 / NeoForge / Java 21
> 真实电力传输与保护层 — 电压等级、电流推算、漏保、空开、接地

VoltCraft 不发电。发电交给格雷科技、沉浸工程这类工业 mod，VoltCraft 在它们之上加一层真实的电气传输与保护：电压等级、电流自动推算、漏保 / 空开跳闸、火零地接线规范、接地系统。底层能量单位用 Forge Energy（FE），保证与主流科技 mod 兼容。

## 为什么开源

不开源的 mod 是没有出路的。整合包作者要能审计电力逻辑、玩家要能定位 bug、其他开发者要能基于这套保护体系扩展自己的器件。MIT License 允许任意使用、修改、再分发。

## 仓库结构

```
.
├── LICENSE                       MIT
├── README.md                     当前文件
├── MC电力Mod设计文档.md          完整设计文档（v0.2）
└── voltcraft/                    Mod 源代码（NeoForge MDK 项目）
    ├── build.gradle
    ├── gradle.properties
    └── src/main/{java,resources,templates}/
```

设计文档是产品级的需求规格，包含线路（EnergyNetwork）定义、变压器功率守恒模型、漏保阈值算法、触电伤害数值表、配置项清单等。先读它再读代码。

## 当前状态

第一阶段进行中。完成情况见 [设计文档第十一章](./MC电力Mod设计文档.md#十一开发阶段规划)。

可见成果：
- [x] NeoForge MDK 脚手架（Gradle 9.2.1 / moddev 2.0.141）
- [x] 四级电缆方块（low / medium / high / extra-high）
- [x] 电压等级 + 电缆等级强绑定（`VoltageTier` / `CableTier` 枚举）
- [x] 电缆 BlockEntity，电压标签 NBT 持久化
- [ ] 线路（EnergyNetwork）连通图扫描
- [ ] 变压器（写入电压标签 + 功率守恒）
- [ ] FE Capability 集成

## 构建

```powershell
cd voltcraft
./gradlew build           # 产物 build/libs/voltcraft-0.1.0.jar
./gradlew runClient       # 启动开发用客户端
./gradlew runServer       # 启动开发用服务端
```

国内首次拉 NeoForge 反编译产物会从 `maven.neoforged.net` 下载几百 MB；项目里已配好阿里云镜像 + 项目级 HTTP 代理（`gradle.properties` 中的 `systemProp.http.proxyHost=127.0.0.1:7897`，按需调整）。

## 设计原则

- **真实物理模型优先**：变压器遵循功率守恒（`P_in × η = P_out`），电压标签是线路属性而非电缆属性，多电源并联前会校验电压一致
- **错误由玩家造成**：漏电不会随机发生，必须由"线缆暴露在水边 / 持续过载 / 接线端子接错"等具体行为触发
- **数值全部可配置**：阈值、损耗率、伤害值通过 `ModConfigSpec` 暴露，整合包作者可调

## License

[MIT](./LICENSE)
