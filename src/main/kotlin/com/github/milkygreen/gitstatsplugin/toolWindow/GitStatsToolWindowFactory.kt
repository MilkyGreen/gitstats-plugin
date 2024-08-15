package com.github.milkygreen.gitstatsplugin.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.ui.components.JBTextField
import com.intellij.icons.AllIcons
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.toolWindow.ToolWindowDescriptor
import getContributorsWithLatestCommit
import getDeveloperStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.table.DefaultTableCellRenderer

class GitStatsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = GitStatsToolWindowContent(toolWindow, project)
        val content: Content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "Contributors", false)
        toolWindow.contentManager.addContent(content)
    }

    private class GitStatsToolWindowContent(val toolWindow: ToolWindow, private val project: Project) {

        val contentPanel = JPanel()
        private val refreshButton = JButton(AllIcons.Actions.Refresh)
        private val searchField = JBTextField()
        private val loadingLabel = JLabel(AllIcons.Process.Step_1) // Loading icon
        private lateinit var tableModel: DefaultTableModel
        private var originalData: Array<Array<Any>> = arrayOf()
        val columnNames = arrayOf("Contributor", "Commits", "Latest Commit")
        private val tablePanel = JPanel(BorderLayout())
        private val loadingPanel = JPanel(BorderLayout())
        private lateinit var table: JTable

        init {
            contentPanel.layout = BorderLayout(0, 20)
            contentPanel.border = BorderFactory.createEmptyBorder(20, 0, 0, 0)
            contentPanel.add(createControlsPanel(), BorderLayout.PAGE_END)
            contentPanel.add(createTablePanel(), BorderLayout.CENTER)
            contentPanel.add(createToolbarPanel(), BorderLayout.NORTH)
            refreshTableData() // Load data during initialization
        }

        private fun createControlsPanel(): JPanel {
            val controlsPanel = JPanel()
            return controlsPanel
        }

        private fun createToolbarPanel(): JPanel {
            val toolbarPanel = JPanel(BorderLayout())
            val searchPanel = JPanel(BorderLayout())
            searchField.toolTipText = "Search Contributors"
            searchField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = filterTableData()
                override fun removeUpdate(e: DocumentEvent) = filterTableData()
                override fun changedUpdate(e: DocumentEvent) = filterTableData()
            })
            refreshButton.addActionListener { refreshTableData() }
            searchPanel.add(searchField, BorderLayout.CENTER)
            searchPanel.add(refreshButton, BorderLayout.EAST)
            toolbarPanel.add(searchPanel, BorderLayout.CENTER)
            return toolbarPanel
        }

        private fun createTablePanel(): JPanel {
            val data = arrayOf<Array<Any>>()
            tableModel = object : DefaultTableModel(data, columnNames) {
                override fun isCellEditable(row: Int, column: Int): Boolean {
                    return false // Make cells non-editable
                }
            }
            table = JTable(tableModel)
            table.rowHeight = 20 // Set custom row height
//            table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer() // Apply custom renderer
             // Apply custom renderer
            table.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val row = table.rowAtPoint(e.point)
                    val column = table.columnAtPoint(e.point)
                    if (column == 0) { // Contributor name column
                        val contributorName = table.getValueAt(row, column).toString()
                        openContributorTab(contributorName)
                    }
                }
            })
//            adjustColumnWidths(table)
            val scrollPane = JScrollPane(table)
            table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer()
            tablePanel.add(scrollPane, BorderLayout.CENTER)

            // Add loading icon to the center of the table panel
            loadingLabel.icon = AllIcons.Process.Big.Step_1 // Use a larger icon
            loadingPanel.add(loadingLabel, BorderLayout.CENTER)
            tablePanel.add(loadingPanel, BorderLayout.CENTER)
            loadingLabel.isVisible = false // Initially hidden

            return tablePanel
        }

        private fun refreshTableData() {
            refreshButton.isEnabled = false
            loadingLabel.isVisible = true // Show loading icon
            tablePanel.removeAll() // Clear the table panel
            tablePanel.add(loadingPanel, BorderLayout.CENTER) // Add loading panel
            tablePanel.revalidate()
            tablePanel.repaint()

            ApplicationManager.getApplication().executeOnPooledThread {
                runBlocking {
                    delay(5000) // Simulate a long-running operation
                    val repoPath = "/Users/yunmli/Desktop/projects/private/intellij-sdk-code-samples"
                    val contributors = getContributorsWithLatestCommit(repoPath)

                    originalData = contributors.map { contributor ->
                        val stats = getDeveloperStats(repoPath, contributor.name)
                        arrayOf<Any>(
                            contributor.name,
                            stats.commitCount,
                            contributor.relativeDate
                        )
                    }.toTypedArray()
                    SwingUtilities.invokeLater {
                        tableModel.setDataVector(originalData, columnNames)
                        adjustColumnWidths(table)
                        tablePanel.removeAll() // Remove loading panel
                        table.columnModel.getColumn(0).cellRenderer = ContributorNameCellRenderer()
                        tablePanel.add(JScrollPane(table), BorderLayout.CENTER) // Add table
                        tablePanel.revalidate()
                        tablePanel.repaint()
                        refreshButton.isEnabled = true
                        loadingLabel.isVisible = false // Hide loading icon
                    }
                }
            }
        }

        private fun filterTableData() {
            val searchText = searchField.text.lowercase()
            val filteredData = originalData.filter { row ->
                row[0].toString().lowercase().contains(searchText)
            }.toTypedArray()
            tableModel.setDataVector(filteredData, columnNames)
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

        private fun openContributorTab(contributorName: String) {
            val contentManager = toolWindow.contentManager
            val existingContent = contentManager.contents.find { it.displayName != "Contributors" }

            // Close any existing contributor tab
            if (existingContent != null) {
                contentManager.removeContent(existingContent, true)
            }

            val contributorStats = getDeveloperStats("/Users/yunmli/Desktop/projects/private/intellij-sdk-code-samples", contributorName)
            val contributorPanel = JPanel(BorderLayout())
            val textArea = JTextArea()
            textArea.text = """
                Contributor: $contributorName
                Commits: ${contributorStats.commitCount}
                Languages: ${contributorStats.languages.joinToString(", ")}
                File counts per language: ${contributorStats.languageFileCounts.entries.joinToString(", ") { "${it.key}: ${it.value}" }}
            """.trimIndent()
            textArea.isEditable = false
            contributorPanel.add(JScrollPane(textArea), BorderLayout.CENTER)

            val content: Content = ContentFactory.getInstance().createContent(contributorPanel, contributorName, true)
            content.isCloseable = true
            contentManager.addContent(content)
            contentManager.setSelectedContent(content)
        }
    }

    private class WordWrapCellRenderer : JTextArea(), TableCellRenderer {

        init {
            lineWrap = true
            wrapStyleWord = true
            isOpaque = true
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            text = value?.toString() ?: ""
            foreground = Color.BLUE
            size = Dimension(table.columnModel.getColumn(column).width, preferredSize.height)
            if (table.getRowHeight(row) != preferredSize.height) {
                table.setRowHeight(row, preferredSize.height)
            }
            return this
        }
    }

    private class ContributorNameCellRenderer : DefaultTableCellRenderer() {

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            println("--------ContributorNameCellRenderer called------------")
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (component is JLabel) {
                component.text = "<html><u>${value?.toString() ?: ""}</u></html>" // Underline text
                component.foreground = Color(100, 149, 237) // Set text color to a softer blue (Cornflower Blue)
            }
            return component
        }
    }
}