package hgapi

import (
	"net/http"
	"strconv"
	"time"

	"hugegraph.apache.org/client-go/internal/version"
)

// VERSION returns the package version as a string.
const VERSION = version.Client

// Transport defines the interface for an API client.
type Transport interface {
	Perform(*http.Request) (*http.Response, error)
}

// formatDuration converts duration to a string in the format
// accepted by Hugegraph.
func formatDuration(d time.Duration) string {
	if d < time.Millisecond {
		return strconv.FormatInt(int64(d), 10) + "nanos"
	}
	return strconv.FormatInt(int64(d)/int64(time.Millisecond), 10) + "ms"
}
