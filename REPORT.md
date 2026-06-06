# Death Rewind - 死亡回溯模组

**Minecraft 26.1.2 | Fabric | Java 25 | MIT**

## 核心功能

受到致命伤害时自动回溯到 15 秒前的存档点，恢复玩家状态、背包、周围方块和容器物品。

## 机制

| 功能 | 说明 |
|------|------|
| 存档点 | 每 5 秒自动创建，保留最近 3 个 |
| 回溯范围 | 玩家状态 + 背包 + 方块 + 箱子/熔炉等容器 |
| 掉落物清理 | 回溯窗口内的掉落物自动清除，防复制 |
| 世界时间 | 白天/黑夜同步恢复 |
| 无敌时间 | 回溯后 10 秒无敌 + 不死图腾效果（生命吸收 II + 生命恢复 II） |
| Timeline 隔离 | 连续死亡不会污染存档 |

## 模式

- **生存/创造**：无限次回溯
- **极限模式**：默认 5 次，可调（进世界后锁定）

## 配置

- Mod Menu + Cloth Config 图形界面
- `/deathrewind` 命令
- 配置文件：`config/death_rewind.json`

## 依赖

- Fabric API
- Cardinal Components ≥ 8.0.0
- 可选：Mod Menu、Cloth Config

---

[GitHub](https://github.com/ikunkk02-afk/death-rewind-mod)
