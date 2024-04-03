// Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAwareAction
import software.aws.toolkits.core.TokenConnectionSettings
import software.aws.toolkits.jetbrains.core.credentials.ToolkitConnectionManager
import software.aws.toolkits.jetbrains.core.credentials.pinning.CodeCatalystConnection
import software.aws.toolkits.jetbrains.core.credentials.sono.CodeCatalystCredentialManager
import software.aws.toolkits.jetbrains.core.credentials.sso.bearer.BearerTokenProvider
import software.aws.toolkits.jetbrains.core.explorer.refreshDevToolTree
import software.aws.toolkits.jetbrains.core.gettingstarted.requestCredentialsForCodeCatalyst
import software.aws.toolkits.jetbrains.utils.notifyWarn
import software.aws.toolkits.jetbrains.utils.runUnderProgressIfNeeded
import software.aws.toolkits.resources.AwsToolkitBundle
import software.aws.toolkits.resources.message
import software.aws.toolkits.telemetry.UiTelemetry

class SonoLogin : DumbAwareAction(AllIcons.Actions.Execute) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        UiTelemetry.click(project, elementId = "auth_start_CodeCatalyst")

        ApplicationManager.getApplication().executeOnPooledThread {
            val connectionManager = ToolkitConnectionManager.getInstance(project)
            connectionManager.activeConnectionForFeature(CodeCatalystConnection.getInstance())?.let {
                val connection = (it.getConnectionSettings() as TokenConnectionSettings).tokenProvider.delegate as BearerTokenProvider
                notifyWarn(
                    title = AwsToolkitBundle.message("credentials.sono.login.reauthenticate.title"),
                    content = AwsToolkitBundle.message("credentials.sono.login.reauthenticate.label"),
                    project = null,
                    notificationActions = listOf(
                        NotificationAction.create(
                            AwsToolkitBundle.message("codewhisperer.notification.custom.simple.button.got_it")
                        ) { _, notification ->
                            notification.expire()
                        },
                    )
                )
//                    connection.reauthenticate()
                runUnderProgressIfNeeded(project, message("credentials.pending.title"), true) {
                    connection.reauthenticate()
                }

            } ?: run {
                runInEdt {
                    // Start from scratch if no active connection
                    if (requestCredentialsForCodeCatalyst(project)) {
                        project.refreshDevToolTree()
                    }
                }
            }
        }
    }
}
