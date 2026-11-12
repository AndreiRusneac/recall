# Architecture

One Spring Boot application, split into Gradle modules.

```
web ────────┐
            ├──> core <── verticals
worker ─────┘      ▲
                   │ implements MessageSender
               messaging

web, worker ──> persistence ──> PostgreSQL
app = main class, wires everything
```

## How a reminder is sent in the pilot

1. `DashboardController` loads the next 7 days of appointments from `EventRepository`.
2. `ReminderPlanner` (core) marks which ones are due under the vertical's default rule.
3. The owner presses "Send on WhatsApp". The form posts to `ReminderController`.
4. The controller checks consent, records `reminder_sent_at`, and redirects to a `wa.me` link
   with the customer's number and the message text.
5. WhatsApp opens on the clinic's device and a person presses send.

## How automatic sending will work

`ReminderJob` (worker) already runs the same planning on a schedule and hands each message to
`MessageSender`. Today the only implementation logs the message. Adding the WhatsApp API means
writing a second implementation in `messaging`; nothing else changes.

## Tables

| Table | Holds | Used by code yet |
|---|---|---|
| `business` | the paying account, its vertical, plan and timezone | Yes |
| `app_user` | logins for owners and staff | No |
| `customer` | name, phone, consent, last visit | Yes |
| `event` | an appointment (later also a missed call or a quote) | Yes |
| `rule` | per-business reminder rules | No |
| `message` | every message sent | No |
| `outcome` | a booking attributed to a message | No |
