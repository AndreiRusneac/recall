# 005: The owner records "confirmed" and "cancelled" by hand

Date: 2026-11-20

## Decision

Each appointment row on the dashboard has "Confirmat" and "Anulează" buttons. They set the
appointment's status to `CONFIRMED` or `CANCELLED`. The dashboard shows, for the last 30 days:
reminders sent, how many of those appointments were confirmed, and how many were cancelled.
No new table or column is needed (`event.status` already exists).

## Why

- During the pilot we cannot read WhatsApp replies (decision 002), so the app cannot know
  who confirmed. The clinic reads the reply and taps once.
- "8 reminders sent, 6 confirmed, 1 cancelled so the slot could be refilled" is the number a
  prospective clinic needs to see. It is the pilot's whole claim.

## Consequences

- The numbers depend on the clinic tapping. Treat them as a lower bound for confirmations and
  say so when showing them to a prospect.
- "Reminders sent" counts appointments whose reminder was handed to WhatsApp (`OPENED`,
  decision 003), not messages confirmed delivered.
- A cancelled appointment disappears from the dashboard and can no longer be sent a reminder
  (the server refuses it).
- The `outcome` table stays unused for now. When the WhatsApp API reads replies, it can record
  confirmations there automatically.
