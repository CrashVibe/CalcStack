package com.example.calcStack;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateCommand implements TabExecutor {

    private final CalcStackPlugin plugin;

    public CalculateCommand(@Nonnull CalcStackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@Nullable CommandSender sender, @Nullable Command command, @Nullable String label, @Nonnull String[] args) {
        // 确保 sender 不为 null
        if (sender == null) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(getLangMessage("error.not_player"));
            return true;
        }

        Material material;
        int quantity;

        try {
            if (args.length == 1) {
                // 使用玩家手上的物品
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    sender.sendMessage(getLangMessage("error.no_item_in_hand"));
                    return true;
                }
                material = itemInHand.getType();
                quantity = Integer.parseInt(args[0]);
            } else if (args.length == 2) {
                // 指定物品 ID 和数量
                material = Material.matchMaterial(args[0]);
                if (material == null) {
                    sender.sendMessage(getLangMessage("error.invalid_item").replace("{item}", args[0]));
                    return true;
                }
                quantity = Integer.parseInt(args[1]);
            } else {
                sender.sendMessage(getLangMessage("usage"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(getLangMessage("error.invalid_number"));
            return true;
        }

        // 异步处理合成配方和计算
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // 获取配置的箱子大小和最大堆叠数量
            int maxStackSize = material.getMaxStackSize();
            if (plugin.getConfig().getBoolean("custom-stack-size.enabled")) {
                maxStackSize = plugin.getConfig().getInt("custom-stack-size.value", maxStackSize);
            }
            int chestSize = plugin.getConfig().getInt("chest-size", 27);

            // 计算组和余数
            int totalStacks = quantity / maxStackSize;
            int remainingItems = quantity % maxStackSize;

            // 计算需要的箱子数量
            int chests = (totalStacks + (remainingItems > 0 ? 1 : 0) + chestSize - 1) / chestSize;

            // 获取物品的合成原材料数量
            List<String> materialDetails = getCraftingMaterials(material, quantity);

            // 回到主线程发送消息
            int finalMaxStackSize = maxStackSize;
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(getLangMessage("result.header"));
                sender.sendMessage(getLangMessage("result.item").replace("{item}", material.name().toLowerCase()));
                sender.sendMessage(getLangMessage("result.quantity").replace("{quantity}", String.valueOf(quantity)));
                sender.sendMessage(getLangMessage("result.stacks").replace("{stacks}", String.valueOf(totalStacks))
                        .replace("{stack_size}", String.valueOf(finalMaxStackSize)));
                sender.sendMessage(getLangMessage("result.remaining").replace("{remaining}", String.valueOf(remainingItems)));
                sender.sendMessage(getLangMessage("result.chests").replace("{chests}", String.valueOf(chests))
                        .replace("{chest_size}", String.valueOf(chestSize)));

                if (!materialDetails.isEmpty()) {
                    sender.sendMessage(getLangMessage("result.ingredients.header"));
                    for (String detail : materialDetails) {
                        sender.sendMessage(detail);
                    }
                }

                sender.sendMessage(getLangMessage("result.footer"));
            });
        });

        return true;
    }

    /**
     * 获取物品的合成原材料及数量，数量将根据目标物品数量进行加倍
     *
     * @param material 物品
     * @param quantity 物品数量
     * @return 返回物品的所有原材料及数量
     */
    private List<String> getCraftingMaterials(Material material, int quantity) {
        Map<Material, Integer> materialMap = new HashMap<>();  // 用于合并重复的原材料
        List<String> materialDetails = new ArrayList<>();
        ItemStack result = new ItemStack(material);

        // 获取该物品的所有配方
        List<Recipe> recipes = getRecipesFor(result);

        if (recipes.isEmpty()) {
            return materialDetails;  // 如果没有配方，返回空列表
        }

        // 遍历所有的合成配方
        for (Recipe recipe : recipes) {
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                Map<Character, ItemStack> ingredients = shapedRecipe.getIngredientMap();
                for (ItemStack ingredient : ingredients.values()) {
                    // 添加null检查
                    if (ingredient != null && ingredient.getType() != Material.AIR) {
                        // 计算该原材料的数量
                        materialMap.put(ingredient.getType(), materialMap.getOrDefault(ingredient.getType(), 0) + ingredient.getAmount() * quantity);
                    }
                }
            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                for (ItemStack ingredient : shapelessRecipe.getIngredientList()) {
                    // 添加null检查
                    if (ingredient != null && ingredient.getType() != Material.AIR) {
                        // 计算该原材料的数量
                        materialMap.put(ingredient.getType(), materialMap.getOrDefault(ingredient.getType(), 0) + ingredient.getAmount() * quantity);
                    }
                }
            }
        }

        // 格式化输出原材料数量，并计算组、余数、箱
        for (Map.Entry<Material, Integer> entry : materialMap.entrySet()) {
            Material ingredientType = entry.getKey();
            int totalQuantity = entry.getValue();

            // 计算堆叠数量、组、余数、箱
            int maxStackSize = ingredientType.getMaxStackSize();
            int totalStacks = totalQuantity / maxStackSize;
            int remainingItems = totalQuantity % maxStackSize;
            int chestSize = plugin.getConfig().getInt("chest-size", 27);
            int chests = (totalStacks + (remainingItems > 0 ? 1 : 0) + chestSize - 1) / chestSize;

            // 根据新的语言文件路径输出原材料信息
            String detail = getLangMessage("result.ingredients.item")
                    .replace("{ingredient}", ingredientType.name().toLowerCase())
                    .replace("{quantity}", String.valueOf(totalQuantity));

            // 输出组、余数、箱的信息
            materialDetails.add(detail);
            materialDetails.add(getLangMessage("result.ingredients.stacks")
                    .replace("{stacks}", String.valueOf(totalStacks))
                    .replace("{stack_size}", String.valueOf(maxStackSize)));
            materialDetails.add(getLangMessage("result.ingredients.remaining")
                    .replace("{remaining}", String.valueOf(remainingItems)));
            materialDetails.add(getLangMessage("result.ingredients.chests")
                    .replace("{chests}", String.valueOf(chests))
                    .replace("{chest_size}", String.valueOf(chestSize)));
        }

        return materialDetails;
    }

    /**
     * 使用提供的物品获取所有相关配方
     *
     * @param result 物品
     * @return 返回所有相关配方
     */
    private List<Recipe> getRecipesFor(ItemStack result) {
        // 获取所有配方
        return Bukkit.getRecipesFor(result);
    }

    @Override
    @Nonnull
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (args.length == 1) {
            // 提供所有 Material 名称作为补全选项
            List<String> completions = new ArrayList<>();
            for (Material material : Material.values()) {
                if (material.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(material.name().toLowerCase());
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }

    @Nonnull
    private String getLangMessage(@Nonnull String path) {
        String message = plugin.getLangConfig().getString(path);
        return message != null ? message : "§c配置错误: 未找到语言文件内容 (" + path + ")！";
    }
}