/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.kubernetes.tests;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import com.redhat.devtools.intellij.commonUiTestLibrary.fixtures.dialogs.errors.IdeFatalErrorsDialog;
import org.assertj.swing.core.MouseButton;
import org.jboss.tools.intellij.kubernetes.fixtures.mainIdeWindow.IdeStatusBarFixture;

import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;

/**
 * @author olkornii@redhat.com
 */
public abstract class AbstractKubernetesTest {

    public static void openResourceContentList(String[] path, ComponentFixture kubernetesViewTree){
        openClusterContent(kubernetesViewTree);
        for (String resourceForOpen : path){
            kubernetesViewTree.findText(resourceForOpen).doubleClick(MouseButton.LEFT_BUTTON); // open Nodes content
            waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Resources is not available.", () -> isResourcesLoaded(kubernetesViewTree));
        }
    }

    public static boolean isResourcesLoaded(ComponentFixture kubernetesViewTree){
        List<RemoteText> allTextFromTree = kubernetesViewTree.findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("loading...")){
                return false;
            }
        }
        return true;
    }

    public static void openClusterContent(ComponentFixture kubernetesViewTree){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();
        boolean needClickOnMinikube = true;
        for (RemoteText findNodes : kubernetesToolsText){
            if (findNodes.getText().contains("Nodes")){
                needClickOnMinikube = false;
                break;
            }
        }
        if (needClickOnMinikube){
            String clusterText = kubernetesViewTree.findAllText().get(0).getText();
            kubernetesViewTree.findText(clusterText).doubleClick(MouseButton.LEFT_BUTTON);
        }
        waitFor(Duration.ofSeconds(15), Duration.ofSeconds(1), "Kubernetes Tree View is not available.", () -> isNodesOpened(kubernetesViewTree));
    }

    public static boolean isNodesOpened(ComponentFixture kubernetesViewTree){
        List<RemoteText> allTextFromTree = kubernetesViewTree.findAllText();
        for (RemoteText actualText : allTextFromTree){
            if (actualText.getText().contains("Nodes")){
                return true;
            }
        }
        return false;
    }

    public static RemoteText getResourceByIdInParent(String parentName, int id, ComponentFixture kubernetesViewTree){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();
        int parentId = 0;
        for (RemoteText findParent : kubernetesToolsText){
            if (findParent.getText().contains(parentName)){
                break;
            }
            parentId++;
        }
        return kubernetesViewTree.findAllText().get(parentId + id + 1);
    }

    public static void hideClusterContent(ComponentFixture kubernetesViewTree){
        String clusterText = kubernetesViewTree.findAllText().get(0).getText();
        kubernetesViewTree.findText(clusterText).doubleClick(); // hide cluster content
    }

    public static boolean isResourceCreated(ComponentFixture kubernetesViewTree, String resourceName, boolean hardCompare){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();

        if (hardCompare){
            for (RemoteText findNewResource : kubernetesToolsText){
                if (resourceName.equals(findNewResource.getText())){
                    return true;
                }
            }
        } else {
            for (RemoteText findNewResource : kubernetesToolsText){
                if (findNewResource.getText().contains(resourceName)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isResourceDeleted(ComponentFixture kubernetesViewTree, String resourceName){
        List<RemoteText> kubernetesToolsText = kubernetesViewTree.findAllText();
        for (RemoteText findNewResource : kubernetesToolsText){
            if (resourceName.equals(findNewResource.getText())){
                return false;
            }
        }
        return true;
    }

    public static boolean isError(RemoteRobot robot){
        IdeStatusBarFixture ideStatusBar = robot.find(IdeStatusBarFixture.class);
        try {
            ideStatusBar.ideErrorsIcon();
        } catch (WaitForConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    public static boolean clearErrors(RemoteRobot robot){
        IdeStatusBarFixture statusBar = robot.find(IdeStatusBarFixture.class);
        try {
            statusBar.ideErrorsIcon().click();
        } catch (WaitForConditionTimeoutException e) {
            return true;
        }
        IdeFatalErrorsDialog ideErrorsDialog = robot.find(IdeFatalErrorsDialog.class);
        ideErrorsDialog.clearAll();
        return true;
    }
}
