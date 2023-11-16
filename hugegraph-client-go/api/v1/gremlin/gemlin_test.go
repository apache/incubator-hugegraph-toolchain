package gremlin_test

import (
    "fmt"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go"
    "log"
    "testing"
)

func TestGremlin(t *testing.T) {

    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }
    respGet, err := client.Gremlin.Get(
        client.Gremlin.Get.WithGremlin("hugegraph.traversal().V().limit(3)"),
    )
    if err != nil {
        log.Fatalln(err)
    }
    if respGet.StatusCode != 200 {
        t.Error("client.Gremlin.GremlinGet error ")
    }

    respPost, err := client.Gremlin.Post(
        client.Gremlin.Post.WithGremlin("hugegraph.traversal().V().limit(3)"),
    )
    if err != nil {
        log.Fatalln(err)
    }
    fmt.Println(respPost.Data.Result.Data)

}
