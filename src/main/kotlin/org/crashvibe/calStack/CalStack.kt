package org.crashvibe.calStack

import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.crashvibe.calStack.config.Config
import java.util.*


class CalStack : JavaPlugin() {
  override fun onLoad() {
    instance = this
    Metrics(this, 28779)
  }

  override fun onEnable() {
    Config.init(dataFolder.toPath())
    Objects.requireNonNull(getCommand("calculate"))?.setExecutor(Command())
  }

  override fun onDisable() {
  }

  companion object {
    lateinit var instance: CalStack
      private set
  }
}
