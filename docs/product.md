# Product

## Who it is for

Small local businesses that live on appointments, starting with dental and medical clinics in Cluj-Napoca.

## The problem

Patients forget appointments. Each no-show is lost revenue, and reception staff have no time to phone everyone.

## What the product does

It shows the business which reminders are due and sends them on WhatsApp, then shows how many
appointments were confirmed or recovered.

## Plans

| | Reminders | Reminders + Booking |
|---|---|---|
| Reminders and confirmations | Yes | Yes |
| Appointments come from | CSV import or manual entry | Online booking page, plus manual entry |
| Public booking link | No | Yes |

One app, two plans. `business.plan` and `FeatureAccess` in `core` decide what a business can use.
Prices are not set; test them in interviews.

## Pilot

- Tap-to-send from the clinic's own WhatsApp, so there is no per-message cost and no Meta approval.
- Two or three small clinics, three months, free or at message cost.
- Automatic sending through the WhatsApp API is built once a clinic says it would pay to stop tapping.

## Out of scope for now

- A patient-facing app or login
- Payments inside the product
- SMS
- Re-engagement nudges (billed at WhatsApp's marketing rate)
