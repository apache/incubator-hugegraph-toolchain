#!/bin/bash
function get_params() {
  echo "params: $*"
  ENGINE_PARAMS=""
  HUGEGRAPH_PARAMS=""
  while (("$#")); do
    case "$1" in
      --graph | --schema | --host | --port | --username | --token | --protocol | \
      --trust-store-file | --trust-store-password | --clear-all-data | --clear-timeout | \
      --incremental-mode | --failure-mode | --batch-insert-threads | --single-insert-threads | \
      --max-conn | --max-conn-per-route | --batch-size | --max-parse-errors | --max-insert-errors | \
      --timeout | --shutdown-timeout | --retry-times | --retry-interval | --check-vertex | \
      --print-progress | --dry-run | --help)
        HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS $1 $2"
        shift 2
        ;;
      --file)
        file=$2
        shift 2
        ;;
      --deploy-mode)
        mode=$2
        ENGINE_PARAMS="$ENGINE_PARAMS $1 $2"
        shift 2
        ;;
      *) # preserve positional arguments
        ENGINE_PARAMS="$ENGINE_PARAMS $1"
        shift
        ;;
    esac
  done

  if [ $mode = 'cluster' ];then
    HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS --file ${file##*/}"
    ENGINE_PARAMS="$ENGINE_PARAMS --files ${file}"
  else
    HUGEGRAPH_PARAMS="$HUGEGRAPH_PARAMS --file ${file}"
  fi
}
