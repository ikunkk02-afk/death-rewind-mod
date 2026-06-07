# Death Rewind / 死亡回溯

一个 Fabric 单人向死亡回溯模组。

玩家受到致命伤害时，模组不会立刻让你死亡，而是把你拉回几秒前的状态：位置、血量、背包、经验、世界时间，以及玩家附近的方块变化都会尽量恢复。简单说，就是给生存档加一个“死前倒带”。

---

## 中文介绍

### 这个模组是干什么的？

死亡回溯会持续保存玩家最近的安全状态。默认每 5 秒保存一个存档点，死亡时回到约 15 秒前。

回溯时会恢复：

- 玩家位置
- 血量、饥饿值、经验、背包
- 世界时间
- 玩家附近的方块变化
- 箱子等方块实体的 NBT 数据
- 回溯窗口内新掉落的物品会被清理，避免刷物品

回溯完成后，玩家会获得短暂无敌和类似不死图腾的恢复效果，避免刚回去又被秒杀。

### 模式规则

普通生存世界里，默认可以无限回溯。

极限模式里，默认最多回溯 5 次。进入极限世界后，最大次数和回溯时长会被锁定，防止玩家临死前临时改配置作弊；想改规则需要开新世界。

### 方块回溯范围

方块不是全世界记录，而是记录玩家附近的区块。

默认区块半径是 3，约等于玩家附近 6×6 个区块，也就是大约 96×96 格的水平范围。这样可以在保留“死亡后周围环境回到过去”的效果时，避免记录整个世界导致卡顿。

### C2ME / 优化模组兼容说明

本模组暂不支持 C2ME 下的完整方块回溯。

C2ME 可能会把一些世界逻辑放到非主线程执行，而 Minecraft 的玩家列表、方块实体 NBT、世界方块状态并不适合在这些线程里随便读取。为了避免出现偶发不能放方块、方块记录冲突、整合包线程异常等问题：

检测到 C2ME 时，模组会自动关闭“方块回溯/方块检测”，并在玩家进世界时提示。

这不会关闭死亡回溯本体。玩家位置、背包、血量、经验、世界时间等仍然会回溯，只是方块变化不会自动倒回。

如果你仍然想强行启用方块回溯，可以在配置里：

1. 关闭“优化模组兼容模式”
2. 再打开“启用方块回溯”

不推荐在大型整合包里强行打开。打开后如果出现不能放方块或回溯异常，建议重新开启“优化模组兼容模式”。

### 配置方式

支持 Mod Menu + Cloth Config 的游戏内配置界面。

配置文件位置：

`config/death_rewind.json`

命令：

- `/deathrewind` 查看当前状态
- `/deathrewind set <次数>` 设置极限模式最大回溯次数
- `/deathrewind reload` 重新加载配置

### 主要配置项

- 回溯时长：默认 15 秒
- 存档点间隔：默认 5 秒
- 最大回溯次数：默认 5，仅极限模式生效
- 启用方块回溯：默认开，但检测到 C2ME 且兼容模式开启时会自动关闭
- 优化模组兼容模式：默认开，用来规避 C2ME 等优化模组导致的方块记录问题
- 区块半径：默认 3
- 每刻最大恢复方块数：默认 128
- 客户端特效：默认开
- 存档点通知：默认开
- 冷却：默认关

### 依赖

必需：

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API
- Cardinal Components >= 8.0.0
- Java >= 25

可选：

- Mod Menu：在模组列表里打开配置界面
- Cloth Config：可视化配置界面

### 音效来源

回溯完成音效来自 Pixabay：SoundReality。

---

## English

Death Rewind is a single-player Fabric mod that rewinds the player when fatal damage would normally kill them.

Instead of dying immediately, the player is restored to a recent checkpoint. The mod restores player state, inventory, XP, position, world time, and nearby block changes when block rewind is enabled.

### Features

- Automatic rewind on fatal damage
- Restores position, health, hunger, inventory, XP, and world time
- Optional nearby block rewind
- Block entity NBT snapshots for containers and similar blocks
- Dropped-item cleanup to reduce duplication issues
- Short invulnerability after rewinding
- Action-bar checkpoint notifications
- Rewind sound and client visual effects

### C2ME / optimization mod warning

Block rewind is not officially supported with C2ME.

C2ME may run some world logic off the main server thread. Reading player lists, block entity NBT, and world block state from those paths can cause unstable behavior in large modpacks. To avoid occasional block placement failures or thread-related issues, Death Rewind automatically disables block rewind when C2ME is detected and Optimization Mod Compatibility is enabled.

The core death rewind still works: position, inventory, health, XP, and world time can still be restored. Only block tracking is disabled.

If you want to force-enable block rewind anyway, open the config screen, disable “Optimization Mod Compatibility”, and then enable “Block Rewind” again. This is not recommended for large modpacks.

### Configuration

In-game config is available through Mod Menu + Cloth Config.

Config file:

`config/death_rewind.json`

Commands:

- `/deathrewind` shows current status
- `/deathrewind set <count>` sets the hardcore rewind limit
- `/deathrewind reload` reloads the config

### Dependencies

Required:

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API
- Cardinal Components >= 8.0.0
- Java >= 25

Optional:

- Mod Menu
- Cloth Config

### License

MIT License
