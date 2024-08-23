package com.github.milkygreen.gitstatsplugin.toolWindow

import com.github.milkygreen.gitstatsplugin.services.MyProjectService
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBTextField
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel

class GitStatsToolWindowContent(private val toolWindow: ToolWindow, private val project: Project) {

    private val service = toolWindow.project.service<MyProjectService>()
    private val cs = service.cs

    val contentPanel = JPanel()
    private val refreshButton = JButton(AllIcons.Actions.Refresh)
    private val searchField = JBTextField()
    private val loadingLabel = JLabel(AllIcons.Process.Big.Step_1) // Loading icon
    private lateinit var tableModel: DefaultTableModel
    private var originalData: Array<Array<Any>> = arrayOf()
    val columnNames = arrayOf("Contributor", "Commits", "Latest Commit")
    private val tablePanel = JPanel(BorderLayout())
    private val loadingPanel = JPanel(BorderLayout())
    private lateinit var table: JTable
    private val loadingIcons = arrayOf(
        AllIcons.Process.Big.Step_1,
        AllIcons.Process.Big.Step_2,
        AllIcons.Process.Big.Step_3,
        AllIcons.Process.Big.Step_4,
        AllIcons.Process.Big.Step_5,
        AllIcons.Process.Big.Step_6,
        AllIcons.Process.Big.Step_7,
        AllIcons.Process.Big.Step_8
    )

    private var loadingTimer: Timer? = null

    init {
        setupContentPanel()
        refreshTableData() // Load data during initialization
    }

    private fun setupContentPanel() {
        contentPanel.layout = BorderLayout(0, 20)
        contentPanel.border = BorderFactory.createEmptyBorder(20, 0, 0, 0)
        contentPanel.add(createControlsPanel(), BorderLayout.PAGE_END)
        contentPanel.add(createTablePanel(), BorderLayout.CENTER)
        contentPanel.add(createToolbarPanel(), BorderLayout.NORTH)
    }

    private fun createControlsPanel(): JPanel {
        return JPanel()
    }

    private fun createToolbarPanel(): JPanel {
        val toolbarPanel = JPanel(BorderLayout())
        val searchPanel = JPanel(BorderLayout())
        setupSearchField()
        refreshButton.addActionListener { refreshTableData() }
        searchPanel.add(searchField, BorderLayout.CENTER)
        searchPanel.add(refreshButton, BorderLayout.EAST)
        toolbarPanel.add(searchPanel, BorderLayout.CENTER)
        return toolbarPanel
    }

    private fun setupSearchField() {
        searchField.toolTipText = "Search Contributors"
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = filterTableData()
            override fun removeUpdate(e: DocumentEvent) = filterTableData()
            override fun changedUpdate(e: DocumentEvent) = filterTableData()
        })
    }

    private fun createTablePanel(): JPanel {
        setupTableModel()
        setupTable()
        val scrollPane = JScrollPane(table)
        tablePanel.add(scrollPane, BorderLayout.CENTER)
        setupLoadingPanel()
        return tablePanel
    }

    private fun setupTableModel() {
        tableModel = object : DefaultTableModel(arrayOf(), columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false // Make cells non-editable
            }
        }
    }

    private fun setupTable() {
        table = JTable(tableModel)
        table.rowHeight = 20 // Set custom row height
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val column = table.columnAtPoint(e.point)
                if (column == 0) { // Contributor name column
                    val contributorName = table.getValueAt(row, column).toString()
                    showContributorDetails(contributorName)
                }
            }
        })
        adjustColumnWidths(table)
        table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer()
    }

    private fun setupLoadingPanel() {
        loadingPanel.add(loadingLabel, BorderLayout.CENTER) // Add loadingLabel to loadingPanel
    }

    private fun refreshTableData() {
        refreshButton.isEnabled = false
        searchField.text = "" // Clear the search field
        showLoadingIcon()
        cs.launch {
            try {
                val contributors = withContext(Dispatchers.Default) { service.getContributorsWithLatestCommit() }
                originalData = contributors.map { contributor ->
                    arrayOf<Any>(contributor.name, contributor.commitCount, contributor.relativeDate)
                }.toTypedArray()
                withContext(Dispatchers.EDT) { updateTableView() }
            } catch (e: Exception) {
                handleRefreshError(e)
            } finally {
                withContext(Dispatchers.EDT) { hideLoadingIcon() }
            }
        }
    }

    private fun showLoadingIcon() {
        loadingLabel.isVisible = true // Show loading icon
        tablePanel.removeAll() // Clear the table panel
        tablePanel.add(loadingPanel, BorderLayout.CENTER) // Add loading panel
        tablePanel.revalidate()
        tablePanel.repaint()

        var iconIndex = 0
        loadingTimer = Timer(100) {
            loadingLabel.icon = loadingIcons[iconIndex]
            iconIndex = (iconIndex + 1) % loadingIcons.size
        }
        loadingTimer?.start()
    }

    private fun hideLoadingIcon() {
        refreshButton.isEnabled = true
        loadingLabel.isVisible = false
        loadingTimer?.stop()
        loadingTimer = null
        tablePanel.removeAll() // Clear the loading panel
        tablePanel.add(JScrollPane(table), BorderLayout.CENTER) // Add table
        tablePanel.revalidate()
        tablePanel.repaint()
    }

    private suspend fun handleRefreshError(e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.EDT) {
            JOptionPane.showMessageDialog(
                null,
                "Failed to refresh data: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun updateTableView() {
        tableModel.setDataVector(originalData, columnNames)
        adjustColumnWidths(table)
        tablePanel.removeAll() // Remove loading panel
        table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer()
        tablePanel.add(JScrollPane(table), BorderLayout.CENTER) // Add table
        tablePanel.revalidate()
        tablePanel.repaint()
    }

    private fun filterTableData() {
        val searchText = searchField.text.lowercase()
        val filteredData = originalData.filter { row ->
            row[0].toString().lowercase().contains(searchText)
        }.toTypedArray()
        tableModel.setDataVector(filteredData, columnNames)
        table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer() // Reapply cell renderer
    }

    private fun adjustColumnWidths(table: JTable) {
        val minColumnWidth = 100
        for (column in 0 until table.columnCount) {
            var maxWidth = minColumnWidth
            for (row in 0 until table.rowCount) {
                val renderer = table.getCellRenderer(row, column)
                val comp = table.prepareRenderer(renderer, row, column)
                maxWidth = maxOf(comp.preferredSize.width + 10, maxWidth)
            }
            table.columnModel.getColumn(column).preferredWidth = maxWidth
        }
    }

    private fun showContributorDetails(contributorName: String) {
        cs.launch {
            try {
                val contributorStats = withContext(Dispatchers.Default) {
                    service.getDeveloperStats(contributorName)
                }
                withContext(Dispatchers.EDT) {
                    val dialog = JDialog()
                    dialog.title = contributorName
                    dialog.setSize(600, 400) // Set suitable width and height
                    dialog.setLocationRelativeTo(null)
                    dialog.isModal = true

                    val mainPanel = JPanel(BorderLayout())
                    val topPanel = createTopPanel(contributorStats.frameworks)
                    val scrollPane = createLanguageTable(contributorStats.languageFileCounts)

                    mainPanel.add(topPanel, BorderLayout.NORTH)
                    mainPanel.add(scrollPane, BorderLayout.CENTER)

                    dialog.add(mainPanel)
                    dialog.isVisible = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createTopPanel(frameworks: Set<String>): JPanel {
        val topPanel = JPanel(BorderLayout())

        val frameworksPanel = JPanel()
        frameworksPanel.layout = BoxLayout(frameworksPanel, BoxLayout.X_AXIS)

        val frameworksTitle = JLabel("Tools & Frameworks: ")
        frameworksTitle.font = frameworksTitle.font.deriveFont(14f)
        frameworksTitle.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        frameworksPanel.add(frameworksTitle)

        val frameworksLabel = JLabel(frameworks.joinToString(" | "))
        frameworksPanel.add(frameworksLabel)

        val centeredPanel = JPanel(BorderLayout())
        centeredPanel.add(frameworksPanel, BorderLayout.CENTER)
        centeredPanel.border = BorderFactory.createEmptyBorder(20, 0, 20, 0)

        topPanel.add(centeredPanel, BorderLayout.CENTER)
        return topPanel
    }

    private fun createLanguageTable(languageFileCounts: Map<String, Int>): JScrollPane {
        val columnNames = arrayOf("Language/File", "Count")
        val data = mutableListOf<Array<Any>>()

        // Add language file counts to the table data
        data.addAll(languageFileCounts.entries
            .sortedByDescending { it.value }
            .map { arrayOf(it.key, it.value) })

        val tableModel = DefaultTableModel(data.toTypedArray(), columnNames)
        val table = JTable(tableModel)
        return JScrollPane(table)
    }
}