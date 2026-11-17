# Snipe Dodge 4.0 — implementation and QA plan

## Delivered in source
- Snipe Bot procedural 2D sprite and 12 cosmetic color variants
- Four world palettes, parallax particles, modes, collisions, dash, shield, magnet, slow-time, near-miss combos
- Basic boss rush waves and boss HP display
- Local persistent coins, shop, random cosmetic chest, progression, stats
- 20 threshold achievements, daily/weekly quests with claim buttons
- Tutorial, pause, menu, graphics and FPS controls, basic touch control
- Short optional SFX; removed annoying looping synthesized background music

## Remaining for production quality
1. Verify GitHub Actions build and resolve compiler/resource errors.
2. Install APK on Android 11, test gameplay, input and rotation, verify persistence after app restart.
3. Replace color-swap bots with 12 illustrated skins and design a polished HD adaptive launcher icon.
4. Implement three genuinely different boss attack patterns and player-controlled boss damage (current boss logic is prototype).
5. Add properly licensed gentle background music, independently adjustable music/SFX volume and audio focus.
6. Add true collision tuning, world-specific geometry and assets, accessible controls and screen aspect handling.
7. Fix quest and achievement progression edge cases and add explicit achievement reward UX.
8. Profile 30/60 FPS, CPU, battery and memory on a physical Android 11 device.
9. Add automated unit/instrumentation tests and publish signed release APK.

## QA acceptance checklist
- [ ] Gradle assembleDebug passes
- [ ] APK installs on Android 11
- [ ] No crash in 15 minutes of play
- [ ] Menus and pause respond consistently
- [ ] Daily/weekly rewards cannot be claimed repeatedly
- [ ] Shop and unlocked skins persist after restart
- [ ] Gameplay collision and dash feel responsive
- [ ] Sound is quiet and can be disabled
- [ ] FPS and memory measured on target phone

This is a prototype milestone, not a certified production release.
