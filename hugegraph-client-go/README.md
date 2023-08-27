# go-hugegraph

#### 介绍
基于Go语言的hugegraph client SDK工具

#### 软件架构
软件架构说明


#### 安装教程
```shell
go get github.com/go-hugegraph
```

#### 实现API


|API|说明|
|--|--|
|schema|获取模型schema|
|version|获取版本信息|


#### 使用说明

##### 1.初始化客户端
```go
package main

import "github.com/izliang/hugegraph"
import "github.com/izliang/hugegraph/hgtransport"

func main() {
	
	clinet,err := hugegraph.NewClient(hugegraph.Config{
		Host:  "127.0.0.1",
		Port:  8888,
		Graph: "hugegraph",
		Logger: &hgtransport.ColorLogger{
			Output:             os.Stdout,
			EnableRequestBody:  true,
			EnableResponseBody: true,
		},
	})
	
	if err != nil {
		log.Fatalf("Error creating the client: %s\n", err)
	}
}
```
##### 2.获取hugegraph版本
- 1.使用SDK获取版本信息
```go
func getVersion() {
    
    client := initClient()
    
    res, err := client.Version()
    if err != nil {
    log.Fatalf("Error getting the response: %s\n", err)
    }
    defer res.Body.Close()
    
    fmt.Println(res.Versions)
    
    fmt.Println(res.Versions.Version)
}

```
- 2.结果集响应体
```go
type VersionResponse struct {
	Versions   struct {
		Version string `json:"version"`
		Core    string `json:"core"`
		Gremlin string `json:"gremlin"`
		API     string `json:"api"`
	} `json:"versions"`
}

```
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技
