package com.macro.mall.tiny.generator;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.po.LikeTable;
import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * MyBatisPlus代码生成器
 * Created by macro on 2020/8/20.
 */
public class MyBatisPlusGenerator {

    // 要生成的表名
    private static final String[] TABLE_NAMES = {
            "user_archive", "position", "position_stats", "favorite", "prediction_log"
    };

    // 模块名
    private static final String MODULE_NAME = "ums";

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");

        // 代码生成器
        AutoGenerator autoGenerator = new AutoGenerator(initDataSourceConfig());
        autoGenerator.global(initGlobalConfig(projectPath));
        autoGenerator.packageInfo(initPackageConfig(projectPath, MODULE_NAME));
        autoGenerator.injection(initInjectionConfig(projectPath, MODULE_NAME));
        autoGenerator.template(initTemplateConfig());
        autoGenerator.strategy(initStrategyConfig(TABLE_NAMES));
        autoGenerator.execute(new VelocityTemplateEngine());

        System.out.println("代码生成完成！生成的表：" + String.join(", ", TABLE_NAMES));
    }

    /**
     * 从application-dev.yml读取数据源配置
     */
    private static DataSourceConfig initDataSourceConfig() {
        Map<String, Object> yamlConfig = loadYamlConfig("application-dev.yml");

        @SuppressWarnings("unchecked")
        Map<String, Object> spring = (Map<String, Object>) yamlConfig.get("spring");
        @SuppressWarnings("unchecked")
        Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");

        String url = (String) datasource.get("url");
        String username = (String) datasource.get("username");
        String password = (String) datasource.get("password");

        System.out.println("使用数据库配置：" + url);

        return new DataSourceConfig.Builder(url, username, password)
                .dbQuery(new MySqlQuery())
                .build();
    }

    /**
     * 加载YAML配置文件
     */
    private static Map<String, Object> loadYamlConfig(String yamlFile) {
        try (InputStream inputStream = MyBatisPlusGenerator.class.getClassLoader().getResourceAsStream(yamlFile)) {
            if (inputStream == null) {
                throw new RuntimeException("找不到配置文件：" + yamlFile);
            }
            Yaml yaml = new Yaml();
            return yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件失败：" + yamlFile, e);
        }
    }

    /**
     * 初始化全局配置
     */
    private static GlobalConfig initGlobalConfig(String projectPath) {
        return new GlobalConfig.Builder()
                .outputDir(projectPath + "/src/main/java")
                .author("macro")
                .disableOpenDir()
                .enableSwagger()
                .fileOverride()
                .dateType(DateType.ONLY_DATE)
                .build();
    }

    /**
     * 初始化包配置 - 输出到标准包路径
     */
    private static PackageConfig initPackageConfig(String projectPath, String moduleName) {
        String basePackage = "com.macro.mall.tiny.modules";
        String modulePackage = basePackage + "." + moduleName;

        // 配置各层代码输出路径
        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.mapperXml, projectPath + "/src/main/resources/mapper/" + moduleName);
        pathInfo.put(OutputFile.controller, projectPath + "/src/main/java/com/macro/mall/tiny/modules/" + moduleName + "/controller");
        pathInfo.put(OutputFile.service, projectPath + "/src/main/java/com/macro/mall/tiny/modules/" + moduleName + "/service");
        pathInfo.put(OutputFile.serviceImpl, projectPath + "/src/main/java/com/macro/mall/tiny/modules/" + moduleName + "/service/impl");
        pathInfo.put(OutputFile.mapper, projectPath + "/src/main/java/com/macro/mall/tiny/modules/" + moduleName + "/mapper");
        pathInfo.put(OutputFile.entity, projectPath + "/src/main/java/com/macro/mall/tiny/modules/" + moduleName + "/model");

        return new PackageConfig.Builder()
                .moduleName(moduleName)
                .parent(basePackage)
                .entity("model")
                .pathInfo(pathInfo)
                .build();
    }

    /**
     * 初始化模板配置 - 使用自定义Controller模板
     */
    private static TemplateConfig initTemplateConfig() {
        return new TemplateConfig.Builder()
                .controller("/templates/controller.java.vm")
                .build();
    }

    /**
     * 初始化策略配置
     */
    private static StrategyConfig initStrategyConfig(String[] tableNames) {
        StrategyConfig.Builder builder = new StrategyConfig.Builder();
        builder.entityBuilder()
                .naming(NamingStrategy.underline_to_camel)
                .columnNaming(NamingStrategy.underline_to_camel)
                .enableLombok()
                .formatFileName("%s")
                .mapperBuilder()
                .enableBaseResultMap()
                .formatMapperFileName("%sMapper")
                .formatXmlFileName("%sMapper")
                .serviceBuilder()
                .formatServiceFileName("%sService")
                .formatServiceImplFileName("%sServiceImpl")
                .controllerBuilder()
                .enableRestStyle()
                .enableHyphenStyle()
                .formatFileName("%sController");

        // 当表名中带*号时可以启用通配符模式
        if (tableNames.length == 1 && tableNames[0].contains("*")) {
            String[] likeStr = tableNames[0].split("_");
            String likePrefix = likeStr[0] + "_";
            builder.likeTable(new LikeTable(likePrefix));
        } else {
            builder.addInclude(tableNames);
        }
        return builder.build();
    }

    /**
     * 初始化自定义配置
     */
    private static InjectionConfig initInjectionConfig(String projectPath, String moduleName) {
        // 自定义配置
        return new InjectionConfig.Builder().build();
    }

}