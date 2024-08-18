package com.github.milkygreen.gitstatsplugin.toolWindow

import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class ContributorNameCellRenderer : DefaultTableCellRenderer() {

    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (component is JLabel) {
            component.text = "<html><u>${value?.toString() ?: ""}</u></html>" // Underline text
            component.foreground = Color(100, 149, 237) // Set text color to a softer blue (Cornflower Blue)
        }
        return component
    }
}