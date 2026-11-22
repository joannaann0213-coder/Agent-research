"""A small client of the Agent API, with nothing but the standard library.

It shows the calls an application makes: set up an agent, send a user's message, follow the
user's updates as a live stream, tell the agent about an event, decide an action, and check a
webhook delivery. Run it against an agent service (demo mode is enough):

    python3 examples/agent-api/agent_api.py --api http://localhost:8090 --admin <administration key>

See docs/en/agent-api.md for the guide and GET /openapi.yaml for the specification.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import threading
import time
import urllib.error
import urllib.request
from typing import Any, Iterator


class AgentApiError(Exception):
    """An answer with a problem document: `status`, a stable `code` and a `detail` for people."""

    def __init__(self, status: int, problem: dict[str, Any]):
        super().__init__(f"{status} {problem.get('code')}: {problem.get('detail')}")
        self.status = status
        self.code = problem.get("code")
        self.problem = problem


class AgentApi:
    """The Agent API as one caller: an administrator, an application's back end, or a user's client."""

    def __init__(self, base_url: str, credential: str | None = None, timeout: float = 30):
        self.base_url = base_url.rstrip("/")
        self.credential = credential
        self.timeout = timeout

    def as_(self, credential: str) -> "AgentApi":
        return AgentApi(self.base_url, credential, self.timeout)

    def request(self, method: str, path: str, body: Any = None) -> Any:
        data = None if body is None else json.dumps(body).encode()
        request = urllib.request.Request(self.base_url + path, data=data, method=method)
        request.add_header("Accept", "application/json")
        if data is not None:
            request.add_header("Content-Type", "application/json")
        if self.credential:
            request.add_header("Authorization", f"Bearer {self.credential}")
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                text = response.read()
                return json.loads(text) if text else None
        except urllib.error.HTTPError as error:
            text = error.read()
            raise AgentApiError(error.code, json.loads(text) if text else {}) from None

    # ---- the operator ------------------------------------------------------------------------

    def create_app(self, app_id: str, name: str) -> dict:
        return self.request("POST", "/v1/admin/apps", {"app_id": app_id, "name": name})

    def create_key(self, app_id: str, label: str) -> str:
        return self.request("POST", f"/v1/admin/apps/{app_id}/keys", {"label": label})["api_key"]

    # ---- the application's back end ---------------------------------------------------------

    def put_agent(self, key: str, config: dict) -> dict:
        return self.request("PUT", f"/v1/agents/{key}", config)

    def user_token(self, user_id: str) -> str:
        return self.request("POST", f"/v1/users/{user_id}/tokens")["token"]

    def post_event(self, user_id: str, event: dict) -> dict:
        return self.request("POST", f"/v1/users/{user_id}/events", event)

    def put_webhook(self, url: str, events: list[str] | None = None) -> dict:
        return self.request("PUT", "/v1/webhook", {"url": url, "events": events} if events else {"url": url})

    # ---- an application's back end or a user's client ----------------------------------------

    def post_message(self, user_id: str, text: str, conversation_id: str | None = None, message_id: str | None = None) -> dict:
        body = {"text": text}
        if conversation_id:
            body["conversation_id"] = conversation_id
        if message_id:
            body["message_id"] = message_id
        return self.request("POST", f"/v1/users/{user_id}/messages", body)

    def messages(self, user_id: str, after: int = 0) -> dict:
        return self.request("GET", f"/v1/users/{user_id}/messages?after={after}")

    def updates(self, user_id: str, after: int = 0) -> dict:
        return self.request("GET", f"/v1/users/{user_id}/updates?after={after}")

    def actions(self, user_id: str) -> list[dict]:
        return self.request("GET", f"/v1/users/{user_id}/actions")["actions"]

    def resolve(self, user_id: str, action_id: str, decision: dict) -> dict:
        return self.request("POST", f"/v1/users/{user_id}/actions/{action_id}/resolve", decision)

    def stop(self, user_id: str, conversation_id: str = "default") -> dict:
        return self.request("POST", f"/v1/users/{user_id}/conversations/{conversation_id}/stop")

    def stream(self, user_id: str, after: int | None = None) -> Iterator[dict]:
        """The user's updates as they happen, from server-sent events. Ends when the server ends the connection."""
        path = f"/v1/users/{user_id}/stream" + (f"?after={after}" if after is not None else "")
        request = urllib.request.Request(self.base_url + path)
        request.add_header("Accept", "text/event-stream")
        request.add_header("Authorization", f"Bearer {self.credential}")
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            data: list[str] = []
            for raw in response:
                line = raw.decode().rstrip("\r\n")
                if line.startswith("data:"):
                    data.append(line[5:].strip())
                elif not line and data:
                    yield json.loads("\n".join(data))
                    data = []


def verify_webhook(secret: str, headers: dict[str, str], body: bytes, tolerance: int = 300) -> bool:
    """Whether a webhook delivery comes from the service and is recent. Header names in lower case."""
    key = base64.b64decode(secret.removeprefix("whsec_"))
    signed = f"{headers['webhook-id']}.{headers['webhook-timestamp']}.".encode() + body
    expected = "v1," + base64.b64encode(hmac.new(key, signed, hashlib.sha256).digest()).decode()
    fresh = abs(time.time() - int(headers["webhook-timestamp"])) <= tolerance
    return fresh and any(hmac.compare_digest(expected, s) for s in headers["webhook-signature"].split(" "))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--api", default="http://localhost:8090")
    parser.add_argument("--admin", default="change-me-admin-key-0123456789abcdef")
    parser.add_argument("--app", default="acme-py")
    parser.add_argument("--user", default="u42")
    args = parser.parse_args()

    operator = AgentApi(args.api, args.admin)
    try:
        operator.create_app(args.app, "Acme (Python example)")
    except AgentApiError as error:
        if error.code != "conflict":
            raise
    app = operator.as_(operator.create_key(args.app, "python example"))
    app.put_agent("default", {"name": "Assistant", "instructions": "You help Acme customers with their orders."})
    user = app.as_(app.user_token(args.user))

    # Follow the user's updates live, from now on, in the background.
    seen: list[dict] = []
    start = app.updates(args.user)["next_after"]

    def follow() -> None:
        for update in user.stream(args.user, after=start):
            seen.append(update)
            print(f"  update {update['seq']}: {update['type']}", update["data"].get("text", update["data"].get("status", "")))

    threading.Thread(target=follow, daemon=True).start()

    print("The user writes:")
    user.post_message(args.user, "Where is my order 1234?")
    print("The application's systems report a delay:")
    app.post_event(args.user, {"type": "order.delayed", "subject": "order-1234",
                               "data": {"order_id": "1234", "new_delivery_date": "2026-11-27"}})

    deadline = time.time() + 30
    while time.time() < deadline and not any(u["data"].get("kind") == "notification" for u in seen):
        time.sleep(0.5)
    print("Waiting for people:", [a["kind"] for a in user.actions(args.user)] or "nothing")


if __name__ == "__main__":
    main()
