# Captured Request Notes

## Server list endpoint

- Request URL: `https://panel.example.invalid/servers/_list?limit=8`
- Method: `GET`
- Expected response: `application/json`
- Triggered from: dashboard or server list surface

## Observed request characteristics

- same-origin XHR request
- requires authenticated session cookie
- requires `XSRF-TOKEN` cookie and matching `x-xsrf-token` header
- includes `x-requested-with: XMLHttpRequest`

## Sanitization rules

- never commit live cookie values
- never commit live `x-xsrf-token`
- replace real hostnames, UUIDs, IP addresses, and MAC addresses
- replace embedded preview images or other large encoded blobs when they are not needed for parsing
