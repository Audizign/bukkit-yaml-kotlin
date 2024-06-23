import dev.idot.yaml.Configuration
import dev.idot.yaml.Configuration.Companion.load
import dev.idot.yaml.Configuration.Companion.save
import dev.idot.yaml.Section
import dev.idot.yaml.Section.Companion.buildHeader
import dev.idot.yaml.Section.Companion.get
import dev.idot.yaml.Section.Companion.getValues
import dev.idot.yaml.Section.Companion.new
import dev.idot.yaml.Section.Companion.of
import dev.idot.yaml.Section.Companion.set
import dev.idot.yaml.Section.Companion.string
import java.io.File
import kotlin.test.*
import kotlin.test.Test

class Sanity {
    private val configFile = File("./src/test/resources/config.yml")
    private var config: Configuration = Configuration(configFile).also { it.load() }

    @BeforeTest
    fun setup() {
        //configFile.writeText("")
    }

    @Test
    fun emptySection() {
        config.new("empty-section")
        assertEquals(config.of("empty-section")?.getValues(false)?.size, 0)
    }

    @Test
    fun singleSection() {
        config["key"] = "value"
        config.save()
        assertEquals(config["key"], "value")
    }

    @Test
    fun nestedSection1() {
        config.new("section-1")["key"] = "nested"
        config.save()
        assertEquals(config["section-1.key"], "nested")
    }

    @Test
    fun nestedSection2() {
        config["section-2.key"] = "tested"
        config.save()
        assertEquals(config["section-2.key"], "tested")
    }

    @Test
    fun nestedSections() {
        val path = "parent.child.key"
        val value = "nested value"
        config.new("parent")
        config.new("parent.child")
        config[path] = value
        config.save()

        assertEquals(config.string(path), value, "Direct path failed")

        var lastSection: Section = config.root
        path.split('.').apply {
            dropLast(1).forEach {
                lastSection = lastSection.of(it) ?: throw NullPointerException("Section not found")
            }
            assertEquals(lastSection.string(last()), value, "Section path failed")
        }
    }

    @Test
    fun overwriteSection() {
        config["overwrite.key"] = "value"
        config["overwrite.key"] = "new value"
        assertEquals(config["overwrite.key"], "new value")
        config.save()
    }

    @Test
    fun headerGeneration() {
        val defaults = Section(path = "defaults")
        defaults["key1"] = "value1"
        defaults["key2"] = "value2"

        config.options.header = null
        val header = config.buildHeader(defaults)

        assertTrue(header.contains("# key1: value1"))
        assertTrue(header.contains("# key2: value2"))
        config.save()
    }

    @Test
    fun removeKey() {
        config["remove.key"] = "to be removed"
        config["remove.key"] = null
        assertNull(config["remove.key"], "Key was not removed")
    }

    @Test
    fun retrieveSection() {
        config.new("section")
        val section = config.of("section")
        assertNotNull(section)
    }

    @Test
    fun optionPathSeparator() {
        config.options.pathSeparator = '/'
        config["delimiter/key"] = "value"
        config.save()
        config.options.pathSeparator = '.'
        assertEquals(config["delimiter.key"], "value")
    }

    @Test
    fun defaultValue() {
        assertEquals(config["nonexistent.key", "default"], "default")
    }
}
