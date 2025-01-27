/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.kubernetes.model.mocks

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.redhat.devtools.intellij.common.validation.KubernetesResourceInfo
import com.redhat.devtools.intellij.common.validation.KubernetesTypeInfo
import com.redhat.devtools.intellij.kubernetes.model.IModelChangeObservable
import com.redhat.devtools.intellij.kubernetes.model.IResourceModel
import com.redhat.devtools.intellij.kubernetes.model.context.IActiveContext
import com.redhat.devtools.intellij.kubernetes.model.context.IContext
import com.redhat.devtools.intellij.kubernetes.model.resource.ILogWatcher
import com.redhat.devtools.intellij.kubernetes.model.resource.INamespacedResourceOperator
import com.redhat.devtools.intellij.kubernetes.model.resource.INonNamespacedResourceOperator
import com.redhat.devtools.intellij.kubernetes.model.resource.ResourceKind
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.NamedContext
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.client.Client
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.LogWatch
import java.io.OutputStream
import org.mockito.Mockito

object Mocks {

    fun contextFactory(context: IActiveContext<HasMetadata, KubernetesClient>?)
            : (IModelChangeObservable, NamedContext?) -> IActiveContext<HasMetadata, KubernetesClient> {
        return mock {
            /**
             * Trying to use {@code com.nhaarman.mockitokotlin2.doReturn} leads to
             * "Overload Resolution Ambiguity" with {@code org.mockito.Mockito.doReturn} in intellij.
             * Gradle compiles it just fine
             *
             * @see <a href="https://youtrack.jetbrains.com/issue/KT-22961">KT-22961</a>
             * @see <a href="https://stackoverflow.com/questions/38779666/how-to-fix-overload-resolution-ambiguity-in-kotlin-no-lambda">fix-overload-resolution-ambiguity</a>
             */
            Mockito.doReturn(context)
                .`when`(mock).invoke(any(), anyOrNull()) // anyOrNull() bcs NamedContext is nullable
        }
    }

    fun context(namedContext: NamedContext)
            : IContext {
        return mock {
            Mockito.doReturn(namedContext)
                .`when`(mock).context
        }
    }

    fun activeContext(currentNamespace: Namespace, context: NamedContext)
            : IActiveContext<HasMetadata, KubernetesClient> {
        return mock {
            Mockito.doReturn(currentNamespace.metadata.name)
                .`when`(mock).getCurrentNamespace()
            Mockito.doReturn(context)
                .`when`(mock).context
        }
    }

    inline fun <reified T : HasMetadata, C : Client> namespacedResourceOperator(
        kind: ResourceKind<T>?,
        resources: Collection<T>,
        namespace: Namespace,
        crossinline watchOperation: (watcher: Watcher<in T>) -> Watch? = { null },
        deleteSuccess: Boolean = true,
        getReturnValue: T? = null
    ): INamespacedResourceOperator<T, C>  {
        val mock = mock<INamespacedResourceOperator<T, C>>()
        mockNamespacedOperatorMethods(namespace, kind, resources, watchOperation, deleteSuccess, getReturnValue, mock)
        return mock
    }

    inline fun <reified T : HasMetadata, C : Client> logWatchingNamespacedResourceOperator(
        kind: ResourceKind<T>?,
        resources: Collection<T>,
        namespace: Namespace,
        crossinline watchOperation: (watcher: Watcher<in T>) -> Watch? = { null },
        deleteSuccess: Boolean = true,
        getReturnValue: T? = null,
        out: OutputStream = mock()
    ): INamespacedResourceOperator<T, C>  {
        val mock = mock<INamespacedResourceOperator<T, C>>(arrayOf(ILogWatcher::class))
        mockNamespacedOperatorMethods(namespace, kind, resources, watchOperation, deleteSuccess, getReturnValue, mock)
        @Suppress("UNCHECKED_CAST")
        val logWatcher = mock as ILogWatcher<T>
        val logWatch: LogWatch = mock()
        doReturn(logWatch)
            .whenever(logWatcher).watchLog(any(), eq(out))
        return mock
    }

    inline fun <C : Client, reified T : HasMetadata> mockNamespacedOperatorMethods(
        namespace: Namespace,
        kind: ResourceKind<T>?,
        resources: Collection<T>,
        crossinline watchOperation: (watcher: Watcher<in T>) -> Watch?,
        deleteSuccess: Boolean,
        getReturnValue: T?,
        mock: INamespacedResourceOperator<T, C>
        ) {
        doReturn(namespace.metadata.name)
            .whenever(mock).namespace
        doReturn(kind)
            .whenever(mock).kind
        doReturn(resources)
            .whenever(mock).allResources
        doAnswer { invocation ->
            watchOperation.invoke(invocation.getArgument(0))
        }
            .whenever(mock).watch(any(), any())
        doAnswer { invocation ->
            watchOperation.invoke(invocation.getArgument(0))
        }
            .whenever(mock).watchAll(any())

        doReturn(deleteSuccess)
            .whenever(mock).delete(any())
        doReturn(getReturnValue)
            .whenever(mock).get(any())
    }

    inline fun <reified T : HasMetadata, C : Client> nonNamespacedResourceOperator(
        kind: ResourceKind<T>,
        resources: Collection<T>,
        crossinline watchOperation: (watcher: Watcher<in T>) -> Watch? = { null },
        deleteSuccess: Boolean = true
    ) : INonNamespacedResourceOperator<T, C> {
        return mock {
            on { this.kind } doReturn kind
            on { allResources } doReturn resources
            on { watch(any(), any()) } doAnswer { invocation ->
                watchOperation.invoke(invocation.getArgument(0))
            }
            on { watchAll(any()) } doAnswer { invocation ->
                watchOperation.invoke(invocation.getArgument(0))
            }
            on { delete(any()) } doReturn deleteSuccess
        }
    }

    fun resourceModel(): IResourceModel {
        return mock {}
    }

    fun kubernetesTypeInfo(kind: String?, apiGroup: String?): KubernetesTypeInfo {
        return mock {
            on { this.apiGroup } doReturn apiGroup
            on { this.kind } doReturn kind
        }
    }

    fun kubernetesResourceInfo(name: String?, namespace: String?, typeInfo: KubernetesTypeInfo): KubernetesResourceInfo {
        return mock {
            on { this.name } doReturn name
            on { this.namespace } doReturn namespace
            on { this.typeInfo } doReturn typeInfo
        }
    }

}
