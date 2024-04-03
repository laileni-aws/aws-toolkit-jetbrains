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
import software.aws.toolkits.jetbrains.core.credentials.pinning.CodeCatalystConnection
import software.aws.toolkits.jetbrains.core.credentials.sono.CodeCatalystCredentialManager
import software.aws.toolkits.jetbrains.core.explorer.actions.AnActionTreeNode
import software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes.actions.OpenWorkspaceInGateway
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.ActiveConnection
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.ActiveConnectionType
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.BearerTokenFeatureSet
import software.aws.toolkits.jetbrains.core.gettingstarted.editor.checkBearerConnectionValidity
import software.aws.toolkits.jetbrains.services.codewhisperer.explorer.isCodeWhispererExpired
import software.aws.toolkits.jetbrains.utils.isRunningOnRemoteBackend
import software.aws.toolkits.resources.message

class CawsRootNode(private val nodeProject: Project) : AbstractTreeNode<String>(nodeProject, CawsServiceNode.NODE_NAME), PinnedConnectionNode {
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val connection = checkBearerConnectionValidity(project, BearerTokenFeatureSet.CODECATALYST)
        val groupId = when {
            // if connection token is valid, CodeWhisperer is not expired and CodeCatalyst is expired consider this condition as Partial Expiration.
            connection is ActiveConnection.ValidBearer && !isCodeWhispererExpired(project) &&
                CodeCatalystCredentialManager.getInstance(project).checkPartialExpiration() -> CAWS_EXPIRED_TOKEN_ACTION_GROUP
            connection is ActiveConnection.NotConnected -> CAWS_SIGNED_OUT_ACTION_GROUP
            connection is ActiveConnection.ExpiredBearer -> CAWS_EXPIRED_TOKEN_ACTION_GROUP
            else -> CAWS_SIGNED_IN_ACTION_GROUP
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
        val connection = checkBearerConnectionValidity(project, BearerTokenFeatureSet.CODECATALYST)
        val connectionText = when {
            /*
            This "update" function is called regularly so we can not make a service call for every execution.
            This condition checks for the Paritial expiration to display "Expired Connection" text next to CodeCatalyst in Developer Tools.
             */
            /*
            connection is ActiveConnection.ValidBearer && !isCodeWhispererExpired(project) &&
                CodeCatalystCredentialManager.getInstance(project).checkPartialExpiration() -> message(
                "caws.expired.connection"
            )*/
            connection is ActiveConnection.NotConnected -> null
            connection is ActiveConnection.ExpiredBearer -> message("caws.expired.connection")
            else -> when (connection.connectionType) {
                ActiveConnectionType.BUILDER_ID -> message("caws.connected.builder_id")
                else -> message("caws.connected.identity_center")
            }
        }
        presentation.addText(connectionText, SimpleTextAttributes.GRAY_ATTRIBUTES)
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
    override fun buildServiceRootNode(nodeProject: Project): AbstractTreeNode<*> = CawsRootNode(nodeProject)

    companion object {
        val NODE_NAME = message("caws.devtoolPanel.title")
    }
}
