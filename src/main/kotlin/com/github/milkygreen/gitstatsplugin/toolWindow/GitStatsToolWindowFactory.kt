package com.github.milkygreen.gitstatsplugin.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

class GitStatsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = GitStatsToolWindowContent(toolWindow, project)
        val content: Content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "Contributors", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(createLetterIcon('G'))
    }

    private fun createLetterIcon(letter: Char): ImageIcon {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = image.createGraphics()
        g2d.font = Font("Arial", Font.BOLD, 14)
        g2d.color = Color.BLACK
        g2d.drawString(letter.toString(), 4, 12)
        g2d.dispose()
        return ImageIcon(image)
    }

}