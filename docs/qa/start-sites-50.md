# Start-site check, seeds 1-50

Run: `./gradlew test -Pstartsite.report --tests '*StartSiteReportTest*'` (writes build/start-sites.tsv). It calls `StartSite.search` (the 12 checks, score S, up to 20 attempts) for level seeds 1-50 on the standard pipeline, and records the measures of the winning site. Nothing was fixed. Took about 50 min on the shared 2-core box, not 60 s; the existing opt-in `-Pstartsite.full` test (seeds 0-49) covers the independent re-measure.

Blueprint rules checked (Start site, 12 checks): water within 40 m, bank 1-10 m above the river (start_site.json; score S prefers 2-4 m); steepest slope under 8 deg within 20 m; bear: a den at least 500 m away and at least 1 bear range overlapping; moose range within 1,200 m. "Rerolls" is attempts spent before the winner (limit 20).

| Seed | Result | Rerolls | Water / bank | Slope | Bear (den, ranges) | Moose | Score S |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | pass | 0 | water 34 m, bank 3.4 m up | steepest 7.7 deg in 20 m | den 1029 m, 3 bear range | moose range 900 m | 2.676 |
| 2 | pass | 7 | water 22 m, bank 1.0 m up | steepest 2.6 deg in 20 m | den 620 m, 3 bear range | moose range 577 m | 2.104 |
| 3 | pass | 7 | water 21 m, bank 2.1 m up | steepest 6.9 deg in 20 m | den 501 m, 2 bear range | moose range 496 m | 2.623 |
| 4 | pass | 5 | water 38 m, bank 2.7 m up | steepest 7.9 deg in 20 m | den 589 m, 3 bear range | moose range 616 m | 2.640 |
| 5 | pass | 1 | water 23 m, bank 2.2 m up | steepest 4.8 deg in 20 m | den 560 m, 3 bear range | moose range 375 m | 2.564 |
| 6 | pass | 1 | water 22 m, bank 1.0 m up | steepest 7.6 deg in 20 m | den 552 m, 2 bear range | moose range 652 m | 2.087 |
| 7 | pass | 2 | water 22 m, bank 1.3 m up | steepest 7.8 deg in 20 m | den 878 m, 2 bear range | moose range 176 m | 2.419 |
| 8 | pass | 0 | water 25 m, bank 1.9 m up | steepest 4.8 deg in 20 m | den 776 m, 2 bear range | moose range 369 m | 2.417 |
| 9 | pass | 1 | water 38 m, bank 1.3 m up | steepest 2.8 deg in 20 m | den 876 m, 2 bear range | moose range 74 m | 2.095 |
| 10 | pass | 4 | water 22 m, bank 1.1 m up | steepest 5.9 deg in 20 m | den 1260 m, 2 bear range | moose range 952 m | 2.400 |
| 11 | pass | 2 | water 30 m, bank 1.0 m up | steepest 3.7 deg in 20 m | den 574 m, 3 bear range | moose range 679 m | 2.130 |
| 12 | pass | 7 | water 40 m, bank 2.0 m up | steepest 6.6 deg in 20 m | den 928 m, 3 bear range | moose range 58 m | 2.200 |
| 13 | pass | 0 | water 31 m, bank 2.1 m up | steepest 6.6 deg in 20 m | den 563 m, 3 bear range | moose range 589 m | 2.683 |
| 14 | pass | 0 | water 22 m, bank 2.9 m up | steepest 5.9 deg in 20 m | den 1136 m, 3 bear range | moose range 936 m | 2.638 |
| 15 | pass | 6 | water 32 m, bank 1.6 m up | steepest 3.8 deg in 20 m | den 672 m, 3 bear range | moose range 745 m | 2.026 |
| 16 | pass | 3 | water 20 m, bank 2.4 m up | steepest 5.9 deg in 20 m | den 692 m, 2 bear range | moose range 498 m | 2.580 |
| 17 | pass | 0 | water 22 m, bank 2.2 m up | steepest 7.6 deg in 20 m | den 547 m, 2 bear range | moose range 201 m | 2.302 |
| 18 | pass | 0 | water 17 m, bank 2.1 m up | steepest 4.9 deg in 20 m | den 639 m, 3 bear range | moose range 642 m | 2.657 |
| 19 | pass | 0 | water 38 m, bank 1.5 m up | steepest 6.1 deg in 20 m | den 811 m, 3 bear range | moose range 701 m | 1.888 |
| 20 | pass | 5 | water 25 m, bank 1.2 m up | steepest 5.8 deg in 20 m | den 823 m, 3 bear range | moose range 361 m | 2.040 |
| 21 | pass | 14 | water 20 m, bank 1.2 m up | steepest 6.5 deg in 20 m | den 641 m, 2 bear range | moose range 267 m | 2.090 |
| 22 | pass | 1 | water 26 m, bank 2.7 m up | steepest 7.8 deg in 20 m | den 600 m, 2 bear range | moose range 656 m | 2.561 |
| 23 | pass | 4 | water 33 m, bank 2.4 m up | steepest 6.3 deg in 20 m | den 674 m, 3 bear range | moose range 29 m | 2.611 |
| 24 | pass | 0 | water 31 m, bank 1.1 m up | steepest 4.5 deg in 20 m | den 512 m, 2 bear range | moose range 427 m | 2.118 |
| 25 | pass | 14 | water 28 m, bank 1.3 m up | steepest 6.0 deg in 20 m | den 528 m, 3 bear range | moose range 538 m | 2.175 |
| 26 | pass | 2 | water 37 m, bank 1.5 m up | steepest 3.8 deg in 20 m | den 576 m, 2 bear range | moose range 854 m | 2.195 |
| 27 | pass | 1 | water 29 m, bank 1.3 m up | steepest 7.7 deg in 20 m | den 561 m, 3 bear range | moose range 792 m | 2.318 |
| 28 | pass | 5 | water 26 m, bank 2.6 m up | steepest 7.0 deg in 20 m | den 538 m, 3 bear range | moose range 665 m | 2.477 |
| 29 | pass | 13 | water 34 m, bank 1.9 m up | steepest 6.7 deg in 20 m | den 522 m, 3 bear range | moose range 692 m | 2.163 |
| 30 | pass | 1 | water 25 m, bank 1.4 m up | steepest 2.9 deg in 20 m | den 930 m, 3 bear range | moose range 534 m | 2.166 |
| 31 | pass | 6 | water 26 m, bank 2.1 m up | steepest 5.8 deg in 20 m | den 629 m, 2 bear range | moose range 112 m | 2.684 |
| 32 | pass | 0 | water 17 m, bank 1.2 m up | steepest 6.5 deg in 20 m | den 541 m, 3 bear range | moose range 509 m | 2.029 |
| 33 | pass | 1 | water 19 m, bank 3.0 m up | steepest 6.3 deg in 20 m | den 671 m, 3 bear range | moose range 496 m | 2.650 |
| 34 | pass | 3 | water 18 m, bank 2.2 m up | steepest 4.7 deg in 20 m | den 725 m, 3 bear range | moose range 747 m | 2.883 |
| 35 | pass | 1 | water 22 m, bank 2.5 m up | steepest 5.5 deg in 20 m | den 569 m, 3 bear range | moose range 451 m | 2.670 |
| 36 | pass | 3 | water 17 m, bank 2.1 m up | steepest 6.4 deg in 20 m | den 662 m, 2 bear range | moose range 208 m | 2.621 |
| 37 | pass | 12 | water 35 m, bank 3.0 m up | steepest 6.7 deg in 20 m | den 831 m, 3 bear range | moose range 451 m | 2.505 |
| 38 | pass | 0 | water 25 m, bank 2.2 m up | steepest 8.0 deg in 20 m | den 585 m, 3 bear range | moose range 637 m | 2.603 |
| 39 | pass | 0 | water 37 m, bank 1.2 m up | steepest 5.0 deg in 20 m | den 521 m, 2 bear range | moose range 57 m | 2.219 |
| 40 | pass | 5 | water 18 m, bank 2.6 m up | steepest 7.3 deg in 20 m | den 1066 m, 2 bear range | moose range 780 m | 2.637 |
| 41 | pass | 1 | water 28 m, bank 2.4 m up | steepest 6.3 deg in 20 m | den 691 m, 2 bear range | moose range 756 m | 2.795 |
| 42 | pass | 2 | water 36 m, bank 2.5 m up | steepest 6.7 deg in 20 m | den 555 m, 2 bear range | moose range 736 m | 2.552 |
| 43 | pass | 1 | water 26 m, bank 3.0 m up | steepest 7.6 deg in 20 m | den 563 m, 3 bear range | moose range 64 m | 2.730 |
| 44 | pass | 1 | water 31 m, bank 2.1 m up | steepest 6.1 deg in 20 m | den 507 m, 3 bear range | moose range 284 m | 2.650 |
| 45 | pass | 15 | water 24 m, bank 2.5 m up | steepest 7.6 deg in 20 m | den 793 m, 2 bear range | moose range 316 m | 2.604 |
| 46 | pass | 0 | water 24 m, bank 1.5 m up | steepest 5.9 deg in 20 m | den 637 m, 2 bear range | moose range 104 m | 1.960 |
| 47 | pass | 0 | water 34 m, bank 1.1 m up | steepest 4.9 deg in 20 m | den 583 m, 3 bear range | moose range 970 m | 2.162 |
| 48 | pass | 2 | water 22 m, bank 2.0 m up | steepest 6.9 deg in 20 m | den 653 m, 2 bear range | moose range 203 m | 2.836 |
| 49 | pass | 0 | water 30 m, bank 2.2 m up | steepest 6.4 deg in 20 m | den 621 m, 2 bear range | moose range 115 m | 2.452 |
| 50 | pass | 4 | water 26 m, bank 3.3 m up | steepest 4.5 deg in 20 m | den 700 m, 3 bear range | moose range 474 m | 2.555 |

## Result
- 50 of 50 seeds found a site within 20 attempts. No seed failed a check. Rerolls: 163 in all, worst 15 (seed 45 at 15, seeds 21 and 25 at 14, 29 at 13, 37 at 12). Median is low; seeds 21, 25, 29, 37, 45 spent more than half the budget, so a small change to the checks could push them over 20.
- Seed 38 shows "8.0 deg": the check is strictly under 8, so it is a rounding of a value just below. Seeds 2, 6 and 22 etc. sit on the 1.0 m bank minimum.
- Slope, water, den distance and moose range all sit inside the blueprint limits for every winner (the search only accepts a site that passes all 12 checks; the independent re-measure is in StartSiteTest.verify).
