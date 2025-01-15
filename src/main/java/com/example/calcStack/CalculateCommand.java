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
import me.rubix327.itemslangapi.ItemsLangAPI;
import me.rubix327.itemslangapi.Lang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class CalculateCommand implements TabExecutor {

    private final CalcStackPlugin plugin;
    private final int chestSize;
    private final boolean customStackSizeEnabled;
    private final int customStackSize;

    public CalculateCommand(@Nonnull CalcStackPlugin plugin) {
        this.plugin = plugin;
        this.chestSize = plugin.getConfig().getInt("chest-size", 27);
        this.customStackSizeEnabled = plugin.getConfig().getBoolean("custom-stack-size.enabled", false);
        this.customStackSize = plugin.getConfig().getInt("custom-stack-size.value", 64);
    }

    @Override
    public boolean onCommand(@Nullable CommandSender sender, @Nullable Command command, @Nullable String label, @Nonnull String[] args) {
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
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    sender.sendMessage(getLangMessage("error.no_item_in_hand"));
                    return true;
                }
                material = itemInHand.getType();
                quantity = Integer.parseInt(args[0]);
            } else if (args.length == 2) {
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int maxStackSize = customStackSizeEnabled ? customStackSize : material.getMaxStackSize();
            int totalStacks = quantity / maxStackSize;
            int remainingItems = quantity % maxStackSize;
            int chests = calculateChests(totalStacks, remainingItems, chestSize);

            List<String> materialDetails = getCraftingMaterials(material, quantity, maxStackSize, chestSize);

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(getLangMessage("result.header"));
                sender.sendMessage(getLangMessage("result.item").replace("{item}", material.name().toLowerCase()).replace("{translated_name}", getTranslatedItemName(material)));
                sender.sendMessage(getLangMessage("result.quantity").replace("{quantity}", String.valueOf(quantity)));
                sender.sendMessage(getLangMessage("result.stacks").replace("{stacks}", String.valueOf(totalStacks))
                        .replace("{stack_size}", String.valueOf(maxStackSize)));
                sender.sendMessage(getLangMessage("result.remaining").replace("{remaining}", String.valueOf(remainingItems)));
                sender.sendMessage(getLangMessage("result.chests").replace("{chests}", String.valueOf(chests))
                        .replace("{chest_size}", String.valueOf(chestSize)));

                if (!materialDetails.isEmpty()) {
                    sender.sendMessage(getLangMessage("result.ingredients.header"));
                    materialDetails.forEach(sender::sendMessage);
                }

                sender.sendMessage(getLangMessage("result.footer"));
            });
        });

        return true;
    }

    private List<String> getCraftingMaterials(Material material, int quantity, int maxStackSize, int chestSize) {
        Map<Material, Integer> materialMap = new EnumMap<>(Material.class);
        ItemStack result = new ItemStack(material);
        List<Recipe> recipes = getRecipesFor(result);

        if (recipes.isEmpty()) {
            return Collections.emptyList();
        }

        for (Recipe recipe : recipes) {
            List<ItemStack> ingredients = getIngredientsFromRecipe(recipe);
            int outputAmount = recipe.getResult().getAmount();

            for (ItemStack ingredient : ingredients) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    int neededAmount = (int) Math.ceil((double) quantity / outputAmount) * ingredient.getAmount();
                    materialMap.merge(ingredient.getType(), neededAmount, Integer::sum);
                }
            }
        }

        return materialMap.entrySet().stream()
                .map(entry -> formatMaterialDetail(entry.getKey(), entry.getValue(), maxStackSize, chestSize))
                .collect(Collectors.toList());
    }

    private String formatMaterialDetail(Material material, int quantity, int maxStackSize, int chestSize) {
        int totalStacks = quantity / maxStackSize;
        int remainingItems = quantity % maxStackSize;
        int chests = calculateChests(totalStacks, remainingItems, chestSize);

        return getLangMessage("result.ingredients.item")
                .replace("{ingredient}", material.name().toLowerCase()).replace("{translated_name}", getTranslatedItemName(material))
                .replace("{quantity}", String.valueOf(quantity))
                + "\n" + getLangMessage("result.ingredients.stacks")
                .replace("{stacks}", String.valueOf(totalStacks))
                .replace("{stack_size}", String.valueOf(maxStackSize))
                + "\n" + getLangMessage("result.ingredients.remaining")
                .replace("{remaining}", String.valueOf(remainingItems))
                + "\n" + getLangMessage("result.ingredients.chests")
                .replace("{chests}", String.valueOf(chests))
                .replace("{chest_size}", String.valueOf(chestSize));
    }

    private int calculateChests(int totalStacks, int remainingItems, int chestSize) {
        return (totalStacks + (remainingItems > 0 ? 1 : 0) + chestSize - 1) / chestSize;
    }

    private List<ItemStack> getIngredientsFromRecipe(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            return new ArrayList<>(shapedRecipe.getIngredientMap().values());
        } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            return new ArrayList<>(shapelessRecipe.getIngredientList());
        }
        return Collections.emptyList();
    }

    private List<Recipe> getRecipesFor(ItemStack result) {
        return Bukkit.getRecipesFor(result);
    }

    @Override
    @Nonnull
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .map(String::toLowerCase)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Nonnull
    private String getLangMessage(@Nonnull String path) {
        String message = plugin.getLangConfig().getString(path);
        return message != null ? message : "§c配置错误: 未找到语言文件内容 (" + path + ")！";
    }

    private String getTranslatedItemName(Material material) {
        String translatedName = ItemsLangAPI.getApi().translate(material, Lang.ZH_CN);
        return translatedName != null ? translatedName : material.name().toLowerCase();
    }


}