# Go huggage

#### Introduction

Hugegraph client SDK tool based on Go language

#### Software Architecture

Software Architecture Description

#### Installation Tutorial

```Shell

Go get github. com/go huggraph

```

#### Implement API

| API     | 说明                      |
|---------|-------------------------|
| schema  | Obtain the model schema |
| version | Get version information |

#### Instructions for use

##### 1. Initialize the client

```Go
package main

import "hugegraph.apache.org/client-go"
import "hugegraph.apache.org/client-go/hgtransport"

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

##### 2. Obtain the hugegraph version

-1. Use the SDK to obtain version information

```Go
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

-2. Result Set Response Body

```Go
Type VersionResponse struct{
    Versions struct{
    Version string ` json: "version"`
    Core string ` json: 'core'`
    Gremlin string ` json: 'gremlin'`
    API string ` json: 'API'`
    }` json: 'versions'`
}
```