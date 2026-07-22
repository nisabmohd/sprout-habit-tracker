# Design tokens (extracted from `Habit Tracker UI.html`)

PNGs in this folder are rendered from that file (390 dp wide artboards at 2x).

## Colors
`tone(h, s, l)` = `hsl(h s% l%)`. App seed hue = **150**.

App palette `pal(h, dark)`:

| Token | Light | Dark |
| --- | --- | --- |
| bg | tone(h,30,96) | tone(h,14,7) |
| card | tone(h,40,99) | tone(h,12,12) |
| hi | tone(h,28,91) | tone(h,12,18) |
| sc | tone(h,32,93) | tone(h,14,15) |
| pri | tone(h,48,30) | tone(h,55,76) |
| onPri | #ffffff | tone(h,45,14) |
| pc | tone(h,55,86) | tone(h,30,25) |
| onPc | tone(h,55,14) | tone(h,60,88) |
| text | tone(h,15,11) | tone(h,10,93) |
| muted | tone(h,8,36) | tone(h,8,68) |
| line | tone(h,14,84) | tone(h,8,28) |

Habit palette `hab(hue, dark)`:

| Token | Light | Dark |
| --- | --- | --- |
| solid | tone(h,48,40) | tone(h,50,66) |
| soft | tone(h,55,92) | tone(h,22,19) |
| mid | tone(h,48,82) | tone(h,30,30) |
| on | #ffffff | tone(h,40,12) |
| ink | tone(h,50,24) | tone(h,60,84) |

Habit card: bg = skip ? hi : card; progress fill = done ? mid : soft; icon tile = done ? solid : partial ? mid : skip ? card : soft.

Sample habit hues: sun 38, book 215, drop 192, calm 275, dumbbell 12, pill 95, leaf 150, pen 330.

## Habit icons (24x24, stroke 1.9, round caps/joins, no fill)
- sun: circle(12,12,r4) + `M12 2.5v2M12 19.5v2M4.6 4.6L6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4L6 18M18 6l1.4-1.4`
- book: `M5 4.5A1.5 1.5 0 0 1 6.5 3H19v14H6.5A1.5 1.5 0 0 0 5 18.5z` + `M5 18.5A1.5 1.5 0 0 0 6.5 20H19`
- drop: `M12 3.5s6 6.4 6 10.5a6 6 0 0 1-12 0c0-4.1 6-10.5 6-10.5z`
- calm: `M12 19c-1.8-1.6-3-3.8-3-6.5s1.2-5.2 3-7c1.8 1.8 3 4.3 3 7s-1.2 4.9-3 6.5z` + `M12 19c-3.5 0-7-2-8-6 2.6-.2 5 .6 6.6 2.2` + `M12 19c3.5 0 7-2 8-6-2.6-.2-5 .6-6.6 2.2`
- dumbbell: `M6.5 7v10M3.5 9.5v5M17.5 7v10M20.5 9.5v5M6.5 12h11`
- pill: rect(x3 y8.5 w18 h7 rx3.5) rotated -45° about (12,12) + `M9.5 9.5l5 5`
- leaf: `M5 19c0-8 5-14 14-14 0 9-6 14-14 14z` + `M5 19l8-8`
- pen: `M4 20h4L19 9l-4-4L4 16z` + `M14 6l4 4`

UI glyphs: plus `M12 5v14M5 12h14`, check `M5 12.5l4.5 4.5L19 7.5`.
