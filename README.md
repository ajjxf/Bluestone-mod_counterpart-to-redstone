# Bluestone mod(counterpart to redstone)
A blue counterpart to redstone. Adds **bluestone wire / repeater / comparator / torch / block** as a fully isolated, parallel power system that behaves like redstone but never interacts with it.

> This mod is partially written with AI assistance, but heavily references vanilla redstone code. It
> may contain undiscovered bugs — please report them on GitHub. Any behavioural difference between the
> bluestone power system and the vanilla redstone power system should be considered a bug.

## What it adds
| Block / Item | id | Counterpart |
|---|---|---|
| Bluestone (dust) | `bluestone` (item) / `bluestone_wire` (block) | redstone / redstone_wire |
| Bluestone Torch (floor + wall) | `bluestone_torch` / `bluestone_wall_torch` | redstone_torch |
| Bluestone Repeater | `bluestone_repeater` | repeater |
| Bluestone Comparator | `bluestone_comparator` | comparator |
| Block of Bluestone | `bluestone_block` | redstone_block |
| Bluestone Ore / Deepslate Bluestone Ore | `bluestone_ore` / `deepslate_bluestone_ore` | redstone_ore / deepslate_redstone_ore |
| Purplestone (dust) | `purplestone` (item) / `purplestone_wire` (block) | Bridge wire — connects redstone & bluestone |
| Redstone-Bluestone Signal Converter | `converter_repeater` | Two-mode gate (right-click to toggle) |

All items live in a new **Bluestone** creative tab (next to the vanilla Redstone tab).

## How isolation works (Option Y)
Blue components emit **no real redstone power**; they keep their own blue power in block state. This
makes the two systems cleanly separated:

- **Red ignores blue for free.** Redstone wire / repeater / comparator / torch read real redstone
  power, which blue never emits, so they never see blue.
- **Blue ignores red by construction.** Blue components read inputs through `BluePower`, which skips
  all red-coloured components (redstone wire/repeater/comparator/torch/block) and attributes the
  colour of strongly-powered solid blocks (powered by a red component -> invisible to blue; by a
  neutral source or a blue component -> visible to blue).
- **Blue activates reactive blocks** via one mixin on `World.isReceivingRedstonePower` that OR-s in
  `BluePower`. Pistons, doors, lamps, dispensers, hoppers, powered rails, ... all respond to blue
  exactly as they do to red (quasi-connectivity included), because they call the same method.
- **Redstone torches stay red-only**: a mixin redirects the torch's power check to a red-only helper
  (so the global OR-blue does not let blue turn a red torch off). Blue torches use the same redirect
  to read `BluePower` instead.

### Coloured vs neutral
- **Coloured** (mutually exclusive): redstone & bluestone wire/repeater/comparator/torch/block.
- **Neutral** (shared by both): levers, buttons, pressure plates, weighted pressure plates, tripwire
  hooks, daylight detectors, target blocks, sculk sensors, calibrated sculk sensors, lecterns,
  chiseled bookshelves, detector rails, observers. Identification is generic: any block that
  `emitsRedstonePower()` and is not a coloured component is neutral, so unlisted sources are covered.
- Neutral sources are **pure sources** (output set by a non-redstone trigger), so they cannot bridge
  the two systems.

### Purplestone (bridge wire)
Purplestone dust has the same power behaviour as redstone but connects to **both** redstone and
bluestone wires, acting as a signal bridge at wire level. Crafted from 1 redstone + 1 bluestone.

### Redstone-Bluestone Signal Converter
A repeater-style gate with two modes toggled by right-click (like a comparator):
- **Red→Blue** (default): reads redstone input, outputs blue signal.
- **Blue→Red**: reads blue input, outputs redstone signal.

Crafted with mixed redstone/bluestone torches + purplestone + stone.

## Installation
**Required on both client and server.** Place the mod jar and [Fabric API](https://modrinth.com/mod/fabric-api)
into the `mods/` folder of both the client and the server. The mod will not function if installed on
only one side.

Requires Minecraft 1.20.1, Fabric Loader, and Fabric API (`0.92.11+1.20.1` or later).

## "Logic identical to redstone"
Repeaters / comparators / torches / blocks **subclass the vanilla classes** and only override input
reading (to use blue) and suppress real emission; all update scheduling, delays, locking, compare/
subtract logic, and detection order are inherited — so vanilla quirks (e.g. observer-vs-comparator
update-order interactions) carry over to the blue counterparts.

`BluestoneWireBlock` is reimplemented from `Block` (vanilla `RedstoneWireBlock`'s connection logic is
static/private and can't be cleanly recoloured by subclassing). It mirrors vanilla's algorithm
(`wiresGivePower`-style separation, up-steps, dot/line/cross, `scheduleBlockTick`, power-decreases-
by-1-per-block) but reads `BluePower` instead of real redstone.

## Known limitations / risks
- `BluestoneWireBlock` is a hand-written reimplementation; edge cases (up-steps, dot/line switching,
  update ordering) may need tuning to perfectly match vanilla.
- Other mods' redstone components are not filtered and could cross-talk with blue.
- Performance: `World.isReceivingRedstonePower` now also evaluates `BluePower`, which (for solid
  blocks) inspects neighbours-of-neighbours. Heavy redstone may cost more; a cache can be added.



---

## 说明

Bluestone 是红石的蓝色镜像。添加了**蓝石粉/蓝石中继器/蓝石比较器/蓝石火把/蓝石块**作为完全隔离的并行能量系统，行为与红石一致但互不干扰。

> 本模组部分使用 AI 编写，但大量参考原版红石代码，可能含有未发现的 bug——欢迎到 GitHub 提交新的 issue。所有蓝石能量系统中表现与原版红石能量系统不同的，一律视作 bug。

### 添加内容
| 方块/物品 | id | 对应原版 |
|---|---|---|
| 蓝石（粉） | `bluestone`（物品）/ `bluestone_wire`（方块） | 红石 / redstone_wire |
| 蓝石火把（地面+墙面） | `bluestone_torch` / `bluestone_wall_torch` | 红石火把 |
| 蓝石中继器 | `bluestone_repeater` | 中继器 |
| 蓝石比较器 | `bluestone_comparator` | 比较器 |
| 蓝石块 | `bluestone_block` | 红石块 |
| 蓝石矿石/深层蓝石矿石 | `bluestone_ore` / `deepslate_bluestone_ore` | 红石矿石 / 深层红石矿石 |
| 紫石（粉） | `purplestone`（物品）/ `purplestone_wire`（方块） | 桥接导线——连接红石与蓝石 |
| 红蓝信号转换器 | `converter_repeater` | 双模式门（右键切换） |

所有物品位于新建的**蓝石方块**创造模式物品栏中。

### 安装
**客户端和服务端都需要安装。** 将模组 jar 和 [Fabric API](https://modrinth.com/mod/fabric-api) 放入客户端和服务端的 `mods/` 文件夹。仅安装一端时模组无法正常工作。

需要 Minecraft 1.20.1、Fabric Loader 和 Fabric API（`0.92.11+1.20.1` 或更高版本）。

### 隔离原理（Option Y）
蓝石组件**不发射真实红石能量**，而是在方块状态中维护独立的蓝石能量。两个系统因此完全隔离：

- **红石无视蓝石**：红石导线/中继器/比较器/火把读取真实红石能量，蓝石从不发射，所以红石系统看不到蓝石。
- **蓝石无视红石**：蓝石组件通过 `BluePower` 读取输入，跳过所有红色组件（红石粉/中继器/比较器/火把/红石块），对被红石强充能的实体方块也不可见。
- **蓝石激活反应方块**：通过 `World.isReceivingRedstonePower` 的一个 Mixin 将蓝石能量 OR 进去。活塞、门、灯、发射器、漏斗、充能铁轨等都像响应红石一样响应蓝石。
- **红石火把仅响应红石**：Mixi 将火把的能量检查重定向到仅红石的辅助方法，使蓝石无法熄灭红石火把。

### 紫石（桥接导线）
紫石粉具有与红石相同的能量行为，但可以同时连接红石和蓝石导线，在导线层面桥接信号。配方：1红石 + 1蓝石 = 2紫石。

### 红蓝信号转换器
中继器造型的双模式门，右键切换模式（类似比较器）：
- **红转蓝**（默认）：读取红石输入，输出蓝石信号。
- **蓝转红**：读取蓝石输入，输出红石信号。

配方：红石火把+蓝石火把+紫石+石头。

### 已知限制
- `BluestoneWireBlock` 是AI重写的实现，边缘情况（上台阶、点/线切换、更新顺序）可能需要微调以完美匹配原版。
- 其他模组的红石组件未经过滤，可能与蓝石交叉干扰。
- 性能：`World.isReceivingRedstonePower` 现在也计算蓝石能量，将其应用到重型红石电路可能损耗/需要更多性能。
