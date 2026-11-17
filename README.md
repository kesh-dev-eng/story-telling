# ServerGuide

Build: install JDK 25 + Gradle, then run `gradle build`. Jar: `build/libs/ServerGuide-1.0.0.jar`.
Put the jar in `plugins/`, restart, edit `plugins/ServerGuide/config.yml`, then `/guide reload`.

Commands: /guide, /guide next|back|skip, /guide reload, /guide reset <player>
Permissions: serverguide.use (default true), serverguide.admin (op)

ItemsAdder: set `character.type: ITEMSADDER` and `itemsadder-id` to your custom entity,
and use ItemsAdder font-image placeholders in `header-prefix` or any step text for a portrait / speech-bubble look.

## Build with GitHub (no install needed)
1. Upload all files of this project to a new GitHub repository (keep the `.github` folder).
2. Open the **Actions** tab -> "Build plugin jar" runs automatically (or press "Run workflow").
3. When it finishes, open the run and download **ServerGuide-jar** under *Artifacts*. Unzip it to get the `.jar`.

## Screen overlay (HUD) mode - character bottom-left + cloud bubble
1. Edit the steps in `config.yml`, then run `pip install pillow pyyaml` and `python tools/generate_bubbles.py`.
   This draws one cloud bubble image per step (text wrapped inside) and writes `guide_hud.yml`.
2. Copy `itemsadder-pack/contents/guide` into `plugins/ItemsAdder/contents/`, run `/iazip`, rejoin.
3. Your character picture is `resourcepack/guide/textures/font/guide_girl.png` (swap it for any transparent PNG).
4. Tuning: `hud.screen-width` (= screen width / GUI scale, 1920/4 = 480) moves the picture left/right,
   `y_position` in `guide_hud.yml` moves images up/down (then /iazip again).
Controls: chat buttons, or F = next, Shift+F = skip. `hud.enabled: false` = 3D NPC instead.

## Build with GitHub (no install needed)
1. Upload all files of this project to a new GitHub repository (keep the `.github` folder).
2. Open the **Actions** tab -> "Build plugin jar" runs automatically (or press "Run workflow").
3. When it finishes, open the run and download **ServerGuide-jar** under *Artifacts*. Unzip it to get the `.jar`.

## Screen overlay (HUD) mode - picture + speech bubble
1. Copy the folder `itemsadder-pack/contents/guide` into `plugins/ItemsAdder/contents/`.
2. Replace `guide_girl.png` / `guide_bubble.png` (in `resourcepack/guide/textures/font/`) with YOUR OWN art.
   Do not use Supercell / Clash of Clans artwork - it is copyrighted.
3. Run `/iazip` (or `/iareload`) in game, reconnect so you get the new resource pack.
4. Tune `scale_ratio`/`y_position` in `configs/guide_hud.yml` and `hud.gap` / `hud.text-back` in ServerGuide `config.yml`
   until the picture and bubble sit where you want. Set `hud.enabled: false` to use the 3D NPC instead.
Controls in HUD mode: chat buttons, or press F = next, Shift+F = skip.
