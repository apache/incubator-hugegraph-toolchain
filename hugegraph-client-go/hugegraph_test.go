package hugegraph

import (
	"testing"
)

func TestNewDefaultCommonClient(t *testing.T) {
	tests := []struct {
		name    string
		wantErr bool
	}{
		{
			name:    "test",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := NewDefaultCommonClient()
			if (err != nil) != tt.wantErr {
				t.Errorf("NewDefaultCommonClient() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
