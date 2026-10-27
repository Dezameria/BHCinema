# Changelog

All notable changes to bhcinema (BeeHouse Cinema). Client-side only: servers do not need it.

## 0.8.1 — 2026-10-25
- **Fixed**: the "Voice Chat From" setting was too long for its button, so the text scrolled and read as "Voice Chat
  From: Voice Chat From". It now takes a whole row, and its choices are shorter: Character, Cam, Character + Cam.

## 0.8.0 — 2026-10-25
- **Added**: Plasmo Voice can hear from your player and the camera at once. `[general] voiceListener` (replaces
  `voiceFromCamera`) picks where voice chat is heard from while a camera is on: `PLAYER`, `CAMERA`, or `BOTH`
  (default). In BOTH each voice is as loud as from whichever is closer, your player or the camera, so you hear people
  near you and people you fly the camera up to; directions are heard from the camera. The server rules still apply
  (`pv.allow_freecam`, and only voices within talking range of your player are sent).

## 0.7.1 — 2026-10-25
- **Fixed**: Plasmo Voice from the camera (0.7.0) only moved where voices seemed to come from, not how loud they were.
  Plasmo Voice measures voice distance from the camera *entity*, which a BHCinema camera leaves at the player, so
  flying up to someone did not make them louder. Voice distance is now measured from the BHCinema camera too, under the
  same rules (the server's `pv.allow_freecam`, on by default).

## 0.7.0 — 2026-10-25
- **Fixed**: in Epic Fight mode from first person, a camera still showed the plain vanilla body instead of the animated
  Epic Fight model. figura_extra_fight (Figura + Epic Fight) turns Epic Fight's model off in first person when a
  Figura avatar is loaded, and the game stays in first person under a BHCinema camera. While a camera is on it now sees
  third person, as with F5. Does nothing without figura_extra_fight.
- **Changed**: Plasmo Voice listens from the camera while a camera is on (`[general] voiceFromCamera`, replaces
  0.5.0's `voiceFromPlayer`), so flying the freecam up to someone talking makes them clearer. This works even with
  Plasmo Voice's own camera listener option off. The server must still allow freecam listening (`pv.allow_freecam`),
  and it only sends voices within talking range of your player.

## 0.6.0 — 2026-10-25
- **Fixed**: with Epic Fight's first-person model on, turning on a camera from first person left your character in
  Epic Fight's stiff first-person pose. The game stays in first person under a BHCinema camera, so Epic Fight still
  posed the body (and bent the view) for first person. While a camera is on, Epic Fight now treats the player as seen
  in third person. Its own shoulder camera stays off. Does nothing without Epic Fight.

## 0.5.0 — 2026-10-25
- **Added**: Plasmo Voice support (`[general] voiceFromPlayer`, on by default). With Plasmo Voice's Camera Sound
  Listener on, voice chat is heard from the camera, so it went quiet when the freecam flew away. While a BHCinema
  camera is on, voice chat is now heard from your player. Does nothing without Plasmo Voice.

## 0.4.0 — 2026-10-24
- **Changed**: auto-hide HUD now hides the whole HUD, not only the vanilla hotbar, health, food and XP: HUDs from
  other mods (health bars, stamina, skill icons...), chat, the crosshair and everything else drawn over the world. The
  first-person hand stays. It still shows for a moment when you change hotbar slot, and also while you type in chat
  or have F3 open. The fade covers modded HUDs too.

## 0.3.0 — 2026-10-24
Changed defaults only apply to settings you never saved: an existing `config/bhcinema-client.toml` keeps
`cameraCollision = true`, and `options.txt` keeps F6 for Behind. Use Reset All in the settings (F7) and reset the key in
Controls to pick up the new defaults.

- **Added**: freecam starts a little behind your head (`[freecam] startDistance`, 2 blocks; 0 = from the eyes), so you
  are in the shot right away. It never starts inside a wall, even with collision off.
- **Changed**: camera collision is off by default (cameras pass through blocks).
- **Changed**: the Behind camera has no key by default; set one in Controls.
- **Fixed**: holding jump (or WASD) while flying the freecam could make the player jump or move with it in modpacks
  where Epic Fight or skill mods read the keys directly. The movement keys now stay released for the game while the
  freecam flies.
- **Fixed**: Epic Fight's animated first-person arms showed while a camera was on.

## 0.2.0 — 2026-10-24
Config sections and several key names changed, so `config/bhcinema-client.toml` falls back to defaults for the moved
settings once (framing values carry over). Saved key bindings for removed keys are dropped.

- **Added**: freecam lock. In freecam (F4), **B** locks the camera where it was flown to and gives control back to the
  player; **B** again unlocks it to fly somewhere else. **G** switches the lock between Locked (stays put, catches up
  when you walk past `lockedCatchUpDistance`) and Follow (travels with you; middle mouse orbits, middle mouse + scroll
  changes the distance). While flying, G picks the mode the next lock uses.
- **Added**: Behind camera on **F6**, F5-style: behind you, turning with your face, with smooth turning, wall
  collision and its own `distance` / `cameraIgnoresJumps` / `heightSmoothing`.
- **Added**: framing shift (left/right, up/down, forward/back) for the locked freecam and Behind, with -/+/0 buttons
  and a live preview (the world is not dimmed or paused on that tab).
- **Added**: camera collision (on by default): flying slides along walls, Follow and Behind move in front of walls with
  a quick pull-in and an eased release, so they do not jitter.
- **Added**: jump-ignoring height tracking for Lock and Behind, so jumps do not bounce the shot.
- **Added**: auto-hide HUD with a fade-out (block items in the hotbar fade too), shown for a moment when the hotbar
  slot changes.
- **Added**: in-game settings (F7 or Mods → BHCinema → Config) in tabs: All Cameras, Freecam, Lock, Behind, Framing,
  HUD, with Reset Tab / Reset All. Bilingual (English / Thai) labels, tooltips and toml comments.
- **Changed**: plain scroll always changes the hotbar slot. Fly speed is **Speed Modifier (Z) + scroll**, FOV zoom is
  **Zoom Modifier (Left Alt) + scroll**; both keys can be rebound.
- **Changed**: left / right click work in freecam (from the player's own view).
- **Changed**: roll is on the Left / Right arrow keys and does not apply to Behind. **R** (Reset Camera) resets roll,
  zoom and distance as a cut, not an eased swing. Scroll-wheel distance tweaks last for one session.
- **Changed**: freecam always starts from the first-person view, even with F5 on. View bobbing is off while a camera
  is on.
- **Changed**: keybind category renamed to **BHCinema**.
- **Fixed**: Lock / Follow starting from first person put the camera inside the player's head (and look-at-player aimed
  at the ground); the camera now backs out behind the player first.
- **Fixed**: MixinExtras is bundled with jarJar. `@WrapOperation` (used so the mouse hook coexists with Epic Fight)
  crashed at launch because Forge 1.20.1 does not ship MixinExtras. Use `bhcinema-<version>.jar`, not `-slim.jar`.

## 0.1.0 — 2026-10-23
- First version: smooth freecam (F4) with frame-rate independent movement and correct WASD directions, scroll speed,
  roll, FOV zoom and letterbox bars.
