# 002: The pilot sends by tap-to-send links, not the WhatsApp API

Date: 2026-11-12

## Decision

During the pilot, reminders are sent from the clinic's own WhatsApp through pre-filled `wa.me`
links. The WhatsApp Business API is not used yet.

## Why

- No per-message cost and no business verification or template approval, so a pilot can start at once.
- It tests the real question first: do reminders reduce no-shows, and would the clinic pay?

## Consequences

- Someone at the clinic taps once per reminder.
- We cannot see replies or delivery status, so confirmations are not tracked automatically.
- Never automate the regular WhatsApp app with unofficial libraries. It breaks WhatsApp's terms
  and risks the clinic's number being banned.
