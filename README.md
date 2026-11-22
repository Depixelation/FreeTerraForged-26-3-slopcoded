This fork is a port of the FreeTerraForged 1.21.1_unstable branch from commit 16f68124d6de4f4654456008275efd344fc1c722 ported to 26.3 in Antigravity by Gemini Argon. The Fabric subproject for 26.3 compiles and runs, with overall generation and presets seemingly working. The mod has remained running without obvious issue on a Fabric 26.3 server for several days. However, no testing has been done beyond regular use by me in singleplayer and on the server, several non-breaking bugs are known to exist, and correctness and quality has not been human reviewed. The Neoforge subproject is not tested and presumed to be nonworking. This fork was made in the process of building a custom modpack for a personal server, and was not made with public use in mind. This repository is public only because I do not see any harm in making it so. No support nor further development is expected from me on this project.  

Compilation of this project requires manual placement of the following .jar files (exact) into `libs/`:
- `TerraBlender-fabric-26.3-26.3.0.0.9.jar` (TerraBlender 26.3.0.0.9)
- `biolith-fabric-3.8.0-beta.2.jar` (Biolith 3.8.0-beta.2)
- `lithostitched-2.0.4-fabric-26.3.jar` (Lithostitched 2.0.4)

If the devs of any of the upstream projects wish for me to remove this repository, please let me know via the issue tracker.

Original README follows below:  

# FreeTerraForged
A community driven fork of the Legendary ReTerraForged project for modern minecraft providing heavily customizable overworld terrain generation.
Additional feature contributions are welcomed via forking and raising a merge PR.

### We stand on the shoulders of giants
- Original project https://github.com/TerraForged/TerraForged
- Builds on the substantial post v1.19+ work of Racoonman2 in https://github.com/racoonman2/ReTerraForged
- Finishes the Neoforge port work started by Equalizer32 in https://github.com/equalizer32/NeoTerraForged/tree/1.21.1

### Licensing 
- Continued under the permissive MIT license as per all historic contributions, with the exception of `Erosion.java` which is licensed under the Mozilla Public License 2.0 (MPL-2.0). See [LICENSE](LICENSE) for details.

***

# Getting started

### Customize a new world
<img width="856" height="526" alt="image" src="https://github.com/user-attachments/assets/941bc4b1-9334-4c08-9197-9ff729876369" />

---

### Select a preset 
<img width="1579" height="743" alt="image" src="https://github.com/user-attachments/assets/fb9fc15b-dd2c-4970-bc11-b4d873fe6830" />

- Use template settings directly by clicking [Done]
- Customize the selected preset settings by clicking [Edit]
- Manage preset modifications using the right hand menu options.

---

### Customize your world
<img width="2345" height="1244" alt="image" src="https://github.com/user-attachments/assets/93e99f98-da19-4718-a283-3c9f5dee8362" />
<img width="2345" height="1244" alt="image" src="https://github.com/user-attachments/assets/3d033148-aa9c-44c0-bbdb-971aef581267" />

- There are many pages of settings accessible via paging through using [<<] and [>>]
- Click Done to save your edits.
- Click Cancel to abandon your edits.

---

## Biome replacement

FreeTerraForged creates and reads its biome replacement configuration at:

```text
config/freeterraforged/biome_replacements.properties
```

On first startup:

- if `config/biome_replacer.properties` exists, FTF atomically copies its rules into the native file
  and records that they came from a legacy import;
- otherwise, FTF creates a documented native template; and
- after the native file has been created, it is authoritative. Later changes to the legacy Biome
  Replacer file are not merged or copied over it.

The native format supports global rules, dimension sections, source-side biome tags, and explicit
removal:

```properties
format_version=1

minecraft:snowy_plains > minecraft:desert
#minecraft:is_forest > minecraft:cherry_grove
minecraft:deep_dark > null
```

`format_version` allows later FTF releases to identify the file syntax. Native rules are validated
strictly, and replacement always preserves the source biome's placement. Neither behavior is a
configurable setting. Files imported from Biome Replacer carry a fixed migration-source marker so
legacy rules that refer to absent content can be skipped safely.

Rules are atomic and one-hop. For example, with `a > b` and `b > c`, an original `a` becomes `b`
and an original `b` becomes `c`; FTF does not recursively chase the chain. Swaps are therefore
well-defined. Tag rules fill sources not already claimed in their scope, direct rules overwrite in
the same scope, and rules in the selected dimension overlay global rules.

Replacement changes only the final biome. The source biome's climate point, terrain, hydrology, and
placement envelope are preserved. Removal filters the biome from every FTF provider table. If a
later selection operation produces a removed biome, FTF falls back to that query's provider-base
biome and applies the fallback's replacement once. Configurations that would empty a required
provider/fallback table or remove from an unsupported direct custom source are rejected without
replacing the previous live worldgen plan.

These rules apply only to dimensions generated by FTF. In the standard FTF world type, that means
the Overworld. Use Biome Replacer's own `config/biome_replacer.properties` file for replacements in
the standard Nether, End, or other non-FTF dimensions. Dimension sections in the native file are
useful only for additional dimensions that have explicitly been configured to use FTF's generator.

Use Minecraft's resource reload or reopen the world after editing the file. A failed reload leaves
the previous immutable plan active. In the preset biome preview, replaced cells use the target
biome's color with a zoom-aware crosshatch. The biome legend identifies them as
`target (overriding source)` and wraps long IDs to the available width.

Biome Replacer remains available to non-FTF generators when both mods are installed. FTF does not
call its mutable resolver; FTF-owned plans use the native immutable rules and suppress only
Lithostitched injectors carrying explicit `biome_replacer` provenance. Biolith's captured range API
does not expose caller provenance, so FTF emits a warning rather than claiming source-proven
exact-once coexistence for an overlapping Biome Replacer + Biolith stack.

---

# Bugs
- Any issues encountered should be raised as Github issues with as much supporting documentation as you can provide, ideally latest.log and screenshots at a minimum.
