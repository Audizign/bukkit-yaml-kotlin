package dev.idot.yaml

import dev.idot.yaml.Section.Companion.getValues
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer as YamlRepresenter

class Representer(options: DumperOptions) : YamlRepresenter(options) {
    init {
        this.representers[Section::class.java] = RepresentSection()
    }

    private inner

    class RepresentSection : Represent {
        override fun representData(data: Any?): Node {
            return representMapping(Tag.MAP, (data as Section).getValues(false), DumperOptions.FlowStyle.BLOCK)
        }
    }
}
