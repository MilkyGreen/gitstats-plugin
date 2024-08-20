package com.github.milkygreen.gitstatsplugin.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ScalableIcon
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class GitStatsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = GitStatsToolWindowContent(toolWindow, project)
        val content: Content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "Contributors", false)
        toolWindow.contentManager.addContent(content)
    }
}