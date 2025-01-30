# NoPermsToJoin
This is a Minecraft Paper 1.21.1 plugin that checks whether a player has the permission to join the server in the Velocity group.

```yml
# 检查间隔（秒）
interval: 3

# 需要检查的权限组列表
checked-groups:
  - default

# 踢出玩家的原因，支持多行
kick-reason: |
  &c您无权访问此服务器！
  &e请返回大厅或联系管理员
  &7错误代码：NO_PERMISSION
```

You can specify in `config.yml` how often to traverse online players and check their permissions.

The setting of `checked-groups` means that if a player s in any of the groups below the `checked-groups`, they will be kicked from the server.

When a player is kicked, the `kick-reason` will be shown to them.