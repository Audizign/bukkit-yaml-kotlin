# bukkit-yaml-kotlin
A module that's inspired by Bukkit's YAML code, in Kotlin. It's functionally the same.

It's definitely unfinished, but I haven't run into any issues (yet).
# Usage
How detailed does this need to be?
## Loading and Saving
### File
```
val configFile = File("./src/test/resources/config.yml")
val config = Configuration(configFile).also { it.load() }
config.save()
```
### Text
```
val configText = """
                your:
                  keys:
                    and: 'values'
                """.trim())
                
val config = Configuration.deserialize(configText)

config.serialize()
```
### Getting and Setting
Simply getting will return an object...
```
config["your.path.here"]
config.get["your.path.here"]
```
...or you may specify a primitive type...
```
config.string("your.string.path")
config.int("your.int.path")
```
...or a default value if it's null...
```
config.double("your.double.path", 4.20)
```
...or a list or primitives (returns empty if it doesn't exist)...
```
config.floatList("your.float.list.path")
```
### Sections
Similar to MemorySection in Bukkit, use Section#of(path: String)
```
val chat: Section = config.of("general.chat")
val chatFormat: String = chat.string("format")			// general.chat.format
val chatCooldown: Int = chat.int("cooldown-ticks")		// general.chat.cooldown-ticks
```
### Options
You can change them like this...
```
config.options.header = "# This is the start of the file #"
```
...or like this, if you really want to...
```
val section = config.of("")
section.options.apply {
	pathSeparator = '.'
	copyDefaults = false
	header = null
	copyHeader = true
}
```
# Permission
I don't really know which license to use, but do whatever you like with this. Maybe I'll look into it later. Use at your own risk and stuff lol.
