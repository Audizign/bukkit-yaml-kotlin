package dev.idot.yaml

import java.io.*

open class Configuration(private val file: File) : Section() {
    companion object {
        const val COMMENT_PREFIX = "# "
        const val BLANK_CONFIG = "{}\n"

        @Throws(IOException::class, InvalidConfigurationException::class)
        fun Configuration.save() = file.writer(Charsets.UTF_8).use { it.write(serialize()) }

        @Throws(FileNotFoundException::class, IOException::class, InvalidConfigurationException::class)
        fun Configuration.load() = deserialize(InputStreamReader(FileInputStream(file), Charsets.UTF_8).readText())
    }
}


