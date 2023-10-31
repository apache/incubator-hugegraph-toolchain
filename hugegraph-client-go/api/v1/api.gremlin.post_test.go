package v1_test

import (
    "fmt"
    "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go"
    "log"
    "testing"
)

func TestGremlinPostRequest_Do(t *testing.T) {

    client, err := hugegraph.NewDefaultCommonClient()
    if err != nil {
        log.Println(err)
    }
    resp, err := client.Gremlin.GremlinPost(
        client.Gremlin.GremlinPost.WithGremlin("hugegraph.traversal().V().limit(3)"),
    )
    if err != nil {
        log.Fatalln(err)
    }
    fmt.Println(resp.Data.Result.Data)

}
