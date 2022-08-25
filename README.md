# SakuraMail

> 这是一个发送系统邮件的解决方案，适用于各种复杂情况

## 插件特点

### 模拟情景

* 给 2022/08/25 14点 ~ 16点 在线时长满1小时的玩家发送邮件
* 给超过30天未登录且总游戏时间超过50小时的老玩家发送邮件
* 每周周一晚上8点给在线的玩家发送邮件
* 给今天在线超过10小时的玩家发送邮件
* 给当前在线/离线的玩家发送邮件
* 给服务器新人发送邮件
* 按权限发送邮件
* ......

### 其他特点

* 插件全程异步操作，主线程0卡顿
* 支持 MySQL、MariaDB、SQLite、H2、Oracle、PostgreSQL、SQLServer 数据库
* 高度自定义的ui、消息

### 基本信息

* 插件使用 `Kotlin` 语言编写,开源地址 https://github.com/SakuraTown/SakuraMail

* 兼容的MC版本: `1.7.10` ~` 1.19.2` (仅测试1.19.2)

* 软依赖 `PlaceholderAPI`

* 运行环境: `jre8` 及以上

* 依赖(在第一次插件启用时将自动下载)

    * org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10

    * org.jetbrains.exposed:exposed-core:0.39.2

    * org.jetbrains.exposed:exposed-dao:0.39.2

    * org.jetbrains.exposed:exposed-jdbc:0.39.2

    * org.jetbrains.exposed:exposed-java-time:0.39.2

    * com.zaxxer:HikariCP:4.0.3

    * org.quartz-scheduler:quartz:2.3.2

    * com.github.cryptomorin:XSeries:9.0.0

----

## 安装

### 安装方式一

将服务端关闭，将插件复制到服务端下的 `plugins` 文件夹中，启动服务端

### 安装方式二

将插件复制到服务端下的 `plugins` 文件夹中，使用 `pluginman` `yum` `serverutils` 等插件不停服载入插件

### 出现以下信息说明插件正常启动

![image-20220825221511845](https://pic.imgdb.cn/item/6307841016f2c2beb1680684.png)



---

## 配置

> 插件的邮件系统分为3个部分：邮件、邮件接收者、邮件发送者

### 邮件配置

> 一封邮件的所有接收者的附件与标题都是一样的，邮件可以被重复发送，当一封邮件被删除时，所有人的该邮件将被删除

在 `plugins\SakuraMail`文件夹中, `mails.yml` 为储存本地邮件的配置文件。

邮件分为 `id`、`expire`(有效时间) 、`icon`(显示的图标)、`title`(邮件标题)、`items`(附件物品)、`fakeItems`(虚拟物品的位置)
、`commands`(命令) 其中只有 `id` 是必须的

你有2种方式来创建邮件:

1. 使用命令 ` sakuramail systemMail create [id]` 其ID为任意唯一的字符串，如果手上拿着物品，则这个物品为 icon
   ，之后会打开一个物品栏，放入附件物品(会记录位置)，当关闭物品栏时邮件完成创建。`expire` `fakeItems` `commands`无法通过命令创建。

2. 通过配置修改 `mails.yml`

   如下

   ~~~ yaml
   mails:
     test: # 随便取名，建议与ID一致
       id: test # 唯一ID
       expire: P30D # 过期时间，格式见下文 #时间格式
       icon: #图标，格式遵循XItemStack的格式https://github.com/CryptoMorin/XSeries/wiki/XItemStack
         material: STONE 
       title: aaa  # 标题(箱子界面的标题)
       items: ...(省略)
       fakeItems: '47' # 虚拟物品的位置，该位置的物品将不会发给玩家
       commands: 
         - spawn                     # 以玩家省份运行命令
         - CMD:eco give %player% 200 # 以控制台省份运行命令 %player% 将转为领取的玩家名字
         - OP:fly                    # 以玩家为OP运行命令
   ~~~

**
需要注意的是,以下选项建议为true，将支持所有物品，为false虽然能编辑附件，但是有可能不兼容一些特殊物品，比如拥有自定义附魔的物品**

~~~ yaml
# 是否对邮件的 items 加密压缩 
# 如果为true，只能使用命令/sakuramail systemMail edit 命令在游戏内修改物品
# 请注意，设置为 false 可能不支持某些非原版物品!
isEncrypted: true
~~~

#### 相关命令

`sakuramail systemMail` 为邮件命令的根节点

有以下子节点

~~~ tex
create [id] <title>    创建系统邮件,手上的是图标
edit [id]              编辑系统邮件的物品,其他操作请从yml修改
upload                 上传邮件数据至数据库
download               从数据库下载邮件数据至本地
remove [id]            删除邮件
~~~

---

## 邮件接收者设置

在 `plugins\SakuraMail`文件夹中, `receivers.yml` 为储存邮件接收者的配置文件。

你有2种方式来创建邮件接收者:

1. 使用命令 ` sakuramail receiver add [id] <parms>`  为某个id的接收者添加参数，如果不存在则将创建新的。

2. 通过配置修改 `receivers.yml`

   如下是一个最简单的选择在线玩家的选择器，id为 `在线玩家` 参数为 online

   ~~~ yaml
   # 邮件目标选择器节点
   receivers: 
     在线玩家: # 邮件目标选择器的ID
      - online # 参数
      - ...
   ~~~

### 参数说明

> 邮件接收者拥有丰富的参数选择，可以适应大多数情况
>
> 每条参数都会匹配所有符合情况的人

参数的解析自上而下，但是有一类参数的解析优先—— `一次游戏sql参数`

所有`一次游戏sql参数`将合并为一条sql语句以节省查询次数

**sql参数将会从数据库查询匹配的接收者，请尽量少用以节省性能消耗**

[time]、[duartion]的格式见下 # 时间格式

#### **一次游戏sql参数**

玩家从**进服**到**退服**将被记录，以下参数就是对这一次游戏的选择

| 参数                                     | 说明                                        |
| ---------------------------------------- | ------------------------------------------- |
| loginTime before [time]                  | 登陆时间 在 [time]之前                      |
| loginTime after [time]                   | 登陆时间 在 [time]之后                      |
| loginTime between [time1] [time2]        | 登陆时间 在 [time1] 和 [time2]之间          |
| quitTime before [time]                   | 退出时间 在 [time]之前                      |
| quitTime after [time]                    | 退出时间 在 [time]之后                      |
| quitTime between [time1] [time2]         | 退出时间 在 [time1] 和 [time2]之间          |
| playtime greater [duartion]              | 一次游戏时间大于 [duartion]                 |
| playtime less [duartion]                 | 一次游戏时间小于 [duartion]                 |
| playtime between [duartion1] [duartion1] | 一次游戏时间在 [duartion1] [duartion2] 之间 |

#### 其他sql参数

<start> <end> 为 `totaltime` 专用可选参数,分别为 `统计时间`的 `起始`和`终点`不选则默认所有时间范围 格式与[time] 相同

| 参数                                                    | 说明                                    |
| ------------------------------------------------------- | --------------------------------------- |
| totaltime greater [duartion] <start> <end>              | 游戏时间大于 [duartion]                 |
| totaltime less [duartion] <start> <end>                 | 游戏时间小于 [duartion]                 |
| totaltime between [duartion1] [duartion1] <start> <end> | 游戏时间在 [duartion1] [duartion1] 之间 |
| hasmail [id]                                            | 拥有 ID 为 [id]的邮件                   |
| all                                                     | 所有记录过的玩家(在安装本插件之后)      |

#### 其他参数 (范围在触发的那个服务器里)

| 参数                              | 说明                                  |
| --------------------------------- | ------------------------------------- |
| online                            | 所有在线玩家                          |
| offline                           | 所有离线玩家                          |
| permission [permission]           | 所有拥有 [permission] 权限的在线玩家  |
| gamemode [gamemode]               | 所有 游戏模式为 [gamemode] 的在线玩家 |
| uuids [uuid1];[uuid2];[uuid3].... | 一组uuid的玩家                        |
| names [name1];[name2];[name3].... | 一组name的玩家（从登录过的玩家里找）  |
| limit [count]                     | 限制集合最多 [count] 个               |

### 参数集合操作

参数的解析自上而下，彼此为独立的集合，而集合之间的关系也可以指定

比如

- online
- permission xxx

表示既是在线玩家 又是 拥有 xxx 权限的玩家 所有参数默认为集合的**与**的关系，也就是**交集**

集合关系的声明位于参数之前 格式为: `关系,参数 `以英文逗号分隔

集合关系有如下

| 关系   | 说明                                                         |
| ------ | ------------------------------------------------------------ |
| and    | 2个集合取交集                                                |
| andNot | 在所有接收者(对应all参数)的集合中减去本行参数的玩家组成的集合再取**交集** |
| or     | 2个集合的并集，也就是相加 与add一致                          |
| orNot  | 在所有接收者(对应all参数)的集合中减去本行参数的玩家组成的集合再取**并集** |
| add    | 2个集合相加，也就是并集 与or一致                             |
| remove | 从上面参数的集合中减去本集合的接收者                         |

比如

* online
* or,offline

=> 效果与 `localAll` 相同

* online

* remove,permission xxx

=> 在线玩家中没有xxx权限的玩家

#### 相关命令

`sakuramail receiver` 为邮件接收者命令的根节点

有以下子节点

~~~ tex
set [id] <index> <parms>  设置邮件接收者参数
add [id] <parms>          设置邮件接收者参数
remove [id]               删除邮件接收者
test [id]                 测试邮件接收者
upload [id]               上传邮件接收者数据
download [id]             下载邮件接收者数据
export [id] <type>        导出符合邮件接收者的玩家
~~~

---

## 邮件发送者设置

在 `plugins\SakuraMail`文件夹中, `senders.yml` 为储存邮件发送者的配置文件。

你有2种方式来创建邮件发送者:

1. 使用命令 ` sakuramail sender create [id] [type] [param]`

   其中 [id] 为唯一id [type] 为发送者的类型，目前有 `onTime` `period` `login` `manual` 4种 [parma] 为对应类型的参数

2. 通过配置修改 `senders.yml`

   ~~~ yaml
   手动: # 邮件发送者ID
     type: manual 
     receivers:
       - online
     mails:
       - test
     
   准时: # 邮件发送者ID
     type: onTime 
     param: 2022-08-20T20:00:00 ## 只有这种格式
     receivers:
       - online
     mails:
       - test
     
   周期: # 邮件发送者ID
     type: period
     param: 0 0 * * * ?  ## cron 表达式 生成器 https://cron.qqe2.com/
     receivers:
       - online
     mails:
       - test
     
   登录: # 邮件发送者ID
     type: login # 该类型在玩家登录时触发
     receivers:
       - online
     mails:
       - test
     
   ~~~

   特别的: login 类型的发送者中的接收者的参数中如果有 `%uuid%` 将被替换为哦该登录玩家的uuid

   **一个发给新人的邮件应该这么写**

   receivers.yml

   ~~~ yaml
   receivers:
     login_player:
       - uuids %uuid%
     logined_before:
       - logintime before -PT1S
   ~~~

   senders.yml

   ~~~ yaml
   登录:
     type: login
     receivers:
       - login_player
       - remove,logined_before
     mails:
       - test
   ~~~

   如上，你会发现 邮件发送者的 receivers 也接受 集合操作 这样可以有更多的组合以满足不同情况

   从**登录的玩家**(login_player)中 **减去** **以前登录过的玩家**(logined_before)，剩下的就是**新玩家**了

---

## 其他设置

`lang.yml` 中为玩家能看到的消息

`database.yml`为数据库设置，默认使用 `H2` 数据库,生成的默认文件名为 `database.mv.db`

ui目录下为邮箱界面的设置，可以通过修改文件来修改邮箱界面

`quartz.properties`为 quartz 库的设置，不懂可以不管

相关信息在文件中都有注释

## 命令

插件主命令根节点为 `sakuramail` 别名 `smail` `mail`

输入根节点可查看所有子节点的信息

命令的权限为 `sakuramail.节点名称` 可嵌套

玩家默认可用的命令只有一个 `sakuramail open` 打开自己的邮箱

## 跨服

如果你想要跨服，请先设置好数据库

将 **邮件、邮件发送者、邮件接收者** 的信息使用 **upload** 命令上传到数据库

**请使用一台服务器作为配置服务器和发送服务器，以免重复发送邮件**

## 其他说明

所有 yml 配置都会自动重载 无需命令

## 构建插件

推荐在IDEA中通过 gralde 的 build 构建

或者控制台运行`./gradlew build`
