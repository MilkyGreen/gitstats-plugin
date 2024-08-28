package com.github.milkygreen.gitstatsplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.io.File
import java.util.*

@Service(Service.Level.PROJECT)
class MyProjectService(
     private val project: Project,
     val cs: CoroutineScope
) {

    data class DeveloperStats(
        val languages: Set<String>,
        val languageFileCounts: Map<String, Int>,
        val frameworks: Set<String>
    )

    data class Contributor(
        val name: String,
        val latestCommitDate: Date,
        val relativeDate: String,
        val commitCount: Int
    )

    private val extensionToLanguageMap = mapOf(
        "kt" to "Kotlin",
        "kts" to "Kotlin Script",
        "java" to "Java",
        "py" to "Python",
        "js" to "JavaScript",
        "ts" to "TypeScript",
        "jsx" to "JavaScript",
        "tsx" to "TypeScript",
        "rb" to "Ruby",
        "cpp" to "C++",
        "hpp" to "C++",
        "c" to "C",
        "h" to "C",
        "cs" to "C#",
        "php" to "PHP",
        "swift" to "Swift",
        "go" to "Go",
        "rs" to "Rust",
        "html" to "HTML",
        "css" to "CSS",
        "scss" to "Sass",
        "sass" to "Sass",
        "md" to "Markdown",
        "pl" to "Perl",
        "pm" to "Perl",
        "sh" to "Shell",
        "bash" to "Shell",
        "sql" to "SQL",
        "ps1" to "PowerShell",
        "lua" to "Lua",
        "elm" to "Elm",
        "ex" to "Elixir",
        "exs" to "Elixir",
        "erl" to "Erlang",
        "hrl" to "Erlang",
        "r" to "R",
        "f" to "Fortran",
        "f90" to "Fortran",
        "f95" to "Fortran",
        "jl" to "Julia",
        "groovy" to "Groovy",
        "gd" to "Godot (GDScript)",
        "nim" to "Nim",
        "hs" to "Haskell",
        "lhs" to "Haskell",
        "ml" to "OCaml",
        "mli" to "OCaml",
        "scala" to "Scala",
        "sc" to "Scala",
        "vb" to "Visual Basic",
        "vbs" to "VBScript",
        "dart" to "Dart",
        "clj" to "Clojure",
        "cljs" to "ClojureScript",
        "cljc" to "Clojure/ClojureScript",
        "coffee" to "CoffeeScript",
        "yml" to "YAML",
        "yaml" to "YAML",
        "json" to "JSON",
        "xml" to "XML"
    )

    private fun detectFrameworks(files: List<String>): Set<String> {
        val frameworks = mutableSetOf<String>()
        files.forEach { file ->
            when {
                file.contains("build.gradle.kts") || file.contains("build.gradle") -> frameworks.add("Gradle")
                file.contains("pom.xml") -> frameworks.add("Maven")
                file.contains("package.json") -> frameworks.add("Node.js")
                file.contains("Gemfile") -> frameworks.add("Ruby on Rails")
                file.contains("composer.json") -> frameworks.add("PHP")
                file.contains("pubspec.yaml") -> frameworks.add("Dart")
                file.contains("mix.exs") -> frameworks.add("Elixir")
                file.contains("Makefile") -> frameworks.add("Make")
                file.contains("build.sbt") -> frameworks.add("Scala (SBT)")
                file.contains("CMakeLists.txt") -> frameworks.add("CMake")
                file.contains("build.xml") -> frameworks.add("Ant")
                file.contains("webpack.config.js") -> frameworks.add("Webpack")
                file.contains("angular.json") -> frameworks.add("Angular")
                file.contains("vue.config.js") -> frameworks.add("Vue.js")
                file.contains("next.config.js") -> frameworks.add("Next.js")
                file.contains("nuxt.config.js") -> frameworks.add("Nuxt.js")
                file.contains("gatsby-config.js") -> frameworks.add("Gatsby")
                file.contains("server.js") || file.contains("app.js") -> frameworks.add("Express.js")
                file.contains("spring-boot-starter") -> frameworks.add("Spring Boot")
                file.contains("application.yml") -> frameworks.add("Spring Framework")
                file.contains("application.properties") -> frameworks.add("Spring Framework")
                file.contains("config.ru") -> frameworks.add("Rack")
                file.contains("Rakefile") -> frameworks.add("Rake")
                file.contains("Jenkinsfile") -> frameworks.add("Jenkins")
                file.contains("Dockerfile") -> frameworks.add("Docker")
                file.contains("Vagrantfile") -> frameworks.add("Vagrant")
                file.contains("terraform") -> frameworks.add("Terraform")
                file.contains("ansible") -> frameworks.add("Ansible")
                file.contains("kubernetes") -> frameworks.add("Kubernetes")
                file.contains("helm") -> frameworks.add("Helm")
                file.contains("react") -> frameworks.add("React")
                file.contains("redux") -> frameworks.add("Redux")
                file.contains("vue") -> frameworks.add("Vue.js")
                file.contains("svelte") -> frameworks.add("Svelte")
                file.contains("ember") -> frameworks.add("Ember.js")
                file.contains("backbone") -> frameworks.add("Backbone.js")
                file.contains("jquery") -> frameworks.add("jQuery")
                file.contains("bootstrap") -> frameworks.add("Bootstrap")
                file.contains("tailwind") -> frameworks.add("Tailwind CSS")
                file.contains("foundation") -> frameworks.add("Foundation")
                file.contains("bulma") -> frameworks.add("Bulma")
                // Add more framework detections as needed
            }
        }
        return frameworks
    }

    private fun mapExtensionToLanguage(extension: String): String {
        return if (extension.isEmpty()) "Unknown" else extensionToLanguageMap[extension] ?: extension
    }

    suspend fun getDeveloperStats(author: String): DeveloperStats {
        val repoPath = project.basePath ?: throw IllegalArgumentException("Project base path is null")
        val repoDir = File(repoPath)
        if (!repoDir.exists() || !repoDir.isDirectory) {
            throw IllegalArgumentException("Provided path is not a valid directory")
        }

        val command = listOf("git", "log", "--author=$author", "--name-only", "--pretty=format:")
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(command)
                .directory(repoDir)
                .start()
        }

        val output = process.inputStream.bufferedReader().readText()
        withContext(Dispatchers.IO) {
            process.waitFor()
        }

        val files = output.lines().filter { it.isNotBlank() }
        val extensions = files.map { File(it).extension }.filter { it.isNotEmpty() }.toSet()

        val languageFileCounts = files.groupingBy { mapExtensionToLanguage(File(it).extension) }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .toMap()

        val frameworks = detectFrameworks(files)

        return DeveloperStats(extensions, languageFileCounts,frameworks)

    }

    private fun getRelativeTime(date: Date): String {
        val now = Date()
        val diff = now.time - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            years > 0 -> "$years years ago"
            months > 0 -> "$months months ago"
            days > 0 -> "$days days ago"
            hours > 0 -> "$hours hours ago"
            minutes > 0 -> "$minutes minutes ago"
            else -> "$seconds seconds ago"
        }
    }

    suspend fun getContributorsWithLatestCommit(): List<Contributor> {
        val repoPath = project.basePath ?: throw IllegalArgumentException("Project base path is null")
        val repoDir = File(repoPath)
        if (!repoDir.exists() || !repoDir.isDirectory) {
            throw IllegalArgumentException("Provided path is not a valid directory")
        }

        val command = listOf("git", "log", "--format=%an|%ad", "--date=raw")
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder(command)
                .directory(repoDir)
                .start()
        }

        val output = process.inputStream.bufferedReader().readText()
        withContext(Dispatchers.IO) {
            process.waitFor()
        }

        val commitData = output.lines()
            .asSequence()
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split("|")
                val name = parts[0]
                val rawDate = parts[1].split(" ")[0].toLong() * 1000
                val date = Date(rawDate)
                name to date
            }
            .groupBy({ it.first }, { it.second })

        val contributors = commitData.map { (name, dates) ->
            val latestCommitDate = dates.maxOrNull()!!
            val commitCount = dates.size
            Contributor(name, latestCommitDate, getRelativeTime(latestCommitDate), commitCount)
        }.sortedByDescending { it.latestCommitDate }

        return contributors
    }

}