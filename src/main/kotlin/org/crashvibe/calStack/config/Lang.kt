package org.crashvibe.calStack.config

import de.exlll.configlib.Configuration

@Configuration
data class Lang(
  var usage: String = "§e用法: /calculate <数量> 或 /calculate <物品ID> <数量>",
  var errors: Errors = Errors(),
  var results: Results = Results(),
) {
  @Configuration
  data class Errors(
    var not_player: String = "§c该指令只能由玩家使用！",
    var no_item_in_hand: String = "§c请手持一个有效的物品，或在指令中指定物品 ID！",
    var invalid_item: String = "§c无效的物品 ID: {item}",
    var invalid_number: String = "§c请输入有效的数量！"
  )

  @Configuration
  data class Results(
    var header: String = "§a===== 计算结果 =====",
    var item: String = "§b物品: {item}({translated_name})",
    var quantity: String = "§b数量: {quantity}",
    var stacks: String = "§b堆叠: {stacks} 组 ({stack_size} 个/组)",
    var remaining: String = "§b剩余: {remaining} 个",
    var chests: String = "§b需要箱子: {chests} 个 (每个箱子 {chest_size} 格)",
    var footer: String = "§a====================",
    var ingredients: Ingredients = Ingredients(),
  ) {
    @Configuration
    data class Ingredients(
      var header: String = "§a===== 原材料 =====",
      var item: String = "§b- {ingredient}({translated_name}): {quantity} 个",
      var stacks: String = "  §7- {stacks} 组 ({stack_size} 个/组)",
      var remaining: String = "  §7- 剩余 {remaining} 个",
      var chests: String = "  §7- 需要箱子: {chests} 个 (每个箱子 {chest_size} 格)",
    )
  }
}
