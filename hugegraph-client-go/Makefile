# init project path
HOMEDIR := $(shell pwd)
OUTDIR  := $(HOMEDIR)/output

# 设置编译时所需要的 go 环境
export GOENV = $(HOMEDIR)/go.env

GO      := go
GOMOD   := $(GO) mod
GOBUILD := $(GO) build
GOTEST  := $(GO) test -race -timeout 30s -gcflags="-N -l"
GOPKGS  := $$($(GO) list ./...| grep -vE "vendor")

# test cover files
COVPROF := $(HOMEDIR)/covprof.out  # coverage profile
COVFUNC := $(HOMEDIR)/covfunc.txt  # coverage profile information for each function
COVHTML := $(HOMEDIR)/covhtml.html # HTML representation of coverage profile

# make, make all
all: prepare compile package

set-env:
	$(GO) env 


#make prepare, download dependencies
prepare: gomod

gomod: set-env
	$(GOMOD) download -x || $(GOMOD) download -x

#make compile
compile: build

build:
	$(GOBUILD) -o $(HOMEDIR)/go-hugegraph

# make test, test your code
test: prepare test-case
test-case:
	$(GOTEST) -v -cover $(GOPKGS)

# make package
package: package-bin
package-bin:
	rm -rf $(OUTDIR)
	mkdir -p $(OUTDIR)
	mv go-hugegraph  $(OUTDIR)/

# make clean
clean:
	$(GO) clean
	rm -rf $(OUTDIR)
	rm -rf $(HOMEDIR)/go-hugegraph
	rm -rf $(GOPATH)/pkg/darwin_amd64

# avoid filename conflict and speed up build
.PHONY: all prepare compile test package clean build 
