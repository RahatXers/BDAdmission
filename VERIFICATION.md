# Admission data audit — 14 November 2026 (Asia/Dhaka)

The canonical dataset is `data/admissions.json`; each institution records exact source URLs and its verification level. `data/admissions.js` is generated for dependency-free browser use. No future automatic updating is implied.

## Confirmed corrections and additions

- RU: applications 15–30 November; B/C/A examinations 12/13/20 February. Two separate shifts; fees include service charge.
- KU: A/B moved to 4 February, C/D to 5 February. No fabricated clock times or application deadlines.
- JNU: application opening changed to 25 November. New official notice overrides the earlier opening. E fee has a numeric/word inconsistency in extracted PDF text; the news-confirmed amount is explicitly qualified.
- CU: applications changed to 10 December–10 January; outstanding payments through 12 January. Latest Prothom Alo report used; older conflicting aggregator dates rejected.
- KUET: 09:30–12:30 exam, applications 10 November–10 December. Official prospectus says deadline 17:00; conflicting news reports say 21:00. Official prospectus wins and conflict is disclosed.
- BUP: both FBS dates are represented; no assumption about which applicant attends which date.
- Added CoU, AAUB, SUST, RUET, JU and HSTU. SUST/HSTU/JU ranges are never assigned invented unit-specific dates.
- AAUB: undergraduate date moved from 5 December to 30 January. Postgraduate circular excluded.
- CUET: old unsupported 25 January/10:00 countdown removed. Current official admission page did not establish a schedule; marked pending.
- ACAS, JU and RUET: tentative dates are visible but do not trigger exact countdowns.

## Evidence boundaries

Official-source status means a current primary source establishes at least the schedule; it does not assert every field is published. Unknown fees, methods, times and application windows remain explicitly unknown. DU fees are cross-checked through Prothom Alo. MIST, CU, BUET, BUTEX and SUST have clearly labelled news-based information, not a false claim of direct primary-source verification.

Existing logo assets and available campus backgrounds were retained. New institutions use text monograms as fallbacks. Wikipedia identity/color references are retained in `brandSource` (AAUB midnight blue/gold, RUET navy/gold, JU peacock blue, HSTU blue/green). CoU teal and SUST gold/red are design accents, not certified brand hex specifications. No newly invented crest is presented as an official logo.

## Automated checks

`node tests/admissions.test.cjs` covers explicit +06:00 timestamps, opening and closing boundaries, date-only precision, unknown schedules, revised dates, all source links being HTTPS, dataset/bundle parity, HTML IDs and static data routing. Run in UTC and America/New_York to check host-timezone independence.

Android Java syntax was parsed with the installed JDK compiler, and XML/source parity/lambda scans were performed. Android SDK/AIDE/emulator not available: APK compilation and device behavior are unverified. Local Chromium could not launch in this execution environment; cloud browser could not reach local preview. Responsive CSS exists, but visual verification must be completed against the deployed page or a local browser.
