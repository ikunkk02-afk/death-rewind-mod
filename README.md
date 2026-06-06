# Death Rewind Mod

死亡回溯模组 — 当你受到致命伤害时，自动回溯到数秒前的状态。

[English](#english) | [中文](#chinese)

---

## 中文

### 功能

- **死亡回溯**：受到致死伤害时自动回退到指定秒数前的状态（位置、生命、背包、经验）
- **方块回溯**：玩家破坏/放置的方块也会一起回退
- **掉落物清理**：回溯时自动清除回溯窗口内的掉落物，防止物品复制
- **图腾效果**：回溯后获得生命吸收 II + 生命恢复 II（等同不死图腾）
- **10 秒无敌**：回溯后短暂无敌，防止连续暴毙

### 模式

| 模式 | 回溯次数 | 配置 |
|------|---------|------|
| 生存 / 创造 | 无限 | 可随时调整 |
| 极限 | 默认 5 次（可配） | 进世界后锁死，开新档才生效 |

### 配置

- **Mod Menu + Cloth Config**：游戏中可视化配置界面
- **配置文件**：`config/death_rewind.json`
- **命令**：`/deathrewind` — 查看状态，`/deathrewind set <次数>` — 设置极限模式次数

### 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| 回溯时长 | 15 秒 | 死亡回退多少秒 |
| 最大回溯次数 | 5 | 仅极限模式生效 |
| 方块回溯 | 开 | 是否回溯方块变化 |
| 区块半径 | 3 | 追踪方块变化的范围 |
| 客户端特效 | 开 | 回溯画面效果 |
| 冷却 | 关 | 两次回溯间需等待 |

### 音效

回溯成功时播放钟声音效（From [SoundReality](https://pixabay.com/users/soundreality-31052304/) on Pixabay）。

### 依赖

- **Minecraft** 26.1.2
- **Fabric Loader** >= 0.19.3
- **Fabric API**
- **Cardinal Components** >= 8.0.0
- **Java** >= 25

可选：
- **Mod Menu** — 模组列表中的配置按钮
- **Cloth Config** — 可视化配置界面

### 许可证

MIT License

---

## English

### Features

- **Death Rewind**: Automatically rewinds to a previous state before fatal damage (position, health, inventory, XP)
- **Block Rewind**: Blocks broken/placed by the player are also restored
- **Item Cleanup**: Dropped items within the rewind window are removed to prevent duplication
- **Totem Effects**: Absorption II + Regeneration II after rewind
- **10s Invulnerability**: Short invulnerability after rewind to prevent chain deaths

### Modes

| Mode | Rewinds | Config |
|------|---------|--------|
| Survival / Creative | Unlimited | Adjustable anytime |
| Hardcore | 5 by default | Locked on world enter, applies to new worlds |

### Configuration

- **Mod Menu + Cloth Config**: In-game config GUI
- **Config file**: `config/death_rewind.json`
- **Command**: `/deathrewind` — view status, `/deathrewind set <count>` — set hardcore limit

### Dependencies

- **Minecraft** 26.1.2
- **Fabric Loader** >= 0.19.3
- **Fabric API**
- **Cardinal Components** >= 8.0.0
- **Java** >= 25

Optional:
- **Mod Menu**
- **Cloth Config**

### License

MIT License
