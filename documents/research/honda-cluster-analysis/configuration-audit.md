# Honda configuration audit: complete catalog and language paths

The earlier summaries were selective. This pass inventories all 313 entries in the supplied stock Honda XML and completes the previously unfinished RZC language trace. It is not a claim that every feature in every Honda/FYT firmware has been traced or works on the user's car.

## Confirmed language settings

| Path | Choices / Portuguese mapping | Evidence / limits |
|---|---|---|
| Original Honda cluster, South America | Setting 03/0E. API1 = Português, encoded0; API2 = English, encoded1. Source50/menu23. | XML, stock UI resource array and value conversions verified. Runtime support still required. |
| FYT RZC Honda settings | 33 choices. Portuguese is value15: SYU command105 with integers [85,15]. | Send path and menu route verified. No current-language callback found; not tested on a car. |
| FYT WC Civic panel | English, Chinese, traditional Chinese; command112 [1,value], values1–3. | No Portuguese in this picker; no verified language readback. |
| FYT WC Accord low settings | English/Chinese; command17 [value], values1–2. | Adjacent Accord path; not established as Civic support. |
| Original Honda head-unit language | `ChangeLanguageActivity` has Brazilian Portuguese mapped to `pt_BR`. | Separate UnitInfoManager path; not the same as cluster customization. |

**The RZC picker has Portuguese.** The prior “no Portuguese” finding applied only to the three-option WC Civic picker, not all FYT implementations.

- [All 15 stock language entries, routes and named values](stock-language-review.md).
- [All 33 RZC choices, command, profile restrictions and callback audit](fyt-additional/other-honda/language-findings.md).
- [Machine-readable RZC language options](fyt-additional/other-honda/rzc-language-options.json).

## Full source inventory

- [All 313 stock entries, readable table](complete-settings-catalog.md).
- [Complete JSON: values, encodings, ranges, route metadata and label gaps](complete-settings-catalog.json).
- [CSV for filtering and sorting](complete-settings-catalog.csv).
- [Export/validation script](export_complete_catalog.py).
- [Complete SiYu/RZC screen-row inventories](fyt-additional/doors-lights/full-settings-inventory.md).

The script verifies every XML category/ID pair against the existing Honda client catalog: 313 unique entries, matching data types, value maps and route flags. There are 167 entries with nonempty route metadata and 146 without. There are 15 language-related entries: eight routed, seven without routes. The stock catalog includes variant duplicates and placeholder/default entries.

249 entries have titles in the existing English/default resource catalog. The others retain their original source descriptions. Missing/empty option labels are explicitly recorded as unresolved in the JSON; no language is assigned to extra values merely by guessing the next index. Named labels come from the existing resource-derived Honda client catalog, while the XML remains authoritative for identifiers, values and routes.

## Scope and next implementation boundary

Newly discovered settings include units, two tachometer controls, language, mirror folding, remote windows, tailgate behavior, camera preferences and maintenance/reset actions. Some entries overlap across screens or are navigation/readout rows rather than settings. See the detailed inventories for their status instead of counting every row as an independent writable feature.

This pass changed research artifacts only. Cabin still has its previously implemented 13 panel controls. No new language, unit, tachometer or reset control has been added, and no vehicle command was sent. In particular, the RZC language writer lacks confirmed readback and must not report a successful language change based on a local checkmark.
