// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import software.aws.toolkits.jetbrains.core.gettingstarted.reauthenticateWithCodeCatalyst
import software.aws.toolkits.jetbrains.utils.notifyWarn
import software.aws.toolkits.resources.AwsToolkitBundle
import software.aws.toolkits.telemetry.UiTelemetry

class ReauthWithCodeCatalyst : DumbAwareAction(AllIcons.Actions.Copy) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
//        UiTelemetry.click(project, elementId = "reauth_CodeCatalyst")// Telemetry event for Re-auth of CodeCatalyst

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

        reauthenticateWithCodeCatalyst(project)
    }
}
