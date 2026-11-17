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

## Screen overlay (HUD) mode - picture + speech bubble
1. Copy the folder `itemsadder-pack/contents/guide` into `plugins/ItemsAdder/contents/`.
2. Replace `guide_girl.png` / `guide_bubble.png` (in `resourcepack/guide/textures/font/`) with YOUR OWN art.
   Do not use Supercell / Clash of Clans artwork - it is copyrighted.
3. Run `/iazip` (or `/iareload`) in game, reconnect so you get the new resource pack.
4. Tune `scale_ratio`/`y_position` in `configs/guide_hud.yml` and `hud.*` offsets in ServerGuide `config.yml`
   until the picture and bubble sit where you want. Set `hud.enabled: false` to use the 3D NPC instead.
Controls in HUD mode: chat buttons, or press F = next, Shift+F = skip.
