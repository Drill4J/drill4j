package com.epam.drill.plugins.coverage

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

fun MutableMap<String, ClassLoader>.getByteCodeOf(className: String) = this[className]?.url(className)?.readBytes()
fun ClassLoader.url(resourceName: String): URL {
    return this.getResource(resourceName) ?: throw NoSuchElementException(resourceName)
}

class ClassPath {
    val scannedUris = mutableSetOf<File>()
    val resources = mutableMapOf<ClassLoader, MutableSet<String>>()


    fun scanItPlease(classLoader: ClassLoader): MutableMap<String, ClassLoader> {
        for ((key, value) in getClassPathEntries(classLoader)) {
            scan(key, value)
        }
        val map = resources.map { (k, v) ->
            v.associate { it to k }
        }
        val mutableMapOf = mutableMapOf<String, ClassLoader>()
        map.forEach {
            mutableMapOf.putAll(it)
        }
        return mutableMapOf
    }


    @Throws(IOException::class)
    fun scan(file: File, classloader: ClassLoader) {
        if (scannedUris.add(file.canonicalFile)) {
            scanFrom(file, classloader)
        }
    }

    @Throws(IOException::class)
    private fun scanFrom(file: File, classloader: ClassLoader) {
        try {
            if (!file.exists()) {
                return
            }
        } catch (e: SecurityException) {

            return
        }

        if (file.isDirectory) {
            scanDirectory(classloader, file)
        } else {
            scanJar(file, classloader)
        }
    }

    @Throws(IOException::class)
    private fun scanJar(file: File, classloader: ClassLoader) {
        val jarFile: JarFile
        try {
            jarFile = JarFile(file)
        } catch (e: IOException) {
            // Not a jar file
            return
        }

        try {
            for (path in getClassPathFromManifest(file, jarFile.manifest)) {
                scan(path, classloader)
            }
            scanJarFile(classloader, jarFile)
        } finally {
            try {
                jarFile.close()
            } catch (ignored: IOException) {
            }

        }
    }

    @Throws(MalformedURLException::class)
    fun getClassPathEntry(jarFile: File, path: String): URL {
        return URL(jarFile.toURI().toURL(), path)
    }

    private fun getClassPathFromManifest(jarFile: File, manifest: Manifest?): Set<File> {
        if (manifest == null) {
            return setOf()
        }
        val builder = mutableSetOf<File>()
        val classpathAttribute = manifest.mainAttributes.getValue(Attributes.Name.CLASS_PATH.toString())
        if (classpathAttribute != null) {
            for (path in classpathAttribute.split(" ")) {
                val url: URL
                try {
                    url = getClassPathEntry(jarFile, path)
                } catch (e: MalformedURLException) {
                    continue
                }

                if (url.protocol == "file") {
                    builder.add(toFile(url))
                }
            }
        }
        return builder
    }

    fun getClassPathEntries(classloader: ClassLoader): MutableMap<File, ClassLoader> {
        val entries = mutableMapOf<File, ClassLoader>()
        val parent = classloader.parent
        if (parent != null) {
            entries.putAll(getClassPathEntries(parent))
        }
        for (url in getClassLoaderUrls(classloader)) {
            if (url.protocol == "file") {
                val file = toFile(url)
                if (!entries.containsKey(file)) {
                    entries[file] = classloader
                }
            }
        }
        return entries
    }

    private fun toFile(url: URL): File {
        return try {
            File(url.toURI()) // Accepts escaped characters like %20.
        } catch (e: URISyntaxException) { // URL.toURI() doesn't escape chars.
            File(url.path) // Accepts non-escaped chars like space.
        }

    }

    private fun getClassLoaderUrls(classloader: ClassLoader): List<URL> {
        if (classloader is URLClassLoader) {
            return classloader.urLs.toList()
        }
        return if (classloader == ClassLoader.getSystemClassLoader()) {
            parseJavaClassPath()
        } else listOf()
    }

    private fun parseJavaClassPath(): List<URL> {
        val urls = mutableListOf<URL>()
        for (entry in System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
            try {
                try {
                    urls.add(File(entry).toURI().toURL())
                } catch (e: SecurityException) { // File.toURI checks to see if the file is a directory
                    urls.add(URL("file", null, File(entry).absolutePath))
                }

            } catch (e: MalformedURLException) {
            }

        }
        return urls
    }


    private fun scanJarFile(classloader: ClassLoader, file: JarFile) {
        val entries = file.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory || entry.name == JarFile.MANIFEST_NAME) {
                continue
            }
            if (resources[classloader] == null) {
                resources[classloader] = mutableSetOf()
            }
            resources[classloader]?.add(entry.name)
        }
    }

    @Throws(IOException::class)
    protected fun scanDirectory(classloader: ClassLoader, directory: File) {
        val currentPath = HashSet<File>()
        currentPath.add(directory.canonicalFile)
        scanDirectory(directory, classloader, "", currentPath)
    }

    @Throws(IOException::class)
    private fun scanDirectory(
        directory: File, classloader: ClassLoader, packagePrefix: String, currentPath: MutableSet<File>
    ) {
        val files = directory.listFiles()
            ?: // IO error, just skip the directory
            return
        for (f in files) {
            val name = f.name
            if (f.isDirectory) {
                val deref = f.canonicalFile
                if (currentPath.add(deref)) {
                    scanDirectory(deref, classloader, "$packagePrefix$name/", currentPath)
                    currentPath.remove(deref)
                }
            } else {
                val resourceName = packagePrefix + name
                if (resourceName != JarFile.MANIFEST_NAME) {
                    if (resources[classloader] == null) {
                        resources[classloader] = mutableSetOf()
                    }
                    resources[classloader]?.add(resourceName)
                }
            }
        }
    }


}
