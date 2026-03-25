# Real Request Path

With `local.auth.properties` in place, the current `VirtFusion` request path is:

1. `GET /servers/_list?limit=8`
2. `GET /server/{id}/resource/server.json`
3. `GET /server/{id}/resource/state.json`

The client currently assumes:

- an authenticated browser-derived session
- `Cookie` header with `XSRF-TOKEN` and `virtfusion_session`
- `X-XSRF-TOKEN` header mirroring the active XSRF token
- same-origin style `Referer`
- `X-Requested-With: XMLHttpRequest`

This is implemented for local development in `adapter:virtfusion`.
