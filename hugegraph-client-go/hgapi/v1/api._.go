package v1

import "hugegraph.apache.org/client-go/hgapi"

// Code generated from specification version 5.6.15 (fe7575a32e2): DO NOT EDIT

// API contains the Elasticsearch APIs
//
type APIV1 struct {
	Version Version
}

// New creates new API
//
func New(t hgapi.Transport) *APIV1 {
	return &APIV1{
		Version: newVersionFunc(t),
	}
}
