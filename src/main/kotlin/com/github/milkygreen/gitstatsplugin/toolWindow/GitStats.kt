import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis
import java.text.SimpleDateFormat
import java.util.*

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

val extensionToLanguageMap = mapOf(
    "kt" to "Kotlin",
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

fun mapExtensionToLanguage(extension: String): String {
    return extensionToLanguageMap[extension] ?: "Unknown"
}

fun getDeveloperStats(repoPath: String, author: String): DeveloperStats {
    val repoDir = File(repoPath)
    if (!repoDir.exists() || !repoDir.isDirectory) {
        throw IllegalArgumentException("Provided path is not a valid directory")
    }


    // 使用 git log 命令获取特定作者的提交，并提取其中的文件路径
    val command = listOf("git", "log", "--author=$author", "--name-only", "--pretty=format:")
    val process = ProcessBuilder(command)
        .directory(repoDir)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    // 解析输出，获取文件扩展名
    val files = output.lines().filter { it.isNotBlank() }
    val extensions = files.map { File(it).extension }.filter { it.isNotEmpty() }.toSet()

    // 计算每种语言的文件数量
    val languageFileCounts = files.groupingBy { mapExtensionToLanguage(File(it).extension) }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
        .toMap()

    // 计算提交次数
    val commitCountCommand = listOf("git", "rev-list", "--count", "HEAD", "--author=$author")
    val commitCountProcess = ProcessBuilder(commitCountCommand)
        .directory(repoDir)
        .start()

    val commitCount = commitCountProcess.inputStream.bufferedReader().readText().trim().toInt()
    commitCountProcess.waitFor()

    return DeveloperStats(commitCount, extensions, languageFileCounts)

}

fun getRelativeTime(date: Date): String {
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

fun getContributorsWithLatestCommit(repoPath: String): List<Contributor> {
    val repoDir = File(repoPath)
    if (!repoDir.exists() || !repoDir.isDirectory) {
        throw IllegalArgumentException("Provided path is not a valid directory")
    }

    val command = listOf("git", "log", "--format=%an|%ad", "--date=raw")
    val process = ProcessBuilder(command)
        .directory(repoDir)
        .start()

    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()

    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH)
    val contributors = output.lines()
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

    return contributors

}

fun main() = runBlocking {
    val repoPath = "/Users/yunmli/Desktop/projects/ebay/yunmli/mjolnir"
    try {
        val timeTaken = measureTimeMillis {

            val contributors = getContributorsWithLatestCommit(repoPath)
            println("Contributors and their latest commit dates (sorted by latest commit date):")
            contributors.forEach { contributor ->
                println("${contributor.name}: ${contributor.relativeDate}")

                val stats = getDeveloperStats(repoPath, contributor.name)
                println("Total commits by ${contributor.name}: ${stats.commitCount}")
                println("Languages used by ${contributor.name}:")
                stats.languages.forEach { println(it) }
                println("File counts per language (sorted by count):")
                stats.languageFileCounts.forEach { (language, count) ->
                    println("$language: $count")
                }
            }
        }
        println("Time taken: $timeTaken ms")
    } catch (e: Exception) {
        println(e.message)
    }
}