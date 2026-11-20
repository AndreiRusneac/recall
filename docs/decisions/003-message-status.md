# 003: Tap-to-send is recorded as OPENED, not SENT

Date: 2026-11-13

## Decision

Every message is recorded in the `message` table with a channel and a status that claims only
what Recall can observe:

| How it left Recall | `channel` | `status` |
|---|---|---|
| Owner tapped "Send on WhatsApp" (pilot) | `WHATSAPP_LINK` | `OPENED` |
| `ReminderJob` handed it to a `MessageSender` | `WHATSAPP_API` | `SENT` |

`sent_at` is when the message was handed on: to WhatsApp on the owner's device, or to the sender.

## Why

- With tap-to-send, Recall only knows WhatsApp was opened with the text. Whether the person
  pressed send happens on their phone and is invisible to us (decision 002).
- Calling that `SENT` would inflate every later number ("X reminders sent, Y appointments
  recovered") by an unknown amount. The pilot exists to measure exactly that.
- It keeps one tap per reminder. Asking the owner to confirm "I sent it" was rejected for now:
  a second tap per reminder would mostly be skipped.

## Consequences

- Reports must treat `OPENED` as "handed to WhatsApp", not as delivered.
- The real WhatsApp API can add statuses such as `DELIVERED`, `READ` or `FAILED` without changing
  what existing rows mean.
- An "I sent it" confirmation that turns `OPENED` into `SENT` can be added later if a clinic wants it.