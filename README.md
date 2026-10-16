# VillagerDupe X

A client-side Fabric mod for Minecraft Java Edition **26.2**. It automates normal client interactions with nearby villagers holding supported items.

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

## Controls

Change bindings under **Options → Controls → Key Binds → VillagerDupe X**.

| Default key | Action |
| --- | --- |
| `G` | Toggle repeating batches |
| `H` | Run one batch |
| `J` | Cycle filter: all → saddle → carpet → leather horse armor |
| `K` | Cycle interval: 50 → 100 → 250 → 500 ms |

The default interval is 100 ms. The client tick rate imposes a practical limit; an interval shorter than a tick does not create extra ticks. Multiple eligible villagers can be interacted with during a single batch. Turning repeating mode off reports attempted interactions and eligible candidates observed.

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

## License

MIT. See `LICENSE`.
