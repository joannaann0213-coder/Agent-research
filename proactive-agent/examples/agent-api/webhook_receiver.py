"""A webhook receiver to try the Agent API's webhook with: it checks the signature of every delivery
and prints the update. Nothing but the standard library.

    python3 examples/agent-api/webhook_receiver.py --port 9999 --secret whsec_...

The agent service delivers only to public https addresses. To try the webhook on one machine, let it
deliver to private addresses over plain http (AGENT_WEBHOOKS_ALLOWPRIVATENETWORKS=true and
AGENT_WEBHOOKS_ALLOWPLAINHTTP=true, never in production), and set the webhook up with
http://host.docker.internal:9999/ when the service runs in docker-compose.yml.
"""

from __future__ import annotations

import argparse
import json
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from agent_api import verify_webhook  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--port", type=int, default=9999)
    parser.add_argument("--secret", required=True, help="the signing_secret shown when the webhook was set up")
    args = parser.parse_args()
    seen: set[str] = set()

    class Receiver(BaseHTTPRequestHandler):
        def do_POST(self) -> None:
            body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
            headers = {name.lower(): value for name, value in self.headers.items()}
            if not verify_webhook(args.secret, headers, body):
                self.send_response(401)
                self.end_headers()
                print("refused a delivery with a wrong signature or an old timestamp", flush=True)
                return
            # Answer quickly with a 2xx; a delivery that comes twice has the same id.
            self.send_response(204)
            self.end_headers()
            update = json.loads(body)
            if update["id"] in seen:
                return
            seen.add(update["id"])
            data = update["data"]
            print(f"{update['user_id']} #{update['seq']} {update['type']}: {data.get('text') or data.get('kind') or data}", flush=True)

        def log_message(self, *_: object) -> None:
            pass

    print(f"listening on port {args.port}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", args.port), Receiver).serve_forever()


if __name__ == "__main__":
    main()
