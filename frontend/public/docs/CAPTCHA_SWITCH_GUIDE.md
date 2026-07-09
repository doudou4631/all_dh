# 服务器验证码开关手册（测试专用）
适用目标：`biaoji.aleo1314.vip` 当前线上环境。
目的：你可以在测试时快速“关闭验证码”，测试完再“恢复验证码”。
## 0. 当前状态（已上线）
- 登录验证码已完成“开关化改造”，并已发布到线上（2026-06-08）。
- 已在服务器实测通过：`true -> false -> true`，每次切换后重启 `geek-admin` 均生效。
- 当前线上默认值：`sys.account.captchaEnabled=true`（开启）。

## 1. 当前生效机制（已开关化）
- 开关键：`sys.account.captchaEnabled`（数据库 `verifynum.sys_config`）。
- **登录**与**注册**验证码校验都受该开关控制。
- 开关值语义：
  - `false`：关闭验证码校验
  - `true`：开启验证码校验

注意：配置有缓存，切换后建议重启后端服务让其立即生效。
兼容性注意：
- 如果后续部署了“开关化改造之前”的旧 Jar，登录可能会恢复为强制验证码。
- 若发现“改开关不生效”，先核对当前运行 Jar 是否包含本次改造。

---

## 2. 日常“关闭验证码”（测试时）
> 以下流程建议在服务器 SSH 交互会话里执行，避免本地 PowerShell 引号转义问题。

### 2.1 登录服务器
```bash
ssh -i ~/.ssh/id_rsa_43_142_125_17 ubuntu@43.142.125.17
```

### 2.2 关闭验证码开关
```bash
sudo mysql -D verifynum -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';"
```

### 2.3 重启后端（让配置立即生效）
```bash
sudo systemctl restart geek-admin
sudo systemctl status geek-admin --no-pager
```

### 2.4 确认当前状态
```bash
sudo mysql -N -D verifynum -e "SELECT config_key, config_value FROM sys_config WHERE config_key='sys.account.captchaEnabled';"
```
预期输出：`sys.account.captchaEnabled    false`

---

## 3. 日常“恢复验证码”（测试后）
### 3.1 恢复开关
```bash
sudo mysql -D verifynum -e "UPDATE sys_config SET config_value='true' WHERE config_key='sys.account.captchaEnabled';"
```

### 3.2 重启后端
```bash
sudo systemctl restart geek-admin
sudo systemctl status geek-admin --no-pager
```

### 3.3 确认当前状态
```bash
sudo mysql -N -D verifynum -e "SELECT config_key, config_value FROM sys_config WHERE config_key='sys.account.captchaEnabled';"
```
预期输出：`sys.account.captchaEnabled    true`

---

## 4. 快速核对清单（每次切换后建议做）
1. `geek-admin` 服务状态是 `active (running)`。
2. `sys.account.captchaEnabled` 的值和预期一致。
3. 页面手工验证：
   - 关闭时：登录/注册不再要求验证码。
   - 恢复时：验证码恢复。

---

## 5. 常见问题
### 5.1 改了开关但页面行为没变化
优先检查：
1. 是否重启了 `geek-admin`。
2. 是否改的是线上数据库 `verifynum`。
3. 当前运行的后端 Jar 是否已是最新发布版本。

### 5.2 测试后忘记恢复
测试结束务必恢复为 `true`，避免线上安全策略被长期关闭。

