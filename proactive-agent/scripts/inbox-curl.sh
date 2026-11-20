#!/usr/bin/env bash
# Sends one signed request to the Event Inbox with curl.
#
#   scripts/inbox-curl.sh <client-id> <secret> <METHOD> <path-with-query> [json-body]
#
# Example:
#   scripts/inbox-curl.sh order-bot "$SECRET" POST /v1/inbox/u42/events \
#     '{"source":"biz_webhook","source_event_id":"order-1234-v7","type":"order.paid.v1","payload":{"order_id":"1234"}}'
#
# The signature is HMAC-SHA256 over:  v1 \n timestamp \n METHOD \n path?query \n hex(sha256(body))
# INBOX_URL selects the server (default http://127.0.0.1:8080).
set -euo pipefail

if [ "$#" -lt 4 ]; then
  sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
fi

client_id="$1"; secret="$2"; method="$3"; path="$4"; body="${5:-}"
base="${INBOX_URL:-http://127.0.0.1:8080}"

timestamp="$(date +%s)"
body_hash="$(printf '%s' "$body" | sha256sum | cut -d' ' -f1)"
signature="$(printf 'v1\n%s\n%s\n%s\n%s' "$timestamp" "$method" "$path" "$body_hash" \
  | openssl dgst -sha256 -hmac "$secret" -hex | sed 's/^.*= *//')"

args=(-sS -X "$method" "$base$path"
  -H "X-Client-Id: $client_id"
  -H "X-Timestamp: $timestamp"
  -H "X-Signature: hmac-sha256=$signature"
  -w '\n[HTTP %{http_code}]\n')
if [ -n "$body" ]; then
  args+=(-H 'Content-Type: application/json' --data-binary "$body")
fi
exec curl "${args[@]}"
