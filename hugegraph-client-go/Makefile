#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# init project path
HOMEDIR := $(shell pwd)
OUTDIR  := $(HOMEDIR)/output

# Get the go environment required for compilation
export GOENV = $(HOMEDIR)/go.env

GO      := GO111MODULE=on go
GOPATH  := $(shell $(GO) env GOPATH)
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
	$(GO) env -w GO111MODULE=on


#make prepare, download dependencies
prepare: gomod

gomod: set-env
	$(GOMOD) download

#make compile
compile: build

build:
	$(GOBUILD) -o $(HOMEDIR)/hugegraph-client-go
# make test, test your code
test: prepare test-case
test-case:
	$(GOTEST) -v -cover $(GOPKGS)

# make package
package: package-bin
package-bin:
	rm -rf $(OUTDIR)
	mkdir -p $(OUTDIR)
	mv hugegraph-client-go  $(OUTDIR)/
# make clean
clean:
	$(GO) clean
	rm -rf $(OUTDIR)
	rm -rf $(GOPATH)/pkg/darwin_amd64
# avoid filename conflict and speed up build
.PHONY: all prepare compile test package clean build 
