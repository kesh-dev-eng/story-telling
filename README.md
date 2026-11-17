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
