package com.example.calcStack;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class CalcStackPlugin extends JavaPlugin {

    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // 生成默认 config.yml

        // 初始化语言文件
        createLangFile();

        // 注册指令
        Objects.requireNonNull(getCommand("calculate")).setExecutor(new CalculateCommand(this));
    }

    @Override
    public void onDisable() {
        // 插件关闭逻辑
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    private void createLangFile() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

}