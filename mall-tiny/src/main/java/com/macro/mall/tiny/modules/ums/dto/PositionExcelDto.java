package com.macro.mall.tiny.modules.ums.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入岗位 DTO
 * Created by macro on 2026-04-03.
 */
@Data
public class PositionExcelDto {

    @ExcelProperty(value = "部门", index = 0)
    private String department;

    @ExcelProperty(value = "职位名称", index = 1)
    private String positionName;

    @ExcelProperty(value = "专业要求", index = 2)
    private String majorRequired;

    @ExcelProperty(value = "学历要求", index = 3)
    private String educationRequired;

    @ExcelProperty(value = "政治面貌要求", index = 4)
    private String politicalStatusRequired;

    @ExcelProperty(value = "是否限应届", index = 5)
    private String isFreshOnly;

    @ExcelProperty(value = "招录人数", index = 6)
    private String recruitmentNumber;

    @ExcelProperty(value = "年份", index = 7)
    private String year;
}
