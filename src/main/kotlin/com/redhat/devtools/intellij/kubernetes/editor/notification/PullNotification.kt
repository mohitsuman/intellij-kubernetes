/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.kubernetes.editor.notification

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotificationPanel
import com.redhat.devtools.intellij.kubernetes.editor.hideNotification
import com.redhat.devtools.intellij.kubernetes.editor.showNotification
import io.fabric8.kubernetes.api.model.HasMetadata
import javax.swing.JComponent

/**
 * An editor (panel) notification that informs about a modification of a resource on the cluster and allows to reload
 * this resource.
 */
class PullNotification(private val editor: FileEditor, private val project: Project) {

    companion object {
        val KEY_PANEL = Key<JComponent>(PullNotification::class.java.canonicalName)
    }

    fun show(resource: HasMetadata, canPush: Boolean) {
        editor.showNotification(KEY_PANEL, { createPanel(resource, canPush) }, project)
    }

    fun hide() {
        editor.hideNotification(KEY_PANEL, project)
    }

    private fun createPanel(resource: HasMetadata, canPush: Boolean): EditorNotificationPanel {
        val panel = EditorNotificationPanel()
        panel.setText("${resource.kind} '${resource.metadata.name}' changed on cluster. Pull?")
        addPull(panel)
        if (canPush) {
            addPush(panel)
        }
        addDiff(panel)
        addIgnore(panel) {
            hide()
        }
        return panel
    }
}