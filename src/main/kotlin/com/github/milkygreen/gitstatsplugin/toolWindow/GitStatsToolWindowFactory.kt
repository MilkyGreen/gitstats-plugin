package com.github.milkygreen.gitstatsplugin.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import getContributorsWithLatestCommit
import getDeveloperStats
import kotlinx.coroutines.runBlocking
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import java.awt.*

class GitStatsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = GitStatsToolWindowContent(toolWindow)
        val content: Content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private class GitStatsToolWindowContent(toolWindow: ToolWindow) {

        val contentPanel = JPanel()
        private val refreshButton = JButton("Refresh")
        private lateinit var tableModel: DefaultTableModel

        init {
            contentPanel.layout = BorderLayout(0, 20)
            contentPanel.border = BorderFactory.createEmptyBorder(40, 0, 0, 0)
            contentPanel.add(createControlsPanel(), BorderLayout.PAGE_END)
            contentPanel.add(createTablePanel(), BorderLayout.CENTER)
            refreshTableData() // Load data during initialization
        }

        private fun createControlsPanel(): JPanel {
            val controlsPanel = JPanel()
            refreshButton.addActionListener { refreshTableData() }
            controlsPanel.add(refreshButton)
            return controlsPanel
        }

        private fun createTablePanel(): JPanel {
            val tablePanel = JPanel(BorderLayout())
            val columnNames = arrayOf("Name", "Commits", "Languages", "Latest Commit")
            val data = arrayOf<Array<Any>>()
            tableModel = object : DefaultTableModel(data, columnNames) {
                override fun isCellEditable(row: Int, column: Int): Boolean {
                    return false // Make cells non-editable
                }
            }
            val table = JTable(tableModel)
            table.rowHeight = 50 // Set custom row height
            table.setDefaultRenderer(Any::class.java, WordWrapCellRenderer())
            adjustColumnWidths(table)
            val scrollPane = JScrollPane(table)
            tablePanel.add(scrollPane, BorderLayout.CENTER)
            return tablePanel
        }

        private fun refreshTableData() {
            refreshButton.isEnabled = false
            SwingUtilities.invokeLater {
                runBlocking {
                    val repoPath = "/Users/yunmli/Desktop/projects/ebay/yunmli/mjolnir"
                    val contributors = getContributorsWithLatestCommit(repoPath)
                    val newData = contributors.map { contributor ->
                        val stats = getDeveloperStats(repoPath, contributor.name)
                        arrayOf(
                            contributor.name,
                            stats.commitCount,
                            stats.languages.joinToString(", "),
                            contributor.relativeDate
                        )
                    }.toTypedArray()
                    tableModel.setDataVector(newData, arrayOf("Name", "Commits", "Languages", "Latest Commit"))
                    val table = JTable(tableModel)
                    adjustColumnWidths(table)
                    refreshButton.isEnabled = true
                }
            }
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
            size = Dimension(table.columnModel.getColumn(column).width, preferredSize.height)
            if (table.getRowHeight(row) != preferredSize.height) {
                table.setRowHeight(row, preferredSize.height)
            }
            return this
        }
    }
}