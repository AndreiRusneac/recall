-- Demo data, loaded only with the "dev" profile. Names and numbers are invented.
-- Appointment times are relative to the moment this script first runs.

insert into business (id, name, vertical, plan)
values ('00000000-0000-0000-0000-000000000001', 'Demo Dental Clinic', 'CLINIC', 'REMINDERS')
on conflict (id) do nothing;

insert into customer (id, business_id, name, phone, consent)
values ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000001', 'Maria Ionescu', '0700 000 001', 'GRANTED'),
       ('00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000001', 'Mihai Popa', '0700 000 002', 'GRANTED'),
       ('00000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000001', 'Elena Marin', '0700 000 003', 'OPTED_OUT')
on conflict (id) do nothing;

insert into event (id, business_id, customer_id, kind, starts_at)
values ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101', 'APPOINTMENT', now() + interval '20 hours'),
       ('00000000-0000-0000-0000-000000001002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000102', 'APPOINTMENT', now() + interval '3 days'),
       ('00000000-0000-0000-0000-000000001003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000103', 'APPOINTMENT', now() + interval '22 hours')
on conflict (id) do nothing;
