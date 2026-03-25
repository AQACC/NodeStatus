# Local Session Auth

For local development, keep `VirtFusion` session credentials in `local.auth.properties` at the repository root.

## Why this exists

- lets local development use real session-backed requests
- keeps cookies and XSRF headers out of Git history
- avoids pasting secrets into chat, fixtures, or docs

## Setup

1. Copy [local.auth.properties.example](../local.auth.properties.example) to `local.auth.properties`.
2. Fill in:
   - `virtfusion.base_url`
   - `virtfusion.cookie_header`
   - `virtfusion.x_xsrf_token`
3. Only if local Java TLS validation blocks your request path, set:
   - `virtfusion.allow_insecure_tls=true`
4. Keep the file local only.

## Current scope

This is a development-only input path.
The product path should still become:

`WebView login -> session extraction -> encrypted local storage`

## Safety note

If a cookie or XSRF token is ever pasted into chat or committed by mistake, invalidate that session immediately.

`virtfusion.allow_insecure_tls=true` should only be used as a local debugging escape hatch.
