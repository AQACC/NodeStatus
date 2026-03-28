# Security Policy

## Supported state

This project is pre-1.0 and under active design. Security-sensitive areas already in scope include:

- API keys
- Session cookies
- WebView-authenticated sessions
- Rule-pack supplied HTTP definitions
- Local snapshot caching for widgets and notifications

## Reporting

If you discover a security issue, do not open a public issue with exploit details.
Send a private report through GitHub Security Advisories:

https://github.com/AQACC/NodeStatus/security/advisories/new

If the final repository name differs from `NodeStatus`, update this advisory URL
and `.github/ISSUE_TEMPLATE/config.yml` together.

If the Security tab is not enabled yet, contact the maintainer via their GitHub
profile and request a private channel. Include:

- affected area
- reproduction steps
- impact assessment
- whether credentials or customer data are required

## Secret handling rules

- Never commit live cookies, API keys, CSRF tokens, or full request dumps.
- All fixtures must be sanitized before entering the repository.
- Local credential storage must eventually use platform-backed encryption.

## Engine boundaries

Rule packs are intended to stay declarative.
They may describe HTTP requests, extraction rules, and field mapping.
They must not be allowed to execute arbitrary scripts, shell commands, or unrestricted file access.
