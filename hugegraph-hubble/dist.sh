cd /home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/../hubble-fe || exit 1
                                    export CI=false
                                    yarn install --network-timeout 600000 && yarn build || exit 1
                                    echo -e "Hubble-FE build successfully.\n"

                                    cd /home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/.. && pwd
                                    rm -rf apache-hugegraph-hubble-incubating-1.0.0/ui
                                    cp -r /home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/../hubble-fe/build apache-hugegraph-hubble-incubating-1.0.0/ui

                                    tar -zcvf /home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/../target/apache-hugegraph-hubble-incubating-1.0.0.tar.gz apache-hugegraph-hubble-incubating-1.0.0 || exit 1
                                    cp -r apache-hugegraph-hubble-incubating-1.0.0 ./hubble-dist
                                    echo -n "apache-hugegraph-hubble-incubating-1.0.0 tar.gz available at: "
                                    echo "/home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/../target/apache-hugegraph-hubble-incubating-1.0.0.tar.gz"
                                    rm -f /home/root1/lx/Development/incubator-hugegraph-toolchain/hugegraph-hubble/hubble-dist/../dist.sh