# StoryTeller

Lyra the Chronicler appears in a popup with her portrait, delivers a story page by page, and offers step-by-step guidance. Stories can be triggered on first join or from any plugin with a console command. Made for Paper 26.1.2 (Java 25).

> This project was written without being compiled against the Paper API. The first build may show a few small errors; send them back and they are quick to fix.

## 1. Build the jar

**Easiest (no setup): GitHub**
1. Create a new GitHub repository and upload everything in this folder (including the `.github` folder).
2. Open the **Actions** tab, then **Build plugin**, then **Run workflow**.
3. When it finishes, download the `StoryTeller` artifact. The jar is inside.

**Locally:** install JDK 25 and Maven, then run `mvn package`. The jar is `target/StoryTeller-1.0.0.jar`.

If Maven cannot find `26.1.2.build.74-stable`, open `pom.xml` and replace the `paper.version` value with `[26.1.2.build,26.1.3)`.

## 2. Install

Put the jar in `plugins/` on every backend server where Lyra should appear (lobby, survival, or both) and restart. Each server keeps its own `data.yml`, so set `triggers.first-join-story` per server (for example `welcome` on the lobby and a survival story on survival).

## 3. Her picture

In `config.yml`, under `narrator.portrait`:

- **Player skin by UUID**: `type: skin` and set `uuid`.
- **Player skin by texture value**: `type: skin` and paste the Base64 value into `texture` (copy the "Value" field from minecraft-heads.com). No Mojang lookup needed.
- **Your own drawn picture with ItemsAdder**: `type: itemsadder` and set `itemsadder-id: storyteller:lyra_portrait`.

ItemsAdder example (check the ItemsAdder docs if your version differs). Save as `plugins/ItemsAdder/contents/storyteller/configs/lyra.yml` and put your PNG in `contents/storyteller/textures/item/lyra_portrait.png`, then run `/iazip`:

```yaml
info:
  namespace: storyteller
items:
  lyra_portrait:
    display_name: "Lyra"
    resource:
      material: PAPER
      generate: true
      textures:
        - item/lyra_portrait.png
```

## 4. Writing stories

Edit `stories.yml`, then run `/story reload`. Each story has `pages`, optional `guide` steps and optional `rewards` (console commands run once per player). Keep each page under about 300 characters.

## 5. Triggering stories

Run this from the console or from any plugin that can run console commands (Quests, BetonQuest, DeluxeMenus, your own season plugin):

```
story send <player|all> <story_id>
```

If the player is offline she visits them on their next join. If they are logged out of AuthMe she waits until they log in.

## Commands

| Command | Who | What |
| --- | --- | --- |
| `/story` | everyone | List stories you have heard (click to replay) |
| `/story replay <id>` | everyone | Hear a story again (no new rewards) |
| `/story send <player\|all> <id>` | admin | Send a story |
| `/story preview <id>` | admin | Preview without saving progress |
| `/story reload` | admin | Reload config and stories |

Permissions: `storyteller.use` (default: everyone), `storyteller.admin` (default: op).

## Notes

- If a player closes the popup without pressing Finish, Lyra waits `reopen-cooldown-minutes` before returning.
- A later version can use Paper's Dialog API for a proper popup window instead of the chest menu.
