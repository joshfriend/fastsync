@file:Suppress("UnstableApiUsage")

package com.fueledbycaffeine.fastsync

import com.fueledbycaffeine.fastsync.FastSyncSettingsPlugin.Companion.FASTSYNC_ENABLED_PROPERTY
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.initialization.Settings

/**
 * A plugin that makes IDE sync faster.
 *
 * plugins {
 *   id 'com.fueledbycaffeine.fastsync'
 * }
 */
public class FastSyncSettingsPlugin : Plugin<Settings> {
  override fun apply(target: Settings): Unit = target.run {
    if (isIdeSync && isFastSyncEnabled) {
      gradle.beforeProject(MakeProjectIntransitiveAction())
    }
  }

  public companion object {
    public const val FASTSYNC_ENABLED_PROPERTY: String = "fastsync.enabled"
  }
}

private class MakeProjectIntransitiveAction : Action<Project> {
  override fun execute(project: Project) {
    project.configurations.configureEach { configuration ->
      if (configuration.isCanBeResolved && configuration.isRuntimeClasspath) {
        configuration.isTransitive = false
        // Use compile classpath for version information to help resolve dependencies from BOMs
        configuration.shouldResolveConsistentlyWith(
          project.configurations.getAt(configuration.matchingCompileClasspathName)
        )
      }
    }
  }
}

private val Settings.isIdeSync: Boolean
  get() = providers.systemProperty("idea.sync.active").getOrElse("false").toBoolean()

private val Settings.isFastSyncEnabled: Boolean
  get() = providers.gradleProperty(FASTSYNC_ENABLED_PROPERTY).getOrElse("true").toBoolean()

private const val RUNTIME_CLASSPATH = "runtimeClasspath"

private val Configuration.isRuntimeClasspath: Boolean
  get() = name.contains(RUNTIME_CLASSPATH, true)

private val Configuration.matchingCompileClasspathName: String
  get() = when (name) {
    RUNTIME_CLASSPATH -> "compileClasspath"
    else -> "${name.removeSuffix(RUNTIME_CLASSPATH)}CompileClasspath"
  }