# Fixtures for `virtfusion.vmvm`

`server-overview.synthetic.json` is the early synthetic sample used to validate the first snapshot pipeline.

`server-list.capture.sanitized.json`, `server-state.capture.sanitized.json`, and `server-details.capture.sanitized.json` are sanitized captures based on real `VirtFusion` responses.
They are the current primary fixtures for the `vmvm` adapter work.

`capture-notes.md` records the request shape without storing live session secrets.

For every future fixture, record:

- where the payload came from
- what was redacted
- which capability it validates

Sensitive fields such as domains, UUIDs, IP addresses, MAC addresses, cookies, CSRF tokens, and embedded preview payloads must be sanitized before commit.
