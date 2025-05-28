# PRD · **Split‑the‑Bill (Android)**  
_A lightweight showcase of AI‑assisted bill splitting for your Londroid talk._

---

## 1. Product Overview
|                                |                                                                                                                                                                         |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Goal**                       | Let a group of friends photograph a UK restaurant receipt, edit items if needed, assign costs, mark their own payments, and keep a history of settled/unsettled events. |
| **Why now?**                   | Demonstrates how LLMs remove the most tedious step—cleaning messy OCR—while the app handles UX, math, and storage.                                                      |
| **Success-metric (talk demo)** | 1‑minute live demo: from photo → clear item list → split → mark paid → see history, with no manual data entry except names.                                             |

---

## 2. Key Features (MVP)

1. **Receipt capture + LLM parsing** (English, UK format)  
2. **Participant list** (locally persisted, reusable)  
3. **Split modes**  
   - Equal share  
   - Manual item assignment (items can be shared)  
4. **Payment tracking** (each person marks their share as paid; notes optional)  
5. **Event history** (saved locally; reopen & edit)  
6. **Text summary sharing** (WhatsApp/SMS)  

---

## 3. Out of Scope (v1)

- Online accounts or cloud sync  
- Tip/tax calculation beyond what's on the receipt  
- Multi‑currency or multi‑language  
- In‑app money transfer (e.g., PayPal API)  
- LLM matching items ↔︎ people  

---

## 4. Target Users & Scenarios

| Persona              | Scenario                                                                                 |
| -------------------- | ---------------------------------------------------------------------------------------- |
| **Organiser Olivia** | Takes the receipt photo, cleans item list, assigns items, asks friends to mark payments. |
| **Casual Chris**     | Just wants to see "£12.40" next to his name and tap **Paid**.                            |
| **Forgetful Faye**   | Opens History later to confirm she actually paid.                                        |

---

## 5. Incremental Delivery Plan

> Each milestone is designed to compile & run on‑device, add one user‑visible win, and keep the codebase demo‑ready.

### Milestone-0 · Project Foundation
- Create Android/Kotlin repo with CI, lint, unit‑test skeleton.  
- Set up Jetpack Compose starter UI, navigation, Room (or DataStore).  
- **Done when:** empty Home screen launches; unit test passes.

---

### Milestone-1 · Receipt Capture & LLM Parse
- Add a Material Design Floating Action Button (FAB) at the bottom right of the main screen.
- On tap, open a chooser for Camera (take photo) or Camera Roll (pick from gallery; emulator-friendly).
- After image selection, reduce the image size to minimize the number of tokens used when sending to OpenAI.
- Send the resized image directly to OpenAI (via a mockable wrapper) using the gpt-4o-mini model, with a prompt:
  > "Detect if this is a UK expense receipt. If so, extract the data using structured JSON output. Return either an error if the photo is invalid or not a receipt, or the following structure, using OpenAI's structured output format and referencing the OpenAPI schema:
  > ```openapi
  > components:
  >   schemas:
  >     ReceiptParseResult:
  >       type: object
  >       properties:
  >         error:
  >           type: string
  >           description: Error message if the image is invalid or not a receipt
  >         items:
  >           type: array
  >           items:
  >             type: object
  >             properties:
  >               name:
  >                 type: string
  >               cost:
  >                 type: number
  >         service:
  >           type: number
  >           description: Service charge if present
  >         total:
  >           type: number
  >           description: Total amount on the receipt
  > ```
- If the response contains an error, show a clear error message (e.g., "Invalid photo – please try again").
- If successful, display an editable list view showing each item (name, cost), service charge (if present), and total. Allow the user to edit items and save the draft locally.
- **Done when:** user can tap the FAB, select/take a photo, see parsed receipt (or error), edit the list, and save the draft locally.

---

### Milestone-2 · Participants
- Add/select participants screen.  
- Persist names locally; simple validation.  
- **Done when:** names survive app restarts and appear in split flow.

---

### Milestone-3 · Split Engine
- Equal‑split math.  
- Manual assignment UI (tap item → pick friend(s) → auto‑recalc).  
- **Done when:** totals per person update in real time.

---

### Milestone-4 · Payment Tracking
- "Mark as paid" toggle + optional note per person.  
- Calculate outstanding amount for event.  
- **Done when:** bill shows green "Settled" banner when all paid.

---

### Milestone-5 · Event History
- List past events with status badges (✔︎ settled / ⏳ outstanding).  
- Detail screen to reopen and edit any event.  
- **Done when:** user can find yesterday's dinner and adjust a mistake.

---

### Milestone-6 · Share Summary & Polish
- Generate plain‑text recap:  
  > "Dinner at Luigi's — Tom paid. Alice owes £18.20, Josh owes £12.10."  
- System share sheet.  
- Final UI tidy‑up, icons, dark theme.  
- **Done when:** 1‑minute demo path is smooth on stage.

---

## 6. LLM Integration Details

| Stage         | Service          | Prompt Snippet                                                                 | Output Contract                                                               |
| ------------- | ---------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| Receipt parse | OpenAI or Gemini | "You are a parser for UK receipts… output JSON array of `{item, price}` only." | `List<BillItem>` validated against schema; fallback to manual entry on error. |

---

## 7. Major Risks & Mitigations

| Risk                          | Mitigation                                                    |
| ----------------------------- | ------------------------------------------------------------- |
| Poor OCR on crumpled receipts | Keep gallery‑import for demo; provide manual edit mode.       |
| LLM latency in live demo      | Cache mock JSON or pre‑warm call; add "Parsing…" shimmer.     |
| Data loss/unpaid edge cases   | Persist drafts immediately; unit test split + rounding logic. |

---

## 8. Future Ideas (Post‑MVP)

- Cloud sync & multiple devices  
- Tip suggestions based on UK norms  
- Multi‑currency & localisation  
- Auto item ↔︎ participant suggestions via LLM  
- Built‑in payment links (Monzo.me, PayPal, Revolut)  

---

### 📅 Suggested Timeline (aggressive demo pace)

| Week | Milestone            |
| ---- | -------------------- |
| 1    | 0, 1                 |
| 2    | 2                    |
| 3    | 3                    |
| 4    | 4                    |
| 5    | 5                    |
| 6    | 6 + rehearsal buffer |

---

**That's the whole PRD in one page.**  
Let me know which milestone you'd like to flesh out first—or if you need wireframes, prompts, or code snippets next!