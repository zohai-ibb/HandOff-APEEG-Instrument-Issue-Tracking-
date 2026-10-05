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





