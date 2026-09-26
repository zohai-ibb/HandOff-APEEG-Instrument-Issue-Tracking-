# Handoff: APEEG Instrument Issue & Tracking (CSIR–CBRI, Roorkee)

## Overview

A mobile application for the Architectural Planning & Energy Efficiency Group (APEEG) at
CSIR–Central Building Research Institute, Roorkee, under Dr. Kishor S. Kulkarni.

It replaces the group's physical instrument logbook. It records which instrument is issued,
which is in use, which is under maintenance or awaiting calibration, and who holds each item.
On issue it mails the project staff member, their scientist and the Group Head. Once the return
date passes it mails a reminder every day until the instrument is received back. Instruments and
people can be added from the app.

## About the design files

`APEEG Instrument Register.dc.html` in this bundle is a **design reference created in HTML** — a
working prototype showing the intended look, copy and behaviour. It is not production code to
copy. The task is to **recreate these designs in the target codebase**, using its established
patterns and libraries. If no codebase exists yet, choose a stack (recommendation in
*Suggested implementation* below) and implement the designs there.

The prototype's data is in-memory sample data. It contains no backend, no real mail sending and
no authentication. Those are implementation work described here.

To view the prototype: open the `.html` file in a browser. `android-frame.jsx` supplies the phone
bezel and is a presentation device only — do not port it.

## Fidelity

**High fidelity.** Colours, typography, spacing, copy and interactions are final and should be
recreated closely. Exact values are in *Design tokens* and per-screen notes. The phone bezel,
the sample data and the explanatory text column beside the phone are prototype scaffolding, not
part of the app.

## Product structure

### Roles and permissions

| Role | Capability |
|---|---|
| Custodian / register keeper | Full write. Issues and receives instruments, edits the master register, sets maintenance status, adds instruments and people. |
| Scientist | Reads instruments held by their own project staff. Approves requests when approval is enabled. Receives issue, reminder and return mail. |
| Project staff | Reads what is in their own name and the return dates. Raises a request, acknowledges receipt. No editing. |
| Group Head (Dr. Kulkarni) | Read-only across the whole group, plus overdue list and exports. Copied on every issue and return mail. |

Open decisions, both of which change scope: whether project staff get their own login, and whether
issue requires the scientist's approval first. The prototype carries `requireApproval` as a switch
(see *Configuration*) so both paths are visible.

### Data model

Five entities. Every issue is a new row; nothing is overwritten, so the register stays auditable.

**instrument**
```
id                 pk
asset_id           text, unique          e.g. "CBRI/APEEG/0121"
name               text                  instrument name and model
make               text
serial_no          text
quantity           int, default 1        for sets, e.g. 12 loggers
location            text                 lab / cabinet / with vendor
status             enum(available, issued, maintenance, calibration_due, damaged, condemned, lost)
calibration_valid_to  date, nullable
purchase_date      date, nullable
purchase_cost      decimal, nullable
accessories        text, nullable        checklist of what goes with it
photo              file, nullable
manual             file, nullable
created_at, updated_at
```

**person**
```
id                 pk
name               text
role               enum(scientist, project_staff, custodian, group_head)
email              text, unique          official CBRI id — mail routing depends on this
mobile             text, nullable
reports_to         fk person, nullable   project staff -> their scientist
project            text, nullable
is_active          boolean, default true
```

**issue_record**
```
id                 pk
instrument         fk instrument
issued_to          fk person             project staff
scientist          fk person             derived from issued_to.reports_to at issue time, stored
issued_by          fk person             custodian
issue_date         date
due_date           date
purpose            text                  project / study the instrument is for
condition_out      text
condition_in       text, nullable
actual_return_date date, nullable
state              enum(pending_approval, open, returned, cancelled)
```

`scientist` is stored on the record rather than looked up later, so history stays correct when
staff change reporting lines.

**mail_log**
```
id                 pk
issue_record       fk issue_record, nullable
instrument         fk instrument, nullable
type               enum(issue, approval_request, advance_notice, overdue, return, calibration, manual_reminder)
recipients         text                  comma-separated addresses actually used
subject            text
body               text
sent_at            datetime
delivery_status    enum(sent, failed)
error              text, nullable
```

**maintenance_record**
```
id                 pk
instrument         fk instrument
type               enum(repair, calibration)
sent_to            text                  vendor
out_date           date
expected_back      date, nullable
returned_on        date, nullable
cost               decimal, nullable
certificate        file, nullable
```

### Automatic mail

All mail is sent server-side, never from the phone. Recipients are always the holder, their
scientist, and the Group Head copied.

| Trigger | Recipients | Content |
|---|---|---|
| On issue | staff, scientist, Group Head | instrument, asset ID, issue date, return date, purpose, condition at issue |
| `due_date − reminderLeadDays` | staff, scientist | advance notice of the coming return date |
| `due_date` passed, daily | staff, scientist, Group Head cc | days overdue, chase to return |
| On return | staff, scientist, Group Head | acknowledgement closing the record, condition at return |
| `calibration_valid_to − 30 days` | custodian, Group Head | calibration validity ending |
| Manual "Send reminder now" | staff, scientist, Group Head cc | immediate chase, logged as `manual_reminder` |

**Reminder job.** One scheduled task, daily at 09:00 IST (cron, Celery beat, or equivalent):

```
for each issue_record where state = 'open':
    d = due_date - today
    if d == reminderLeadDays:  send advance_notice
    if d <  0:                 send overdue     # every day, until returned
for each instrument where calibration_valid_to - today == 30:
    send calibration
```

Write a `mail_log` row for every attempt including failures — a silent SMTP failure must be
visible in the app, since the record is what gets audited. Stop chasing when
`actual_return_date` is set. Make the job idempotent: guard with a check for an existing
`mail_log` row of the same type for the same record on the same date, so a re-run does not
double-mail.

## Screens

Nine screens. Bottom navigation has five items: **Home, Items, Issue, Due, People**. Reports, Add
instrument and Mail log are reached from within those screens (the row of pills above the phone in
the prototype is a prototype-only jump bar — do not build it).

Every screen sits under a fixed app bar: `#1b4d8f` background, white text, 14px 16px 13px padding,
holding a back button (32×32, `rgba(255,255,255,.14)`, radius 8, hidden on Home), the screen title
(16px/600, letter-spacing −.01em), the subtitle "APEEG · CBRI ROORKEE" (IBM Plex Mono 10px,
letter-spacing .08em, uppercase, `rgba(255,255,255,.72)`), and a 30px circular user avatar
(`rgba(255,255,255,.18)`, 11px/600 initials). App bar is sticky at top. Screen body: 16px side
padding, 16px top, 18px bottom, 16px gap between blocks, background `#f7f6f3`.

### 1. Home — "Instrument Register"

Purpose: the custodian's daily position at a glance, plus the three actions they take most.

Layout, top to bottom:
- **Overdue alert**, shown only when overdue count > 0. Card, `#fbe7e3` on `1px solid #e8b4ab`,
  radius 12, padding 13px 14px, 9px gap. Heading 13.5px/600 `#8f2318`, reading
  "N instruments are past their return date" (singular form when N = 1). Body 12.5px/1.5
  `#6d3a33`: "Reminder mails go out daily to the holder and their scientist until the instrument
  is received back." Button "Review overdue", `#8f2318` fill, white 12.5px/500, radius 8,
  padding 7px 13px, left-aligned → Due screen.
- **Four stat tiles**, 2×2 grid, 9px gap. Each white on `1px solid rgba(0,0,0,.09)`, radius 12,
  padding 13px 14px. Number 26px/600, letter-spacing −.02em; label 11.5px `#5d5b56`, 2px above.
  Available `#12695a` · Issued / in use `#8a5a12` · Overdue `#8f2318` ·
  Maintenance / calibration `#5c4a86`.
- **Action row**, three equal buttons, 9px gap, padding 12px 8px, radius 11, 13px/500. "Issue" is
  `#1b4d8f` filled white; "Receive" and "Add item" are white on `1px solid rgba(0,0,0,.16)`.
- **"Currently out"** section. Header row: label 12px/600, letter-spacing .06em, uppercase,
  `#5d5b56`, with a text button "All instruments" 12px `#1b4d8f` on the right. Then the
  instrument row component (below), one per open issue.

### Instrument row (used on Home, Instruments, Return)

Full-width button, left-aligned, white on `1px solid rgba(0,0,0,.09)`, radius 12,
padding 12px 13px, 5px internal gap, 8px between rows. Hover: border `#1b4d8f`.
- Line 1: name 13.5px/500, line-height 1.3, flexing; status pill on the right — 10.5px/500,
  padding 3px 7px, radius 6, colours from the status map in *Design tokens*.
- Line 2: asset ID in IBM Plex Mono 10.5px `#7a7872` (on the Instruments screen, followed by
  " · " and the make).
- Line 3: context line 12px/1.45 `#5d5b56`, which varies by status:
  - overdue → `{holder} · due {dd Mmm}, {n} days late`
  - issued → `{holder} · return by {dd Mmm}`
  - maintenance → `Out for repair / calibration · {location}`
  - calibration due → the calibration validity text
  - available → the location

Tapping the row opens Instrument detail.

### 2. Instruments — list and filters

- Search input, full width, padding 11px 13px, radius 10, `1px solid rgba(0,0,0,.16)`, white,
  13px, focus border `#1b4d8f`. Placeholder "Search name, asset ID or holder". Matches against
  name, asset ID, make and holder name, case-insensitive.
- Filter pills, wrapping row, 6px gap: **All, Available, Issued, Overdue, Service**. Padding
  6px 11px, radius 999, 11.5px/500. Selected: `#1b4d8f` fill, white text, `#1b4d8f` border.
  Unselected: white, `#3f3d39` text, `rgba(0,0,0,.16)` border. "Service" covers both maintenance
  and calibration_due.
- Result count, 11.5px `#7a7872`: "N of M instruments".
- Instrument rows.

### 3. Instrument detail & history

- **Identity card**: white, `1px solid rgba(0,0,0,.09)`, radius 14, padding 15. Name 17px/600,
  line-height 1.25; asset ID mono 11px `#7a7872` 4px below; status pill top-right (11px, padding
  4px 8px). Then a 13px-padded top border and a two-column grid (`auto 1fr`, 7px 14px gap,
  12.5px): Make, Serial (mono 11.5px), Location, Calibration.
- **Current issue card**, when status is issued. Same card styling. Uppercase 11.5px/600 `#5d5b56`
  label "Current issue"; two-column grid: Issued to (name · role), Scientist, Period
  (`dd Mmm yyyy → dd Mmm yyyy`), Purpose. Then a due line, 12.5px/500 — `#8f2318` and
  "N days overdue · reminders going out daily" when late, else `#8a5a12` and
  "N days left before return". Two buttons: "Mark returned" (`#1b4d8f`, white) and
  "Send reminder" (white, bordered), both padding 11, radius 10, 12.5px/500.
- **Available actions**, when status is available: "Issue this instrument" (`#1b4d8f`, flex 1) and
  "Maintenance" (bordered, auto width).
- **Record** — the audit trail. Uppercase section label, then rows separated by
  `1px solid rgba(0,0,0,.08)`, padding 10px 0, 11px gap: a 74px fixed date column (mono 10.5px
  `#7a7872`, e.g. "28 Aug 26") and a body with event 12.5px/500 and detail 12px/1.45 `#5d5b56`.
  Newest first. Events to record: issued, returned, reminder sent, sent for calibration, added to
  register, status changed.

### 4. Issue instrument — four steps

A step bar at the top: four equal columns, 5px gap; each a 3px bar (radius 2) over a 10px label.
Completed and current steps use `#1b4d8f` and `#1b4d8f` text; pending use `rgba(0,0,0,.14)` and
`#9a9892`. Labels: Instrument, Person, Dates, Confirm.

Footer, on every step: "Back" (bordered, auto width, hidden on step 1) and a primary button
flexing to fill — "Continue" on steps 1–3 (`#1b4d8f`), and on step 4 either "Confirm & send mail"
or, with approval enabled, "Send for approval". Step 4's button is `#12695a`.

**Step 1 — instrument.** A dashed scan panel: `1.5px dashed rgba(27,77,143,.45)`, `#eef3fa` fill,
radius 14, padding 26px 16px, centred, 10px gap. A 74×74 square with `2px solid #1b4d8f`,
radius 12, holding "QR" in mono 10px. Then "Scan the asset label" 13px/500 `#1b4d8f`, and
"Each instrument carries a printed CBRI/APEEG QR sticker. Scanning fills the entry below." 12px/1.5
`#5d5b56`, max 26ch. In the real app this is the camera scanner reading the asset ID; the
prototype's "Simulate scan" button stands in for it. Below: "or choose manually" 11.5px `#7a7872`
centred, then a list of available instruments — bordered rows, radius 11, padding 11px 12px,
name 13px/500 over mono 10.5px asset ID; the selected row's border becomes `#1b4d8f`.

Only instruments with status `available` may be issued. Blocking an issue with a validation
message ("Scan or choose an instrument first.") is the intended behaviour.

**Step 2 — person.** A read-only recap card showing the chosen instrument (uppercase 10.5px label
"Instrument", name 13px/500). Then "Issue to project staff" section label and a selectable list of
project staff: bordered rows, radius 11, padding 11px 12px, 11px gap — a 34px circular avatar
(`#eef3fa` fill, `#1b4d8f` 11.5px/600 initials), name 13px/500, and "{role} · under {scientist}"
11.5px `#5d5b56`. Selected row border `#1b4d8f`. The scientist is derived from the staff member's
`reports_to`, not chosen separately.

**Step 3 — dates.** Four fields, 13px gap; each a 11.5px/500 `#5d5b56` label over an input
(padding 10px 12px, radius 10, `1px solid rgba(0,0,0,.16)`, white, 13px).
- Issue date — date picker, defaults to today.
- Expected return — date picker, defaults to today + `defaultLoanDays`. Helper text below,
  11.5px `#7a7872`: "Default loan period is N days. Reminder goes out M day(s) before, then daily
  once overdue."
- Purpose / project — text, placeholder "e.g. Envelope thermal monitoring, Bhopal site".
- Condition at issue — text, pre-filled "Complete with case and accessories".

Validation to add in the real app: return date must not precede the issue date.

**Step 4 — confirm.** A summary card (two-column grid, 8px 14px gap, 12.5px): Instrument, Asset ID
(mono), Issued to, Scientist, Period, Purpose — Purpose falls back to "Not stated" when blank.
Below it, the mail preview panel: `#eef3fa` on `1px solid rgba(27,77,143,.2)`, radius 14,
padding 14. Uppercase 11.5px/600 `#1b4d8f` heading "Mail goes to" (or "Approval request goes to").
Then one row per recipient: a 6px `#1b4d8f` dot, name 12.5px/500 over the address in mono 10.5px
`#5d5b56` (`overflow-wrap: anywhere`), and the role on the right in 10.5px `#5d5b56` — Holder,
Scientist, Group Head. **Deduplicate by email**: when the staff member's scientist *is* the Group
Head, show one row labelled "Scientist & Group Head". Then a top-bordered body preview, 12px/1.5
`#3f4d63`: "{instrument} ({asset ID}) is issued to {staff} from {date} to {date} for {purpose}.
Condition at issue: {condition}."

Confirming writes the `issue_record`, sets the instrument to `issued`, appends a history event,
sends the mail, logs it, and lands the user on the instrument's detail screen with a confirmation
toast.

### 5. Receive back

Intro copy 12.5px/1.5 `#5d5b56`: "Receiving an instrument closes the issue, records the condition
and mails an acknowledgement to the holder, their scientist and the Group Head."

Then one card per open issue — same as the instrument row but with a name/status line, the context
line, and two buttons: "Mark returned" (`#1b4d8f`, flexing) and "Open" (bordered). The real app
should also capture condition at return here, either inline or in a small sheet, before closing
the record.

### 6. Due & overdue

Intro line: "N overdue, M due within L day(s). Reminders are automatic; use the button to chase
immediately." When nothing is overdue: "M due within L day(s). Nothing overdue."

Then overdue items first, followed by items due within `reminderLeadDays`. Card padding 12px 13px,
radius 12; border `#e8b4ab` when overdue, else `rgba(0,0,0,.09)`. Name 13.5px/500 with a pill on
the right reading "N days late", "Due today" or "Due in N days" — overdue pill `#fbe4e0`/`#8f2318`,
due-soon `#fbf0dc`/`#8a5a12`. Context line: "{holder} · {scientist} · due {dd Mmm yyyy}". Then a
mono 10.5px `#7a7872` reminder line: "Auto reminder sent daily since {dd Mmm}" or "Advance notice
sent N day(s) before due". Buttons: "Send reminder now" (`#8f2318`, flexing) and "Received"
(bordered).

### 7. Add instrument

Intro: "New entries are added to the group register and get a printable QR asset label."

Six fields, each label 11.5px/500 `#5d5b56` over a standard input, 5px gap, 4px between fields:
Asset / CBRI ID (`CBRI/APEEG/0231`), Instrument name & model (`Testo 440 IAQ Kit`), Make (`Testo`),
Serial number (`440-01192`), Location / lab (`Energy Lab, Cabinet 4`), Calibration validity
(`Valid to 31 Mar 2027`). Then "Save to register", full width, `#1b4d8f`, radius 10, 13px/500.

Asset ID and name are required; asset ID must be unique. The real app should add quantity,
purchase date and cost, accessories, photo and manual upload, and should generate a printable QR
label carrying the asset ID.

### 8. Directory

One card per scientist: white, radius 12, padding 13. Header row — 36px circular avatar
(`#1b4d8f` fill, white 12px/600 initials), name 13.5px/600, role 11.5px `#5d5b56`, email in mono
10.5px `#7a7872`. Then a top-bordered block listing their project staff, 7px gap: name 12.5px on
the left, "N instrument(s) held" or "nothing held" 11px `#5d5b56` on the right. Footer: two
bordered buttons, "Add scientist" and "Add project staff". New staff must be mapped to a scientist,
since mail routing depends on it.

### 9. Mail log

Intro: "Every mail the system sends is logged against the instrument, so the record stands on its
own at audit time."

One card per mail: white, radius 10, padding 11px 12px, 6px between cards, `1px solid
rgba(0,0,0,.09)` on three sides and a 3px left border in the type colour. Subject 12.5px/500,
line-height 1.35, with the timestamp on the right in mono 10px `#7a7872`. Recipients 11.5px/1.45
`#5d5b56`. Type label 10.5px/500 in the type colour: Issue intimation `#1b4d8f` · Advance notice
`#8a5a12` · Overdue reminder `#8f2318` · Return acknowledgement `#12695a` · Calibration alert
`#5c4a86` · Manual reminder `#8f2318`. Newest first. Show `delivery_status` here too — a failed
send must be visible.

### 10. Reports

- Intro: "Position as on {today}. Exports carry the same figures with the full issue register
  attached."
- **Utilisation** card: four labelled bars. Label 12.5px left, "N of M" in mono 11.5px `#5d5b56`
  right, then a 6px track (`rgba(0,0,0,.07)`, radius 3) with a fill in the status colour at
  `count / total`. Rows: Issued / in use `#8a5a12`, Overdue `#8f2318`, Available in store
  `#12695a`, Repair / calibration `#5c4a86`.
- **Holdings by scientist** card: one row per scientist separated by `1px solid rgba(0,0,0,.07)` —
  name 12.5px, "N held" mono 11.5px, then "N overdue" `#8f2318` or "on time" `#12695a`.
- **Movement, last six months**: a 96px-tall grouped bar chart, one column per month, 7px gap.
  Each column holds two 2px-gapped bars (radius 2px 2px 0 0) in a 72px plot area — issued
  `#1b4d8f`, returned `#a8c2df` — over the month label 9.5px `#7a7872`. Legend below, 11px, with
  9px swatches. Source this from `issue_record` counts by month.
- **Attention list**: overdue items, then calibration-due, then items with vendor. Name 12.5px
  left, note 11px/500 right — "N days overdue" `#8f2318`, "calibration due" and "with vendor"
  `#5c4a86`.
- Two export buttons: "Export register (CSV)" (`#1b4d8f`) and "Monthly PDF" (bordered). CSV is one
  row per issue record. The monthly PDF is a statement for the Group Head with an overdue annexure.

## Interactions & behaviour

- **Navigation** is a flat stack. Bottom nav switches root screens; the app bar's back button
  returns to the previously visited screen (from instrument detail, back to whichever list opened
  it). Bottom nav item colours: active `#1b4d8f` with a `#dce7f4` icon fill, inactive `#8b8985`
  with a transparent fill; icon is a 22px rounded square with a 1.5px border, label 10px/500.
  "Items" stays highlighted while an instrument detail is open.
- **Toasts** confirm every write, and each one names the consequence rather than just the action —
  e.g. "Issued. Mail sent to Ankit Rawat, Dr. Kishor S. Kulkarni and Dr. Kishor S. Kulkarni.",
  "Received back. Acknowledgement mailed to the holder, the scientist and the Group Head.",
  "Reminder mailed to the holder and their scientist, Group Head copied.". Dark `#1b1a18`, white
  12px/1.45, radius 10, padding 11px 13px, `0 8px 24px rgba(0,0,0,.28)` shadow, pinned above the
  bottom nav, auto-dismiss after 4.2s. Validation failures use the same toast.
- **Overdue is derived, not stored.** An instrument's effective status is `overdue` when it is
  `issued` and `due_date < today`. Do not add an `overdue` value to the stored status enum — it
  would go stale.
- **Hover states** apply on tablet/web builds: instrument rows and pills take a `#1b4d8f` border;
  inputs take a `#1b4d8f` border on focus.
- **Offline.** Field use needs a local cache: read the register offline, queue issues and returns,
  sync and send mail when connectivity returns. Show queued-but-unsynced records as such.
- **Loading and error states** are not drawn in the prototype and need designing during build:
  list skeletons, a sync indicator, an SMTP-failure banner on the mail log, and a conflict message
  when two custodians issue the same instrument.

## State management

Prototype state, and what it maps to in a real app:

| Prototype state | Real app |
|---|---|
| `items` | `instrument` collection from the API, cached locally |
| `hist` | `issue_record` + status changes, queried per instrument |
| `mails` | `mail_log`, paginated |
| `screen`, `prev`, `sel` | navigation stack + route params |
| `query`, `filter` | local list UI state |
| `step`, `form` | issue wizard state, discarded on completion |
| `add` | add-instrument form state |
| `toast` | transient UI |

Data fetching: instrument list with status filter and search; instrument detail with its issue
history; open issues; due/overdue; mail log; report aggregates. Aggregates for Reports should be
computed server-side, not by pulling every record to the phone.

## Configuration

Three settings the prototype exposes, which should be admin-editable rather than hard-coded:

- `requireApproval` (boolean, default false) — when on, issue creates a `pending_approval` record
  and mails the scientist an approval request instead of an issue intimation. Step 4's button
  reads "Send for approval" and its heading "Approval request goes to".
- `reminderLeadDays` (int, default 2) — days before `due_date` the advance notice goes out.
- `defaultLoanDays` (int, default 14) — pre-fills the expected return date.

## Design tokens

**Colours**
```
Primary / app bar        #1b4d8f
Primary hover / dark     #123869
Primary tint (panels)    #eef3fa
Primary tint (nav icon)  #dce7f4
Chart secondary          #a8c2df

Page background          #f4f3f0     (prototype page)
Screen background        #f7f6f3
Surface                  #ffffff
Ink                      #1b1a18
Body text                #3f3d39
Secondary text           #4a4844
Muted text               #5d5b56
Faint text               #7a7872
Disabled / inactive      #8b8985 · #9a9892

Hairline                 rgba(0,0,0,.07)
Border                   rgba(0,0,0,.09)
Border (input)           rgba(0,0,0,.16)
Rule                     rgba(0,0,0,.12)

Status — available       text #12695a   fill #e2efe9
Status — issued          text #8a5a12   fill #fbf0dc
Status — overdue         text #8f2318   fill #fbe4e0   border #e8b4ab   alert fill #fbe7e3
Status — maintenance     text #5c4a86   fill #ece7f7
Status — calibration due text #5c4a86   fill #ece7f7
Overdue body text        #6d3a33
Mail preview body        #3f4d63
```

**Typography** — IBM Plex Sans throughout; IBM Plex Mono for asset IDs, serial numbers, email
addresses, dates in the history column and eyebrow labels.
```
Screen title      16px / 600 / -.01em
App bar eyebrow   mono 10px / .08em / uppercase
Stat number       26px / 600 / -.02em
Detail title      17px / 600 / 1.25 / -.01em
Section label     11.5–12px / 600 / .06em / uppercase
Row title         13.5px / 500 / 1.3
Body              12.5–13px / 1.5
Small             11.5px
Caption           10.5–11px
Mono ID           10.5–11.5px
Nav label         10px / 500
```

**Spacing** — 4 · 5 · 7 · 9 · 11 · 13 · 14 · 16 · 18px. Screen padding 16px; card padding
13–15px; input padding 10–11px vertical, 12–13px horizontal.

**Radius** — 6 (pill/badge) · 8 (small button) · 9–10 (button, input) · 11–12 (card) ·
14 (large card) · 999 (filter pill) · 50% (avatar).

**Shadow** — only the toast: `0 8px 24px rgba(0,0,0,.28)`. Cards use borders, not shadows.

## Assets

No images or icon set. Bottom-nav icons are placeholder rounded squares and need a real icon set
in the target codebase (Material Symbols on Android is the obvious fit): home, list, add/scan,
clock or alert, people. The QR panel on issue step 1 is a placeholder for the camera scanner. No
institute logo is used in the prototype — if CBRI branding is required on the app bar or in mail
templates, get the official mark from the institute rather than recreating it.

## Suggested implementation

- **App**: Flutter or React Native — one codebase for Android and iOS.
- **Backend**: Django + PostgreSQL (Django admin doubles as the master register editor), or
  Supabase for less server work.
- **Mail**: institutional SMTP via a sending mailbox cleared by the computer centre.
- **Scheduler**: cron or Celery beat for the daily 09:00 reminder job.
- **Live updates**: WebSockets or Supabase realtime if custodians' lists must update live; for a
  group this size, pull-to-refresh plus a short poll is sufficient and cheaper to maintain.

Suggested build order: import the existing register → print QR labels → issue and return with mail
(this alone replaces the logbook) → maintenance and calibration alerts → reports → read-only
scientist logins last, once the data is trusted.

## What the group needs to supply

- The existing instrument register as a spreadsheet, for import.
- Scientists and project staff with official email IDs and the reporting mapping.
- A decision on whether issue needs the scientist's approval first.
- One institutional sending mailbox with SMTP credentials, and a machine that stays on for the
  daily job.
- Printed QR or barcode labels per instrument carrying the asset ID.
- A hosting decision. If data must stay inside the CBRI network, this needs an institute server
  with intranet-only or VPN access, which also rules out third-party cloud mail.

## Files

```
APEEG Instrument Register.dc.html   the prototype — all nine screens, sample data, all interactions
android-frame.jsx                   phone bezel used by the prototype only; do not port
README.md                           this document
```

Sample data in the prototype (10 instruments, 3 scientists, 4 project staff, 5 logged mails) is
illustrative and drawn from the kind of equipment the group uses. Replace it with the real
register; do not treat it as the group's actual inventory.



