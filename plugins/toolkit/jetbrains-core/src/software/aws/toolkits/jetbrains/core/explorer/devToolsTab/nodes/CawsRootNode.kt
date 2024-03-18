// Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import software.aws.toolkits.jetbrains.ToolkitPlaces
import software.aws.toolkits.jetbrains.core.credentials.ToolkitConnectionManager
import software.aws.toolkits.jetbrains.core.credentials.pinning.CodeCatalystConnection
import software.aws.toolkits.jetbrains.core.credentials.sono.isSono
import software.aws.toolkits.jetbrains.core.explorer.actions.AnActionTreeNode
import software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes.actions.OpenWorkspaceInGateway
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.ActiveConnection
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.BearerTokenFeatureSet
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.checkBearerConnectionValidity
import software.aws.toolkits.jetbrains.utils.isRunningOnRemoteBackend
import software.aws.toolkits.resources.message

class CawsRootNode(private val project: Project) : AbstractTreeNode<String>(project, CawsServiceNode.NODE_NAME), PinnedConnectionNode {
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val connection = checkBearerConnectionValidity(project, BearerTokenFeatureSet.CODECATALYST)
        val groupId = when (connection) {
            is ActiveConnection.NotConnected -> CAWS_SIGNED_OUT_ACTION_GROUP
            is ActiveConnection.ValidBearer -> CAWS_SIGNED_IN_ACTION_GROUP
            else -> CAWS_EXPIRED_TOKEN_ACTION_GROUP
        }
        val actions = ActionManager.getInstance().getAction(groupId) as ActionGroup
        return actions.getChildren(null).mapNotNull {
            if (it is OpenWorkspaceInGateway && isRunningOnRemoteBackend()) {
                return@mapNotNull null
            }

            AnActionTreeNode(project, ToolkitPlaces.DEVTOOLS_TOOL_WINDOW, it)
        }
    }

    override fun update(presentation: PresentationData) {
        presentation.addText(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        val connection = ToolkitConnectionManager.getInstance(project).activeConnectionForFeature(CodeCatalystConnection.getInstance())
        if (connection != null) {
            val msgId = if (connection.isSono()) {
                "caws.connected.builder_id"
            } else {
                "caws.connected.identity_center"
            }
            presentation.addText(message(msgId), SimpleTextAttributes.GRAY_ATTRIBUTES)
        }
    }

    override fun feature() = CodeCatalystConnection.getInstance()
    companion object {
        const val CAWS_SIGNED_IN_ACTION_GROUP = "aws.caws.devtools.actions.loggedin"
        const val CAWS_SIGNED_OUT_ACTION_GROUP = "aws.caws.devtools.actions.loggedout"
        const val CAWS_EXPIRED_TOKEN_ACTION_GROUP = "aws.caws.devtools.actions.expired"
    }
}

class CawsServiceNode : DevToolsServiceNode {
    override val serviceId: String = "aws.toolkit.caws.service"
    override fun buildServiceRootNode(project: Project): AbstractTreeNode<*> = CawsRootNode(project)

    companion object {
        val NODE_NAME = message("caws.devtoolPanel.title")
    }
}
