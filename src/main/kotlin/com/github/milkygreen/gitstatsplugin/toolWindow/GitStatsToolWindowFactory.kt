package com.github.milkygreen.gitstatsplugin.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
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
        }

        private fun createControlsPanel(): JPanel {
            val controlsPanel = JPanel()
            refreshButton.addActionListener { refreshTableData() }
            controlsPanel.add(refreshButton)
            return controlsPanel
        }

        private fun createTablePanel(): JPanel {
            val tablePanel = JPanel(BorderLayout())
            val columnNames = arrayOf("Name", "Commits", "Language")
            val data = arrayOf(
                arrayOf("Alice", 42, "Java, Kotlin, Python"),
                arrayOf("Bob", 37, "Kotlin, Scala"),
                arrayOf("Charlie", 29, "Python, JavaScript, TypeScript")
            )
            tableModel = object : DefaultTableModel(data, columnNames) {
                override fun isCellEditable(row: Int, column: Int): Boolean {
                    return false // Make cells non-editable
                }
            }
            val table = JTable(tableModel)
            table.rowHeight = 50 // Set custom row height
            table.setDefaultRenderer(Any::class.java, WordWrapCellRenderer())
            val scrollPane = JScrollPane(table)
            tablePanel.add(scrollPane, BorderLayout.CENTER)
            return tablePanel
        }

        private fun refreshTableData() {
            refreshButton.isEnabled = false
            // Simulate fetching new data
            SwingUtilities.invokeLater {
                val newData = arrayOf(
                    arrayOf("David", 50, "Go, Rust"),
                    arrayOf("Eve", 45, "Ruby, Perl"),
                    arrayOf("Frank", 40, "C++, C#")
                )
                tableModel.setDataVector(newData, arrayOf("Name", "Commits", "Language"))
                refreshButton.isEnabled = true
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