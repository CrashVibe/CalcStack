package org.crashvibe.CalcStack.config

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
    var item: String = "§b物品: §r{item}§b(§r{translated_name}§b)",
    var quantity: String = "§b数量: §r{quantity}",
    var stacks: String = "§b堆叠: §r{stacks} §b组 (§r{stack_size} §b个/组)",
    var remaining: String = "§b剩余: §r{remaining} §b个",
    var chests: String = "§b需要箱子: §r{chests} §b个 (每个箱子 §r{chest_size} §b格)",
    var ingredients: Ingredients = Ingredients(),
    var footer: String = "§a====================",
  ) {
    @Configuration
    data class Ingredients(
      var header: String = "§a===== 原材料 =====",
      var item: String = "§b- §r{ingredient}§b(§r{translated_name}§b): §r{quantity} §b个",
      var stacks: String = "  §7- §r{stacks} §7组 (§r{stack_size} §7个/组)",
      var remaining: String = "  §7- 剩余 §r{remaining} §7个",
      var chests: String = "  §7- 需要箱子: §r{chests} §7个 (每个箱子 §r{chest_size} §7格)",
    )
  }
}
