package org.crashvibe.CalcStack.config

import de.exlll.configlib.Comment
import de.exlll.configlib.Configuration

/**
 * 插件配置数据结构
 */
@Configuration
data class PluginConfig(
  @Comment("最大堆叠数量设置") var custom_stacksize: StackSize = StackSize(),
  @Comment("单位大小（例如每箱 27）") var chest_size: Int = 27,
) {
  @Configuration
  data class StackSize(
    @Comment("是否开启") var enabled: Boolean = false,
    @Comment("最大堆叠数量") var value: Int = 64,
  )
}
