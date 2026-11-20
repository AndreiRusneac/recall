-- Store every customer phone number in one form: digits only, with the country code
-- ("0700 000 001" becomes "40700000001"). This is the same rule as PhoneNumbers.toInternational
-- in core, with Romania (40) as the default country.
--
-- Why: the CSV import writes numbers in this form. Without this, a customer stored as
-- "0700 000 001" would not be recognised on import and would get a second record, which
-- could silently reset an opt-out to GRANTED.
--
-- If two rows of the same business collapse to the same number, the unique constraint
-- (business_id, phone) makes this migration fail instead of merging customers silently.
-- Fix the duplicates by hand first in that case.

update customer
set phone = case
    when phone like '+%'
        then regexp_replace(phone, '\D', '', 'g')
    when regexp_replace(phone, '\D', '', 'g') like '00%'
        then substr(regexp_replace(phone, '\D', '', 'g'), 3)
    when regexp_replace(phone, '\D', '', 'g') like '0%'
        then '40' || substr(regexp_replace(phone, '\D', '', 'g'), 2)
    else regexp_replace(phone, '\D', '', 'g')
end;
