package com.m998.civilservice.modules.ingestion;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.m998.civilservice.modules.position.model.Position;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

/** Parses only facts present in the official workbook. AI may suggest column indexes, never values. */
@Component
public class NationalPositionParser {
    private final NationalHeaderResolver headerResolver;

    public NationalPositionParser(NationalHeaderResolver headerResolver) {
        this.headerResolver = headerResolver;
    }

    public List<Position> parse(InputStream input, int year) {
        Map<Integer, List<Map<Integer, String>>> sheets = new LinkedHashMap<>();
        EasyExcel.read(input, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                int sheetNo = context.readSheetHolder().getSheetNo();
                sheets.computeIfAbsent(sheetNo, ignored -> new ArrayList<>()).add(new HashMap<>(row));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) { }
        }).headRowNumber(0).doReadAll();
        if (sheets.isEmpty()) throw new IllegalArgumentException("官方表格为空");

        List<Position> result = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        List<Integer> unrecognizedLargeSheets = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<Integer, String>>> sheet : sheets.entrySet()) {
            List<Position> parsed = parseSheet(sheet.getValue(), year, sheet.getKey(), seenCodes, false);
            if (parsed == null) {
                if (sheet.getValue().size() > 20) unrecognizedLargeSheets.add(sheet.getKey());
            } else result.addAll(parsed);
        }
        if (result.isEmpty()) {
            List<Map<Integer, String>> first = sheets.values().iterator().next();
            int firstSheetNo = sheets.keySet().iterator().next();
            List<Position> fallback = parseSheet(first, year, firstSheetNo, seenCodes, true);
            if (fallback != null) {
                result.addAll(fallback);
                unrecognizedLargeSheets.remove(Integer.valueOf(firstSheetNo));
            }
        }
        if (!unrecognizedLargeSheets.isEmpty()) throw new IllegalArgumentException("第 " + (unrecognizedLargeSheets.get(0) + 1) + " 个工作表较大但无法识别表头，拒绝部分导入");
        if (result.isEmpty()) throw new IllegalArgumentException("未解析到有效职位行");
        return result;
    }

    private List<Position> parseSheet(List<Map<Integer, String>> rows, int year, int sheetNo,
            Set<String> seenCodes, boolean allowAiFallback) {
        int headerIndex = -1;
        Map<String, Integer> columns = null;
        for (int i = 0; i < Math.min(20, rows.size()); i++) {
            Map<String, Integer> found = headerResolver.resolveDeterministically(rows.get(i));
            if (found.containsKey("code") && found.containsKey("name") && found.containsKey("department")) {
                headerIndex = i;
                columns = found;
                break;
            }
        }
        if (headerIndex < 0) {
            if (!allowAiFallback) return null;
            headerIndex = Math.min(2, rows.size() - 1);
            columns = headerResolver.resolveWithAi(rows.get(headerIndex));
        } else if (!columns.keySet().containsAll(Arrays.asList("code", "department", "name", "education", "major", "political", "fresh", "count"))) {
            for (Map.Entry<String, Integer> entry : headerResolver.resolveWithAi(rows.get(headerIndex)).entrySet()) {
                columns.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        for (String required : Arrays.asList("code", "department", "name", "education", "major", "political", "fresh", "count")) {
            if (!columns.containsKey(required)) throw new IllegalArgumentException("无法可靠识别列: " + required);
        }

        List<Position> result = new ArrayList<>();
        for (int i = headerIndex + 1; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            String code = value(row, columns, "code");
            if (code.isEmpty()) continue;
            if (code.contains("职位代码") || code.contains("岗位代码")) continue;
            String department = value(row, columns, "department");
            String name = value(row, columns, "name");
            if (department.isEmpty() || name.isEmpty()) throw new IllegalArgumentException("工作表 " + (sheetNo + 1) + " 第 " + (i + 1) + " 行缺少部门或职位名称");
            if (!seenCodes.add(code)) throw new IllegalArgumentException("官方表格包含重复职位代码: " + code);
            String countText = value(row, columns, "count");
            int count;
            try { count = Integer.parseInt(countText); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("第 " + (i + 1) + " 行招考人数无效: " + countText); }
            if (count <= 0) throw new IllegalArgumentException("第 " + (i + 1) + " 行招考人数必须大于零");

            Position position = new Position();
            position.setYear(year);
            position.setSourceType("NATIONAL_OFFICIAL");
            position.setSourcePositionCode(code);
            position.setDepartment(department);
            position.setPositionName(name);
            position.setMajorRequired(value(row, columns, "major"));
            position.setEducationRequired(value(row, columns, "education"));
            position.setPoliticalStatusRequired(value(row, columns, "political"));
            String category = value(row, columns, "fresh");
            position.setIsFreshOnly((category.contains("应届") || category.contains("高校毕业生"))
                    && !category.contains("非应届") && !category.contains("不限"));
            position.setRecruitmentNumber(count);
            position.setRecruitmentStatus("ACTIVE");
            position.setStatus(0);
            result.add(position);
        }
        return result;
    }

    private String value(Map<Integer, String> row, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        String value = index == null ? null : row.get(index);
        return value == null ? "" : value.trim();
    }
}
