@file:Suppress("UnstableApiUsage")

package com.fueledbycaffeine.fastsync

import com.fueledbycaffeine.fastsync.FastSyncSettingsPlugin.Companion.FASTSYNC_ENABLED_PROPERTY
import org.gradle.api.IsolatedAction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.tasks.SourceSetContainer

/**
 * A plugin that makes IDE sync faster.
 *
 * ```
 * plugins {
 *   id("com.fueledbycaffeine.fastsync")
 * }
 * ```
 */
public class FastSyncSettingsPlugin : Plugin<Settings> {
  override fun apply(target: Settings): Unit = target.run {
    if (isIdeSync && isFastSyncEnabled) {
      gradle.lifecycle.beforeProject(MakeProjectIntransitiveAction())
    }
  }

  public companion object {
    public const val FASTSYNC_ENABLED_PROPERTY: String = "fastsync.enabled"
  }
}

private class MakeProjectIntransitiveAction : IsolatedAction<Project> {
  override fun execute(project: Project) {
    // The base plugin applies JvmEcosystemPlugin, which adds the SourceSetContainer as a project extension.
    project.pluginManager.withPlugin("base") {
      val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
      sourceSets.configureEach { sourceSet ->
        val runtimeClasspathName = sourceSet.runtimeClasspathConfigurationName
        val compileClasspathName = sourceSet.compileClasspathConfigurationName

        project.configurations.named(runtimeClasspathName) { configuration ->
          configuration.isTransitive = false

          // Use compile classpath for version information to help resolve dependencies from BOMs
          configuration.shouldResolveConsistentlyWith(project.configurations.named(compileClasspathName).get())
        }
      }
    }
  }
}

private val Settings.isIdeSync: Boolean
  get() = providers.systemProperty("idea.sync.active").getOrElse("false").toBoolean()

private val Settings.isFastSyncEnabled: Boolean
  get() = providers.gradleProperty(FASTSYNC_ENABLED_PROPERTY).getOrElse("true").toBoolean()
