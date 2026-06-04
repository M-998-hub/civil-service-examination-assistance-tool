package com.macro.mall.tiny.modules.ums.strategy;

import com.macro.mall.tiny.modules.ums.dto.ImportFieldMeta;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据导入策略接口
 * 每种导入类型实现此接口，通过策略模式分发
 *
 * @param <T> 导入的实体类型
 */
public interface ImportStrategy<T> {

    /**
     * 获取策略对应的导入类型
     */
    ImportTypeEnum getImportType();

    /**
     * 获取该类型支持的目标字段元数据列表（前端列映射下拉框选项）
     */
    List<ImportFieldMeta> getFieldMetas();

    /**
     * 根据Excel行数据 + 列映射 构建实体对象
     *
     * @param rowData     Excel行数据（key=列索引, value=单元格值）
     * @param mapping     列映射关系（key=字段名, value=Excel列索引）
     * @param extraParams 额外参数（如 year 等）
     * @return 构建的实体对象
     */
    T buildEntity(Map<Integer, String> rowData, Map<String, Object> mapping, Map<String, Object> extraParams);

    /**
     * 校验实体数据
     *
     * @param entity 实体对象
     * @param rowNum 行号（用于错误提示）
     * @return null 表示校验通过，否则返回错误信息
     */
    String validate(T entity, int rowNum);

    /**
     * 生成去重唯一键
     *
     * @param entity 实体对象
     * @return 唯一键字符串
     */
    String getDuplicateKey(T entity);

    /**
     * 获取数据库中已有数据的唯一键集合（用于去重）
     *
     * @param extraParams 额外参数（如 year 等）
     * @return 已有数据的唯一键集合
     */
    Set<String> getExistingKeys(Map<String, Object> extraParams);

    /**
     * 批量保存实体
     *
     * @param entityList 实体列表
     */
    void saveBatch(List<T> entityList);
}
