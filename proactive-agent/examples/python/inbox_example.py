#!/usr/bin/env python3
"""A minimal Event Inbox client in Python, using only the standard library.

It shows both sides of an integration:

  connector  write_event() appends a change from a third-party system to a user's inbox
  agent      run_once() claims a run, keeps its lease, does the work and ends the run

Try it against the server from the README quick start:

  python3 inbox_example.py write    # as the connector: write one event
  python3 inbox_example.py work     # as the agent: claim one run and handle it

INBOX_URL selects the server (default http://127.0.0.1:8080). The client ids and secrets in main()
are the quick-start ones; take real secrets from a secret store.
"""
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE_URL = os.environ.get("INBOX_URL", "http://127.0.0.1:8080")


class InboxError(Exception):
    """A non-2xx answer. The problem document's "code" is the stable, machine-readable part."""

    def __init__(self, status, problem):
        problem = problem or {}
        super().__init__(f"inbox answered {status}: {problem.get('code')} {problem.get('detail')}")
        self.status = status
        self.problem = problem


class LeaseLost(Exception):
    """Another worker owns the run now: stop working on it and change nothing."""


def call(client_id, secret, method, path, body=None):
    """Sends one signed request; returns (status, headers, parsed JSON body or None).

    The signature covers a timestamp, the method, the path with its query and the body, exactly as sent.
    """
    data = b"" if body is None else json.dumps(body, separators=(",", ":")).encode("utf-8")
    timestamp = str(int(time.time()))
    string_to_sign = "\n".join(["v1", timestamp, method, path, hashlib.sha256(data).hexdigest()])
    signature = hmac.new(secret.encode("utf-8"), string_to_sign.encode("utf-8"), hashlib.sha256).hexdigest()
    request = urllib.request.Request(BASE_URL + path, data=data if body is not None else None, method=method)
    request.add_header("X-Client-Id", client_id)
    request.add_header("X-Timestamp", timestamp)
    request.add_header("X-Signature", "hmac-sha256=" + signature)
    if body is not None:
        request.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read()
            return response.status, response.headers, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        raw = error.read()
        return error.code, error.headers, json.loads(raw) if raw else None


def write_event(client_id, secret, inbox_id, event, attempts=5):
    """Appends one event to an inbox and returns the answer once the Inbox has accepted it.

    Retrying is safe: the same event with the same source_event_id is stored once. A 4xx answer means
    the mapping or the credentials are wrong, so it is raised, not retried.
    """
    path = "/v1/inbox/" + urllib.parse.quote(inbox_id, safe="") + "/events"
    status, body = None, None
    for attempt in range(1, attempts + 1):
        try:
            status, _, body = call(client_id, secret, "POST", path, event)
        except (urllib.error.URLError, TimeoutError, ConnectionError) as error:
            status, body = None, {"detail": str(error)}
        if status == 202:
            return body  # {"event_id": ..., "deduped": false or true, "status": ...}
        if status is not None and status < 500:
            raise InboxError(status, body)
        if attempt < attempts:
            time.sleep(0.2 * 2 ** attempt)
    raise InboxError(status or 0, body)


def run_once(client_id, secret, worker_id, handle, wait_ms=20000):
    """Claims one run, hands it to handle(run, heartbeat) and ends it. Returns False when there was none.

    handle() is the agent's turn: model calls, MCP tool calls, messages to the outbox. It calls
    heartbeat() between steps, at least every 20 seconds. Returning normally acks the run. Raising
    LeaseLost abandons it. Any other exception nacks it, and the same run is tried again later.
    """
    status, _, run = call(client_id, secret, "POST", "/v1/runs/claim", {"worker_id": worker_id, "wait_ms": wait_ms})
    if status == 204:
        return False
    if status != 200:
        raise InboxError(status, run)
    run_path = "/v1/runs/" + run["run_id"]
    epoch = run["epoch"]

    def heartbeat():
        beat_status, _, beat = call(client_id, secret, "POST", run_path + "/heartbeat", {"epoch": epoch})
        if beat_status == 409:
            raise LeaseLost(run["run_id"])
        if beat_status != 200:
            raise InboxError(beat_status, beat)
        return beat  # lease_expires_at, extended, steer_pending, interrupt_requested

    try:
        handle(run, heartbeat)
    except LeaseLost:
        return True
    except Exception as error:  # a transient failure, for example an MCP server that timed out
        call(client_id, secret, "POST", run_path + "/nack", {"epoch": epoch, "reason": str(error)[:500]})
        return True
    last_run_seq = max(event["run_seq"] for event in run["events"])
    status, _, outcome = call(client_id, secret, "POST", run_path + "/ack",
                              {"epoch": epoch, "outcome": "done", "last_run_seq": last_run_seq})
    if status not in (200, 409):  # 409: the lease was lost meanwhile and another worker has the run
        raise InboxError(status, outcome)
    return True


def main(argv):
    if argv == ["write"]:
        answer = write_event("order-bot", "order-bot-secret", "u42", {
            "source": "connector",
            "source_event_id": "jira:PROJ-123:2026-11-21T10:00:00Z",
            "type": "jira.issue.updated.v1",
            "merge_key": "issue:PROJ-123",
            "lane": "background",
            "occurred_at": "2026-11-21T10:00:00Z",
            "payload": {"issue": "PROJ-123", "changed": ["status"]},
        })
        print(json.dumps(answer))
    elif argv == ["work"]:
        def handle(run, heartbeat):
            for event in run["events"]:
                heartbeat()
                print(f"run {run['run_id']} ({run['kind']}): {event['type']} {event['source_event_id']}")

        handled = run_once("runtime-1", "runtime-secret", "python-worker-1", handle, wait_ms=5000)
        print("handled one run" if handled else "nothing to claim")
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
