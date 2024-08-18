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
        val commitCount: Int,
        val languages: Set<String>,
        val languageFileCounts: Map<String, Int>
    )

    data class Contributor(
        val name: String,
        val latestCommitDate: Date,
        val relativeDate: String
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

    private fun mapExtensionToLanguage(extension: String): String {
        return extensionToLanguageMap[extension] ?: extension
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

        val commitCountCommand = listOf("git", "rev-list", "--count", "HEAD", "--author=$author")
        val commitCountProcess = withContext(Dispatchers.IO) {
            ProcessBuilder(commitCountCommand)
                .directory(repoDir)
                .start()
        }

        val commitCount = commitCountProcess.inputStream.bufferedReader().readText().trim().toInt()
        withContext(Dispatchers.IO) {
            commitCountProcess.waitFor()
        }

        return DeveloperStats(commitCount, extensions, languageFileCounts)

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

        val contributors = output.lines()
            .asSequence()
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split("|")
                val name = parts[0]
                val rawDate = parts[1].split(" ")[0].toLong() * 1000
                val date = Date(rawDate)
                Contributor(name, date, getRelativeTime(date))
            }
            .groupBy { it.name }
            .map { (name, commits) ->
                val latestCommit = commits.maxByOrNull { it.latestCommitDate }!!
                Contributor(name, latestCommit.latestCommitDate, latestCommit.relativeDate)
            }
            .sortedByDescending { it.latestCommitDate }
            .toList()

        return contributors

    }

}