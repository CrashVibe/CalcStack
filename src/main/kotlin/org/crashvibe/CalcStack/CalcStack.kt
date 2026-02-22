package org.crashvibe.CalcStack

import com.tcoded.folialib.FoliaLib
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.crashvibe.CalcStack.config.Config
import java.util.*


class CalcStack : JavaPlugin() {
  override fun onLoad() {
    instance = this
    folialLib = FoliaLib(this)
    Metrics(this, 28779)
  }

  override fun onEnable() {
    Config.init(dataFolder.toPath())
    Objects.requireNonNull(getCommand("calculate"))?.setExecutor(Command())
  }

  override fun onDisable() {
  }

  companion object {
    lateinit var instance: CalcStack
      private set
    lateinit var folialLib: FoliaLib
      private set
  }
}
