package org.crashvibe.calStack.config

import de.exlll.configlib.YamlConfigurations
import java.nio.file.Path

object Config {
  lateinit var configData: PluginConfig
    private set
  lateinit var langData: Lang
    private set

  fun init(configFile: Path) {
    configData = YamlConfigurations.update(
      configFile.resolve("config.yml"),
      PluginConfig::class.java,
    )
    langData = YamlConfigurations.update(
      configFile.resolve("lang.yml"),
      Lang::class.java,
    )
  }
}
