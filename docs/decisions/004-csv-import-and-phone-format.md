# 004: CSV import, consent in the file, and one phone format

Date: 2026-11-20

## Decision

1. Clinics bring their appointments in with a CSV file (columns: name, phone, starts_at, consent).
   Parsing and validation are pure logic in `core` (`AppointmentCsv`). Saving is in
   `persistence` (`AppointmentImporter`). The upload page is in `web`.
2. **Consent comes from the file, and a blank means `UNKNOWN`.** A customer with `UNKNOWN`
   consent gets no send button until someone confirms they agreed.
3. **A file can never undo an opt-out.** `ConsentStatus.afterImport` keeps `OPTED_OUT` whatever
   the file says. A file can promote `UNKNOWN` to `GRANTED`, and "no" always wins.
4. **Every stored phone number is digits only, with the country code** (`40700000001`).
   Migration `V2` converts existing rows. The import and every future write must store this form.
5. Importing the same file twice changes nothing: customers match on phone number, and an
   appointment that already exists (same customer, kind and start time) is skipped.

## Why

- Without import a clinic cannot try the product with its own data.
- The opt-out rule protects patients and the clinic's WhatsApp number. Re-importing an old
  export must not make someone who said "stop" messageable again.
- While testing, the old phone format (`0700 000 001`) did not match the importer's format, so an
  opted-out customer was duplicated as `GRANTED`. One stored format closes that gap for good.

## Consequences

- Anything that writes `customer.phone` must normalize with `PhoneNumbers.toInternational` first.
- `V2` fails, instead of merging people silently, if two customers of one business collapse to the
  same number. Resolve those by hand before upgrading such a database.
- CSV problems are returned as codes (`AppointmentCsv.Problem`); the wording shown to the owner
  lives in `web`, so `core` has no user-facing text.
- Files saved by Excel in Romania as Windows-1250 are read correctly (UTF-8 is tried first).
