package org.crashvibe.calStack

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.crashvibe.calStack.config.Config
import java.util.*
import java.util.stream.Collectors
import kotlin.math.ceil


data class StackInfo(
  val totalStacks: Int,
  val remainingItems: Int,
  val chests: Int,
  val maxStackSize: Int
)

class Command : TabExecutor {
  override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
    if (sender !is Player) {
      sender.sendMessage(Config.langData.errors.not_player)
      return true
    }

    val (material, quantity) = try {
      when (args.size) {
        1 -> {
          val itemInHand = sender.inventory.itemInMainHand
          if (itemInHand.type == Material.AIR) {
            sender.sendMessage(Config.langData.errors.no_item_in_hand)
            return true
          }
          itemInHand.type to args[0].toInt()
        }

        2 -> {
          val matchedMaterial = Material.matchMaterial(args[0])
          if (matchedMaterial == null) {
            sender.sendMessage(Config.langData.errors.invalid_item.replace("{item}", args[0]))
            return true
          }
          matchedMaterial to args[1].toInt()
        }

        else -> {
          sender.sendMessage(Config.langData.usage)
          return true
        }
      }
    } catch (_: NumberFormatException) {
      sender.sendMessage(Config.langData.errors.invalid_number)
      return true
    }

    val chestSize = Config.configData.chest_size

    Bukkit.getScheduler().runTaskAsynchronously(CalStack.instance, Runnable {
      val maxStackSize =
        if (Config.configData.custom_stacksize.enabled) Config.configData.custom_stacksize.value else material.maxStackSize
      val stackInfo = calculateStackInfo(quantity, maxStackSize, chestSize)

      val materialDetails = getCraftingMaterials(material, quantity, maxStackSize, chestSize)
      Bukkit.getScheduler().runTask(CalStack.instance, Runnable {
        val results = Config.langData.results
        sender.sendMessage(results.header)
        sender.sendMessage(formatResultMessage(material.name.lowercase(Locale.getDefault()), getTranslatedItemName(material), stackInfo, results.item))
        sender.sendMessage(results.quantity.replace("{quantity}", quantity.toString()))
        sender.sendMessage(formatResultMessage(material.name.lowercase(), "", stackInfo, results.stacks))
        sender.sendMessage(results.remaining.replace("{remaining}", stackInfo.remainingItems.toString()))
        sender.sendMessage(results.chests.replace("{chests}", stackInfo.chests.toString()).replace("{chest_size}", chestSize.toString()))

        if (materialDetails.isNotEmpty()) {
          sender.sendMessage(results.ingredients.header)
          materialDetails.forEach(sender::sendMessage)
        }
        sender.sendMessage(results.footer)
      })
    }
    )

    return true
  }

  override fun onTabComplete(
    sender: CommandSender,
    command: Command,
    alias: String,
    args: Array<String>
  ): List<String?>? {
    if (args.size == 1) {
      return Arrays.stream(Material.entries.toTypedArray())
        .map(Material::name)
        .map<String> { obj: String? -> obj!!.lowercase(Locale.getDefault()) }
        .filter { name: String? -> name!!.startsWith(args[0].lowercase()) }
        .collect(Collectors.toList())
    }
    return Collections.emptyList<String>()
  }
}


fun calculateChests(totalStacks: Int, remainingItems: Int, chestSize: Int): Int {
  return (totalStacks + (if (remainingItems > 0) 1 else 0) + chestSize - 1) / chestSize
}

fun calculateStackInfo(quantity: Int, maxStackSize: Int, chestSize: Int): StackInfo {
  val totalStacks = quantity / maxStackSize
  val remainingItems = quantity % maxStackSize
  return StackInfo(totalStacks, remainingItems, calculateChests(totalStacks, remainingItems, chestSize), maxStackSize)
}

fun getIngredientsFromRecipe(recipe: Recipe): MutableList<ItemStack> {
  return when (recipe) {
    is ShapedRecipe -> recipe.choiceMap.values.mapNotNull { choice ->
      getRepresentativeItem(choice)
    }.toMutableList()

    is ShapelessRecipe -> recipe.choiceList.mapNotNull { choice ->
      getRepresentativeItem(choice)
    }.toMutableList()

    else -> mutableListOf()
  }
}

fun getRepresentativeItem(choice: org.bukkit.inventory.RecipeChoice): ItemStack? {
  return when (choice) {
    is org.bukkit.inventory.RecipeChoice.MaterialChoice -> {
      choice.choices.firstOrNull()?.let { material ->
        ItemStack(material, 1)
      }
    }

    is org.bukkit.inventory.RecipeChoice.ExactChoice -> {
      choice.choices.firstOrNull()
    }

    else -> null
  }
}


fun getRecipesFor(result: ItemStack): MutableList<Recipe> {
  return Bukkit.getRecipesFor(result)
}


fun getCraftingMaterials(
  material: Material,
  quantity: Int,
  maxStackSize: Int,
  chestSize: Int
): List<Component> {
  val result = ItemStack(material)
  val recipes = getRecipesFor(result)

  if (recipes.isEmpty()) return emptyList()

  val materialMap = EnumMap<Material, Int>(Material::class.java)

  recipes.forEach { recipe ->
    val output = recipe.result
    val outputAmount = output.amount.takeIf { it > 0 } ?: return@forEach
    val multiplier = ceil(quantity.toDouble() / outputAmount).toInt()

    getIngredientsFromRecipe(recipe)
      .filter { it.type != Material.AIR }
      .forEach { ingredient ->
        val needed = ingredient.amount * multiplier
        materialMap.merge(ingredient.type, needed, Int::plus)
      }
  }

  return materialMap.map { (mat, qty) ->
    formatMaterialComponent(mat, calculateStackInfo(qty, maxStackSize, chestSize))
  }
}


fun formatResultMessage(
  ingredient: String,
  translatedName: String = "",
  stackInfo: StackInfo,
  template: String
): String {
  return template
    .replace("{item}", ingredient)
    .replace("{ingredient}", ingredient)
    .replace("{quantity}", stackInfo.totalStacks.toString())
    .replace("{stacks}", stackInfo.totalStacks.toString())
    .replace("{stack_size}", stackInfo.maxStackSize.toString())
    .replace("{remaining}", stackInfo.remainingItems.toString())
    .replace("{chests}", stackInfo.chests.toString())
    .replace("{chest_size}", Config.configData.chest_size.toString())
    .let { if (translatedName.isNotEmpty()) it.replace("{translated_name}", translatedName) else it }
}

fun formatMaterialComponent(material: Material, stackInfo: StackInfo): Component {
  val translated = material.itemTranslationKey?.let { Component.translatable(it) }
    ?: Component.text(material.name.lowercase(Locale.getDefault()))
  val template = Config.langData.results.ingredients.item
    .replace("{ingredient}", material.name.lowercase(Locale.getDefault()))
    .replace("{quantity}", stackInfo.totalStacks.toString())
    .replace("{stacks}", stackInfo.totalStacks.toString())
    .replace("{stack_size}", stackInfo.maxStackSize.toString())
    .replace("{remaining}", stackInfo.remainingItems.toString())
    .replace("{chests}", stackInfo.chests.toString())
    .replace("{chest_size}", Config.configData.chest_size.toString())
  return Component.text(template).replaceText { builder ->
    builder.matchLiteral("{translated_name}").replacement(translated)
  }
}


private fun getTranslatedItemName(material: Material): String {
  return material.itemTranslationKey ?: material.name.lowercase(Locale.getDefault())
}
