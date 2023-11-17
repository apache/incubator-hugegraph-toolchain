package v1

import "github.com/apache/incubator-hugegraph-toolchain/hugegraph-client-go/api"

type APIV1 struct {
    Version Version
    Schema  Schema
}

// New creates new API
func New(t api.Transport) *APIV1 {
    return &APIV1{
        Version: newVersionFunc(t),
        Schema:  newSchemaFunc(t),
    }
}
