# Doris 集群搭建手册（Linux版）

## 1. Doris 简介

Doris 是一款高性能的分布式分析型数据库，适合大数据实时分析场景。Doris 集群主要由三类节点组成：

- **FE（Frontend）**：负责元数据管理、查询解析与调度
- **BE（Backend）**：负责数据存储与计算
- **Broker**：负责外部数据导入（可选）

---

## 2. 环境准备

- **操作系统**：推荐 CentOS 7/8 或 Ubuntu 18.04 及以上
- **服务器数量**：最少 3 台（1 FE + 2 BE，生产建议 3 FE + 3 BE 及以上）
- **JDK**：1.8 及以上
- **端口**：开放 8030/8040/9050/9060 等端口
- **关闭 SELinux 和防火墙**

### 2.1 安装 JDK

```bash
sudo yum install java-1.8.0-openjdk-devel -y
# 或 Ubuntu
sudo apt-get install openjdk-8-jdk -y
```

### 2.2 配置主机名与 hosts

编辑 `/etc/hosts`，添加所有节点的 IP 和主机名：

```
192.168.1.101 fe1
192.168.1.102 be1
192.168.1.103 be2
```

---

## 3. 下载 Doris 安装包

前往 [Doris Releases](https://github.com/apache/doris/releases) 下载最新的二进制包。

```bash
wget https://downloads.apache.org/doris/xxx/apache-doris-x.x.x-bin-x86_64.tar.xz
tar -xvf apache-doris-x.x.x-bin-x86_64.tar.xz
```

---

## 4. 部署 FE 节点

### 4.1 拷贝 FE 包到 FE 服务器

```bash
scp -r apache-doris-x.x.x-bin-x86_64/fe fe1:/opt/doris/
```

### 4.2 配置 FE

进入 FE 目录，编辑 `conf/fe.conf`：

```bash
cd /opt/doris/fe
vim conf/fe.conf
```

主要参数：

```
FE_ROLE=MASTER
FE_MASTER_HOST=fe1
```

### 4.3 启动 FE

```bash
sh bin/start_fe.sh --daemon
```

### 4.4 检查 FE 状态

```bash
sh bin/stop_fe.sh status
```

---

## 5. 部署 BE 节点

### 5.1 拷贝 BE 包到 BE 服务器

```bash
scp -r apache-doris-x.x.x-bin-x86_64/be be1:/opt/doris/
scp -r apache-doris-x.x.x-bin-x86_64/be be2:/opt/doris/
```

### 5.2 配置 BE

进入 BE 目录，编辑 `conf/be.conf`：

```bash
cd /opt/doris/be
vim conf/be.conf
```

主要参数：

```
priority_networks = 192.168.1.0/24
storage_root_path = /opt/doris/be/storage
```

### 5.3 启动 BE

```bash
sh bin/start_be.sh --daemon
```

### 5.4 检查 BE 状态

```bash
sh bin/stop_be.sh status
```

---

## 6. 集群注册与授权

### 6.1 登录 FE

```bash
mysql -h fe1 -P 9030 -uroot
```

### 6.2 添加 BE 节点

```sql
ALTER SYSTEM ADD BACKEND "be1:9050";
ALTER SYSTEM ADD BACKEND "be2:9050";
```

### 6.3 查看集群状态

```sql
SHOW PROC '/backends';
SHOW PROC '/frontends';
```

### 6.4 创建数据库和授权用户

```sql
-- 创建数据库
CREATE DATABASE test_db;

-- 创建用户并授权
CREATE USER 'doris_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON test_db.* TO 'doris_user'@'%';
FLUSH PRIVILEGES;
```

---

## 7. 可选：部署 Broker 节点

同理，解压 broker 包，配置 `conf/apache_hdfs_broker.conf`，启动 broker。

---

## 8. Web 管理界面

- 访问 `http://fe1:8030`，默认账号 `root`，无密码。

---

## 9. 常见问题

- **端口未开放**：检查防火墙
- **启动失败**：检查日志 `log/fe.log` 或 `log/be.log`
- **节点无法加入**：检查 hosts 配置和网络连通性

---

## 10. 常用端口

| 端口号 | 说明         |
|--------|--------------|
| 8030   | FE Web UI    |
| 9030   | FE MySQL 协议|
| 8040   | BE Web UI    |
| 9050   | BE 服务端口  |

---

## 11. 参考文档

- [Doris 官方文档](https://doris.apache.org/zh-CN/docs/install/install-deploy/)
- [Doris GitHub](https://github.com/apache/doris)

---

如有任何问题，欢迎随时提问！ 