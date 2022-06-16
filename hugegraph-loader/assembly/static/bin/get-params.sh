#!/bin/bash
function get_params() {
  echo "params: $*"
  engine_params=""
  hugegraph_params=""
  while (("$#")); do
    case "$1" in
      -–file | --graph | --schema | --host | --port | --username | --token | --protocol | \
      --trust-store-file | --trust-store-password | --clear-all-data | --clear-timeout | \
      --incremental-mode | --failure-mode | --batch-insert-threads | --single-insert-threads | \
      --max-conn | --max-conn-per-route | --batch-size | --max-parse-errors | --max-insert-errors | \
      --timeout | --shutdown-timeout | --retry-times | --retry-interval | --check-vertex | \
      --print-progress | --dry-run | --help)
      hugegraph_params="$hugegraph_params $1 $2"
      shift 2
      ;;

    *) # preserve positional arguments
      engine_params="$engine_params $1"
      shift
      ;;
    esac
  done
}
