# VillagerDupe X

A client-side Fabric mod for Minecraft Java Edition **26.2**. It automates normal client interactions with nearby villagers holding supported items. It is intended for controlled testing; it cannot force a server to accept an interaction or guarantee duplication.

## Compatibility

- Minecraft: **26.2 only**
- Fabric Loader: 0.19.5+
- Fabric API: 0.156.0+26.2
- Java: 25+

This is not compatible with Minecraft 1.21.x or other 26.x versions without a separate port.

## Concept and behavior

1. Select an emerald in the main hand (the selected hotbar slot must contain an emerald).
2. Keep shears in another hotbar slot.
3. Press `G` to enable repeating mode.
4. Each batch finds all client-tracked villagers inside the player's entity-interaction range whose main-hand item matches the active filter.
5. The mod selects the shears slot, interacts with each eligible villager through Minecraft's normal game-mode interaction API, and explicitly restores/synchronizes the emerald slot in a `finally` block.
6. Repeating mode continues at the selected interval until toggled off or a required item is no longer available.

The mod never moves items between inventory slots, never replaces the offhand, and never silently chooses a different main-hand item. It does not directly alter server state. A sent interaction is not proof that a villager was sheared or that a duplication method worked.

## Controls

Change bindings under **Options → Controls → Key Binds → VillagerDupe X**.

| Default key | Action |
| --- | --- |
| `G` | Toggle repeating batches |
| `H` | Run one batch |
| `J` | Cycle filter: all → saddle → carpet → leather horse armor |
| `K` | Cycle interval: 50 → 100 → 250 → 500 ms |

The default interval is 100 ms. The client tick rate imposes a practical limit; an interval shorter than a tick does not create extra ticks. Multiple eligible villagers can be interacted with during a single batch. Turning repeating mode off reports attempted interactions and eligible candidates observed; these are client-side counts, not proof of successful shearing or duplication.

## Build from source

Requirements: JDK 25 and internet access for Gradle/Fabric dependencies.

### Windows

1. Extract the archive.
2. Open the directory containing `settings.gradle` in IntelliJ IDEA.
3. Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** to JDK 25.
4. Run in PowerShell:

   ```powershell
   .\gradlew.bat clean build
   ```

### Linux/macOS

```bash
./gradlew clean build
```

The JARs are written to `build/libs/`. Install the plain `villagerdupex-1.0.2.jar` in a Fabric 26.2 instance with the matching Fabric API installed.

## Troubleshooting

### Gradle cannot resolve `net.fabricmc:yarn`
Minecraft 26.2 uses official unobfuscated names in this project. There is intentionally no `yarn_mappings` property or Yarn mappings dependency.

### Gradle uses Java 21
Set both the project SDK and Gradle JVM to JDK 25, then reload Gradle.

### It does not interact with a villager
Check that the emerald is selected in the main hand, shears are in the hotbar, the villager is within entity-interaction range, and the villager is holding a saddle, wool carpet, or leather horse armor in its main hand. Server-side behavior and patches may reject interactions.

### Report a bug
Include Minecraft, Fabric Loader/API, Java versions, the full Gradle error, and `latest.log`. Remove tokens or other private information before sharing logs.

## Responsible testing

Use only worlds and servers where you have permission to test. Multiplayer servers may prohibit exploits or automate interactions. Keep backups of test worlds. No duplication outcome or public-release build status is implied until a clean build and in-game smoke test have been completed.

## License

MIT. See `LICENSE`.
