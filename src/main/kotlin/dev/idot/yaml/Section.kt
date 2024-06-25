package dev.idot.yaml

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.IOException

open class Section(val parent: Section? = null, val path: String = "") : Iterable<Map.Entry<String, Any>> {

    var sectionMap: MutableMap<String, Any> = LinkedHashMap()
        private set

    var options: Options = Options()

    /**
     * Gets the root [Configuration] that contains this [ ]
     *
     * For any [Configuration] themselves, this will return its own object.
     *
     * @return Root configuration containing this section.
     */
    var root: Section = parent?.root ?: this
        private set

    /**
     * Gets the path of this [Section] from its root [ ]
     *
     *
     * For any [Configuration] themselves, this will return an empty
     * string.
     *
     *
     * If the section is no longer contained within its root for any reason,
     * such as being replaced with a different value, this may return null.
     *
     *
     * To retrieve the single name of this section, that is, the final part of
     * the path returned by this method, you may use [.getName].
     *
     * @return Path of this section relative to its root
     */
    var fullPath: String = parent?.createPath(path) ?: ""
        private set

    override fun toString(): String = "Section(path='$path', fullPath='$fullPath')"
    override fun iterator(): Iterator<Map.Entry<String, Any>> = sectionMap.entries.iterator()

    companion object {
        val dumperOptions = DumperOptions()
        val yaml: Yaml = Yaml(Representer(dumperOptions))

        /**
         * Creates an empty [Section] at the specified path.
         *
         * Any value that was previously set at this path will be overwritten. If
         * the previous value was itself a [Section], it will be orphaned.
         *
         * @param path Path to create the section at.
         * @return Newly created section
         */
        fun Section.new(path: String): Section {
            if (path.isEmpty()) throw IllegalArgumentException("Cannot create section at empty path")

            var leading = path.indexOf(root.options.pathSeparator)
            var trailing = 0
            var newSection: Section = this

            while (leading != -1) {
                newSection = path.substring(trailing, leading).run {
                    newSection.of(this) ?: newSection.new(this)
                }
                trailing = leading + 1
                leading = path.indexOf(root.options.pathSeparator, trailing)
            }
            path.substring(trailing).also { s ->
                if (newSection === this) {
                    return Section(this, s).also { sectionMap[s] = it }
                }
                return newSection.new(s)
            }
        }

        /**
         * Creates a [Section] at the specified path, with specified values.
         *
         * Any value that was previously set at this path will be overwritten. If
         * the previous value was itself a [Section], it will be orphaned.
         *
         * @param path Path to create the section at.
         * @param map The values to used.
         * @return Newly created section
         */
        fun Section.new(path: String, map: Map<*, *>): Section {
            new(path).also {
                for ((key, value) in map) {
                    if (value is Map<*, *>) it.new(key.toString(), value)
                    else it[key.toString()] = value
                }
                return it
            }
        }

        /**
         * Gets the requested Section by path.
         *
         * If the Section does not exist but a default value has been specified,
         * this will return the default value. If the Section does not
         * exist and no default value was specified, this will return null.
         *
         * @param path Path of the Section to get.
         * @return Requested Section.
         */
        fun Section.of(path: String): Section? {
            this[path]?.let {
                return if (it is Section) it else null
            }

            get(path, defaults(path)).apply {
                return if (this is Section) this else null
            }
        }

        /**
         * Gets a set containing all keys in this section.
         *
         * If deep is set to true, then this will contain all the keys within any
         * child [Section]s (and their children, etc.). These will be in a valid
         * path notation for you to use.
         *
         * If deep is set to false, then this will contain only the keys of any
         * direct children, and not their own children.
         *
         * @param deep Whether to get a deep list, as opposed to a shallow list.
         * @return Set of keys contained within this Section.
         */
        fun Section.getKeys(deep: Boolean): Set<String> = LinkedHashSet<String>().apply {
            if (root.options.copyDefaults) defaults()?.run { addAll(getKeys(deep)) }
            mapChildrenKeys(this, this@getKeys, deep)
        }

        /**
         * Gets a Map containing all keys and their values for this section.
         *
         * If deep is set to true, then this will contain all the keys and values
         * within any child [Section]s (and their children, etc.). These keys will
         * be in a valid path notation for you to use.
         *
         * If deep is set to false, then this will contain only the keys and values
         * of any direct children, and not their own children.
         *
         * @param deep Whether to get a deep list, as opposed to a shallow list.
         * @return Map of keys and values of this section.
         */
        fun Section.getValues(deep: Boolean): Map<String?, Any?> = LinkedHashMap<String?, Any?>().apply {
            if (root.options.copyDefaults) defaults()?.run { putAll(getValues(deep)) }
            mapChildrenValues(this, this@getValues, deep)
        }

        /**
         * Creates a full path to the given [Section] from its root [Configuration].
         *
         * @param key Name of the specified section.
         * @return Full path of the section from its root.
         */
        fun Section.createPath(key: String?): String = createPath(key, this.root)

        /**
         * Creates a relative path to the given [Section] from the given relative
         * section.
         *
         * @param key Name of the specified section.
         * @param relativeTo Section to create the path relative to.
         * @return Full path of the section from its root.
         */
        fun Section.createPath(key: String?, relativeTo: Section?): String {
            val separator = this.root.options.pathSeparator
            return buildString {
                var parent: Section? = this@createPath
                while (parent != null && parent !== relativeTo) {
                    if (isNotEmpty()) insert(0, separator)
                    insert(0, parent.path)
                    parent = parent.parent
                }
                if (!key.isNullOrEmpty()) {
                    if (isNotEmpty()) append(separator)
                    append(key)
                }
            }
        }

        /**
         * Checks if this [Section] contains the given path.
         *
         * If the value for the requested path does not exist but a default value
         * has been specified, this will return true.
         *
         * @param path Path to check for existence.
         * @return True if this section contains the requested path, either via
         *     default or being set.
         * @throws IllegalArgumentException Thrown when path is null.
         */
        fun Section.contains(path: String): Boolean = this.contains(path, false)

        /**
         * Checks if this [Section] contains the given path.
         *
         * If the value for the requested path does not exist, the boolean
         * parameter of true has been specified, a default value for the path
         * exists, this will return true.
         *
         * If a boolean parameter of false has been specified, true will only be
         * returned if there is a set value for the specified path.
         *
         * @param path Path to check for existence.
         * @param ignoreDefault Whether to ignore if a default value for the
         *     specified path exists.
         * @return True if this section contains the requested path, or if a
         *     default value exist and the boolean parameter for this method is
         *     true.
         * @throws IllegalArgumentException Thrown when path is null.
         */
        fun Section.contains(path: String, ignoreDefault: Boolean): Boolean =
            (if (ignoreDefault) this[path, null] else this[path]) != null

        /**
         * Checks if this [Section] has a value set for the given path.
         *
         * If the value for the requested path does not exist but a default value
         * has been specified, this will still return false.
         *
         * @param path Path to check for existence.
         * @return True if this section contains the requested path, regardless of
         *     having a default.
         * @throws IllegalArgumentException Thrown when path is null.
         */
        fun Section.isPathSet(path: String): Boolean =
            if (root.options.copyDefaults) contains(path) else get(path, null) != null

        /**
         * Gets the requested Object by path.
         *
         * If the Object does not exist but a default value has been specified,
         * this will return the default value. If the Object does not exist
         * and no default value was specified, this will return null.
         *
         * @param path Path of the Object to get.
         * @return Requested Object.
         */
        operator fun Section.get(path: String): Any? {
            path.split(root.options.pathSeparator, limit = 2).also {
                return if (it.size > 1) (this.sectionMap[it[0]] as Section)[it[1]] else this.sectionMap[path]
            }
        }

        /**
         * Gets the requested Object by path, returning a default value if not
         * found.
         *
         * If the Object does not exist then the specified default value will
         * return regardless of if a default has been identified in the root
         * [Configuration].
         *
         * @param path Path of the Object to get.
         * @param default The default value to return if the path is not found.
         * @return Requested Object.
         */
        operator fun Section.get(path: String, default: Any?): Any? {
            if (path.isEmpty()) return this
    
            val segments = path.split(root.options.pathSeparator)
            var currentSection: Section? = this
    
            for (i in 0..<segments.lastIndex) {
                currentSection = currentSection?.of(segments[i]) ?: return default
            }
    
            return currentSection?.sectionMap?.get(segments.last()) ?: default
        }

        /**
         * Sets the specified path to the given value.
         *
         * If value is null, the entry will be removed. Any existing entry will be
         * replaced, regardless of what the new value is.
         *
         * Some implementations may have limitations on what you may store. See
         * their individual javadocs for details. No implementations should allow
         * you to store [Configuration]s or [Section]s, please use [.createSection] for
         * that.
         *
         * @param path Path of the object to set.
         * @param value New value to set the path to.
         */
        operator fun Section.set(path: String, value: Any?) {
            var currentSection: Section = this
            val segments = path.split(this.root.options.pathSeparator)
            for (i in 0..<segments.size - 1) {
                currentSection = currentSection.of(segments[i])
                    ?: if (value == null) return else currentSection.new(segments[i])
            }
    
            segments.last().also {
                if (value == null) currentSection.sectionMap.remove(it) else currentSection.sectionMap[it] = value
            }
        }

        /**
         * Gets the requested String by path.
         *
         * If the String does not exist but a default value has been specified,
         * this will return the default value. If the String does not exist
         * and no default value was specified, this will return null.
         *
         * @param path Path of the String to get.
         * @return Requested String.
         */
        fun Section.string(path: String): String? = string(path, defaults(path)?.toString())

        /**
         * Gets the requested String by path, returning a default value if not
         * found.
         *
         * If the String does not exist then the specified default value will
         * return regardless of if a default has been identified in the root
         * [Configuration].
         *
         * @param path Path of the String to get.
         * @param def The default value to return if the path is not found or is
         *     not a String.
         * @return Requested String.
         */
        fun Section.string(path: String, def: String?): String? = this[path, def]?.toString() ?: def

        /**
         * Gets the requested int by path.
         *
         * If the int does not exist but a default value has been specified, this
         * will return the default value. If the int does not exist and no default
         * value was specified, this will return 0.
         *
         * @param path Path of the int to get.
         * @return Requested int.
         */
        fun Section.int(path: String): Int = defaults(path).run { if (this is Number) this.toInt() else 0 }

        /**
         * Gets the requested int by path, returning a default value if not found.
         *
         * If the int does not exist then the specified default value will return
         * regardless of if a default has been identified in the root [Configuration].
         *
         * @param path Path of the int to get.
         * @param def The default value to return if the path is not found or is
         *     not an int.
         * @return Requested int.
         */
        fun Section.int(path: String, def: Int): Int = get(path, def).run { if (this is Number) this.toInt() else def }

        /**
         * Gets the requested boolean by path.
         *
         * If the boolean does not exist but a default value has been specified,
         * this will return the default value. If the boolean does not exist
         * and no default value was specified, this will return false.
         *
         * @param path Path of the boolean to get.
         * @return Requested boolean.
         */
        fun Section.boolean(path: String): Boolean = defaults(path).run { if (this is Boolean) this else false }

        /**
         * Gets the requested boolean by path, returning a default value if not
         * found.
         *
         * If the boolean does not exist then the specified default value will
         * return regardless of if a default has been identified in the root
         * [Configuration].
         *
         * @param path Path of the boolean to get.
         * @param def The default value to return if the path is not found or is
         *     not a boolean.
         * @return Requested boolean.
         */
        fun Section.boolean(path: String, def: Boolean): Boolean = get(path, def).run { if (this is Boolean) this else def }

        /**
         * Gets the requested double by path.
         *
         * If the double does not exist but a default value has been specified,
         * this will return the default value. If the double does not exist and no
         * default value was specified, this will return 0.
         *
         * @param path Path of the double to get.
         * @return Requested double.
         */
        fun Section.double(path: String): Double = defaults(path).run { if (this is Number) this.toDouble() else 0.0 }

        /**
         * Gets the requested double by path, returning a default value if not
         * found.
         *
         * If the double does not exist then the specified default value will
         * return regardless of if a default has been identified in the root
         * [Configuration].
         *
         * @param path Path of the double to get.
         * @param def The default value to return if the path is not found or is
         *     not a double.
         * @return Requested double.
         */
        fun Section.double(path: String, def: Double): Double =
            get(path, def).run { if (this is Number) this.toDouble() else def }

        /**
         * Gets the requested long by path.
         *
         * If the long does not exist but a default value has been specified, this
         * will return the default value. If the long does not exist and no default
         * value was specified, this will return 0.
         *
         * @param path Path of the long to get.
         * @return Requested long.
         */
        fun Section.long(path: String): Long = defaults(path).run { if (this is Number) this.toLong() else 0L }

        /**
         * Gets the requested long by path, returning a default value if not found.
         *
         * If the long does not exist then the specified default value will return
         * regardless of if a default has been identified in the root [Configuration].
         *
         * @param path Path of the long to get.
         * @param def The default value to return if the path is not found or is
         *     not a long.
         * @return Requested long.
         */
        fun Section.long(path: String, def: Long): Long =
            get(path, def).run { if (this is Number) this.toLong() else def }

        /**
         * Checks if the specified path is a long.
         *
         * If the path exists but is not a long, this will return false. If the
         * path does not exist, this will return false. If the path does not exist
         * but a default value has been specified, this will check if that default
         * value is a long and return appropriately.
         *
         * @param path Path of the long to check.
         * @return Whether the specified path is a long.
         */
        fun Section.isLong(path: String): Boolean = this[path] is Number

        /**
         * Gets the requested List by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return null.
         *
         * @param path Path of the List to get.
         * @return Requested List.
         */
        fun Section.list(path: String): List<*>? = list(path, defaults(path) as List<*>?)

        /**
         * Gets the requested List by path, returning a default value if not found.
         *
         * If the List does not exist then the specified default value will return
         * regardless of if a default has been identified in the root [Configuration].
         *
         * @param path Path of the List to get.
         * @param def The default value to return if the path is not found or is
         *     not a List.
         * @return Requested List.
         */
        fun Section.list(path: String, def: List<*>?): List<*>? = get(path, def) as List<*>?

        /**
         * Gets the requested List of String by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a String if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of String.
         */
        fun Section.stringList(path: String): List<String> = ArrayList<String>().apply {
            for (obj in list(path) ?: return this) {
                if (obj is String || isPrimitiveWrapper(obj!!)) add(obj.toString())
            }
        }

        /**
         * Gets the requested List of Integer by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into an Integer if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Integer.
         */
        fun Section.intList(path: String): List<Int> = ArrayList<Int>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Int -> add(obj)
                    is String -> add(runCatching { obj.toInt() }.getOrDefault(0))
                    is Char -> add(obj.code)
                    is Number -> add(obj.toInt())
                }
            }
        }

        /**
         * Gets the requested List of Boolean by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Boolean if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Boolean.
         */
        fun Section.booleanList(path: String): List<Boolean> = ArrayList<Boolean>().apply {
            for (obj in list(path) ?: return ArrayList(0)) {
                when (obj) {
                    is Boolean -> add(obj)
                    is String -> add(obj.toBoolean())
                }
            }
        }

        /**
         * Gets the requested List of Double by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Double if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Double.
         */
        fun Section.doubleList(path: String): List<Double> = ArrayList<Double>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Double -> add(obj)
                    is String -> add(runCatching { obj.toDouble() }.getOrDefault(0.0))
                    is Char -> add(obj.code.toDouble())
                    is Number -> add(obj.toDouble())
                }
            }
        }

        /**
         * Gets the requested List of Float by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Float if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Float.
         */
        fun Section.floatList(path: String): List<Float> = ArrayList<Float>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Float -> add(obj)
                    is String -> add(runCatching { obj.toFloat() }.getOrDefault(0.0f))
                    is Char -> add(obj.code.toFloat())
                    is Number -> add(obj.toFloat())
                }
            }
        }

        /**
         * Gets the requested List of Long by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Long if possible, but
         * may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Long.
         */
        fun Section.longList(path: String): List<Long> = ArrayList<Long>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Long -> add(obj)
                    is String -> add(runCatching { obj.toLong() }.getOrDefault(0L))
                    is Char -> add(obj.code.toLong())
                    is Number -> add(obj.toLong())
                }
            }
        }

        /**
         * Gets the requested List of Byte by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Byte if possible, but
         * may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Byte.
         */
        fun Section.byteList(path: String): List<Byte> = ArrayList<Byte>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Byte -> add(obj)
                    is String -> add(runCatching { obj.toByte() }.getOrDefault(0))
                    is Char -> add(obj.code.toByte())
                    is Number -> add(obj.toByte())
                }
            }
        }

        /**
         * Gets the requested List of Character by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Character if
         * possible, but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Character.
         */
        fun Section.charList(path: String): List<Char> = ArrayList<Char>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Char -> add(obj)
                    is String -> add(obj[0])
                    is Number -> add(obj.toInt().toChar())
                }
            }
        }

        /**
         * Gets the requested List of Short by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Short if possible,
         * but may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Short.
         */
        fun Section.shortList(path: String): List<Short> = ArrayList<Short>().apply {
            for (obj in list(path) ?: return this) {
                when (obj) {
                    is Short -> add(obj)
                    is String -> add(runCatching { obj.toShort() }.getOrDefault(0))
                    is Char -> add(obj.code.toShort())
                    is Number -> add(obj.toShort())
                }
            }
        }

        /**
         * Gets the requested List of Maps by path.
         *
         * If the List does not exist but a default value has been specified, this
         * will return the default value. If the List does not exist and no default
         * value was specified, this will return an empty List.
         *
         * This method will attempt to cast any values into a Map if possible, but
         * may miss any values out if they are not compatible.
         *
         * @param path Path of the List to get.
         * @return Requested List of Maps.
         */
        fun Section.mapList(path: String): List<Map<*, *>?> = ArrayList<Map<*, *>?>().apply {
            for (obj in list(path) ?: return this) {
                if (obj is Map<*, *>) add(obj)
            }
        }

        /**
         * Gets the requested object at the given path.
         *
         * If the Object does not exist but a default value has been specified,
         * this will return the default value. If the Object does not exist
         * and no default value was specified, this will return null.
         *
         * **Note:** For example #getObject(path, [String]) is
         * **not** equivalent to [#getString(path)][.getString] because
         * [#getString(path)][.getString] converts internally all Objects to
         * Strings. However, #getObject(path, [Boolean]) is equivalent to
         * [#getBoolean(path)][.getBoolean] for example.
         *
         * @param path the path to the object.
         * @param clazz the type of the requested object
         * @param <T> the type of the requested object
         * @return Requested object </T>
         */
        fun <T : Any?> Section.obj(path: String, clazz: Class<T>): T? = defaults(path).run {
            obj(path, clazz, if (this != null && clazz.isInstance(this)) clazz.cast(this) else null)
        }

        /**
         * Gets the requested object at the given path, returning a default value
         * if not found
         *
         * If the Object does not exist then the specified default value will
         * return regardless of if a default has been identified in the root
         * [Configuration].
         *
         * **Note:** For example #getObject(path, [String], def) is **not**
         * equivalent to [#getString(path, def)][.getString] because
         * [#getString(path, def)][.getString] converts internally all Objects to
         * Strings. However, #getObject(path, [Boolean], def) is equivalent to
         * [#getBoolean(path,][.getBoolean] for example.
         *
         * @param path the path to the object.
         * @param clazz the type of the requested object
         * @param def the default object to return if the object is not present at
         *     the path
         * @param <T> the type of the requested object
         * @return Requested object </T>
         */
        fun <T : Any?> Section.obj(path: String, clazz: Class<T>, def: T?): T? {
            return get(path, def).run {
                if (this != null && clazz.isInstance(this)) return clazz.cast(this) else def
            }
        }

        /**
         * Checks if the specified path is a Section.
         *
         * If the path exists but is not a Section, this will return false. If the
         * path does not exist, this will return false. If the path does not exist
         * but a default value has been specified, this will check if that default
         * value is a Section and return appropriately.
         *
         * @param path Path of the Section to check.
         * @return Whether the specified path is a Section.
         */
        fun Section.validate(path: String): Boolean = this[path] is Section


        /**
         * Retrieves an equivalent section from the root configuration based on this section's full path.
         * Returns null if:
         * - The root contains no defaults.
         * - The defaults do not contain a value for this path.
         * - The value at this path is not a section.
         *
         * @return Equivalent section in root configuration or null if conditions are not met.
         */
        fun Section.defaults(): Section? {
            if (root === this) return null
            val rootDefault = root.defaults() ?: return null
            return if (rootDefault.validate(fullPath)) rootDefault.of(fullPath) else null
        }

        fun Section.defaults(path: String) = root.defaults()?.get(createPath(path))

        /**
         * Sets the default value in the root at the given path as provided.
         *
         * If value is null, the value will be removed from the default
         * Configuration source.
         *
         * If the value as returned by [.getDefaultSection] is null, then this
         * will create a new section at the path, replacing anything that may have
         * existed there previously.
         *
         * @param path Path of the value to set.
         * @param value Value to set the default to.
         * @throws IllegalArgumentException Thrown if path is null.
         */
        fun Section.defaults(path: String, value: Any?) {
            if (root === this) throw UnsupportedOperationException("Unsupported createDefaultSection(String, Object) implementation")
            root.defaults(createPath(path), value)
        }

        /**
         * Serializes the configuration to a YAML formatted string with an optional header.
         *
         * Configures YAML output options for indentation and flow style, then concatenates a formatted header
         * (if present) with the serialized configuration entries. An empty configuration results in an empty
         * string. The header, when defined, is prefixed to the YAML content with appropriate separation.
         *
         * @return YAML formatted string of the configuration, prefixed by a header comment if defined, or an
         *     empty string for an empty configuration.
         */
        fun Section.serialize(): String {
            dumperOptions.apply {
                indent = options.indent
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            }
            return (buildHeader(this).takeIf { it.isNotEmpty() }?.plus("\n") ?: "") +
                    yaml.dump(getValues(false))?.let { if (it == Configuration.BLANK_CONFIG) "" else it }
        }

        /**
         * Deserializes the given YAML string, extracting a header if present.
         *
         * Attempts to parse the input string as a YAML document into a map structure. If successful, extracts
         * and sets the configuration header from the contents, then converts the map into configuration
         * sections. Throws `InvalidConfigurationException` for parsing errors or if the top-level structure is
         * not a map.
         *
         * @param contents YAML formatted string to be loaded into the configuration.
         */
        fun Section.deserialize(contents: String) {
            yaml.load<Map<String, Any>>(contents)?.let { input ->
                options.header = buildString {
                    var foundHeader = false
                    contents.lines().forEach { line ->
                        when {
                            line.startsWith(Configuration.COMMENT_PREFIX) -> {
                                if (isNotEmpty()) append("\n")
                                append(line.removePrefix(Configuration.COMMENT_PREFIX).trim())
                                foundHeader = true
                            }
                            // Breaks as soon as a non-comment line is encountered after finding the header,
                            // correctly not adding additional newlines for non-header content.
                            foundHeader -> return@forEach
                        }
                    }
                }.takeIf { it.isNotEmpty() }
                System.out.flush()
                input.toSections(this)
            } ?: emptyMap<String, Any>()
        }

        fun Section.buildHeader(defaults: Section?): String {
            val header = options.header ?: buildString {
                defaults?.getValues(false)?.forEach { (key, value) ->
                    if (value !is Section) {
                        append("$key: $value\n")
                    }
                }
            }.trim()
            return if (header.isEmpty()) "" else header.split("\n").joinToString("\n${Configuration.COMMENT_PREFIX}",
                Configuration.COMMENT_PREFIX
            )
        }

        @Throws(IOException::class, InvalidConfigurationException::class, ClassCastException::class)
        fun Map<*, *>.toSections(section: Section) {
            for ((key, value) in this) {
                if (value is Map<*, *>) value.toSections(section.new(key.toString()))
                else section[key.toString()] = value
            }
        }

        private fun Section.mapChildrenKeys(output: MutableSet<String>, section: Section, deep: Boolean) {
            for ((key, value) in section.sectionMap) {
                output.add(section.createPath(key, this))
                if (deep) mapChildrenKeys(output, value as Section, deep)
            }
        }

        private fun Section.mapChildrenValues(output: MutableMap<String?, Any?>, section: Section, deep: Boolean) {
            for ((key, value) in section.sectionMap) {
                section.createPath(key, this).apply {
                    output.remove(this)
                    output[this] = value
                }
                if (deep) mapChildrenValues(output, value as Section, deep)
            }
        }
    }
}

private fun isPrimitiveWrapper(input: Any): Boolean =
    input is Int || input is Boolean ||
            input is Char || input is Byte ||
            input is Short || input is Double ||
            input is Long || input is Float