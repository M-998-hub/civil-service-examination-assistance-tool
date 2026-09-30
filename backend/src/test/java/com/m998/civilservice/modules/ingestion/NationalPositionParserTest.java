package com.m998.civilservice.modules.ingestion;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.modules.position.model.Position;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NationalPositionParserTest {
    private final NationalPositionParser parser = new NationalPositionParser(
            new NationalHeaderResolver(new ObjectMapper(), "", "https://example.invalid", "test"));

    @Test
    void parsesOfficialColumnsWithoutAi() {
        List<List<String>> head = Arrays.asList(
                Arrays.asList("招录机关"), Arrays.asList("职位代码"), Arrays.asList("职位名称"),
                Arrays.asList("专业要求"), Arrays.asList("学历要求"), Arrays.asList("政治面貌"),
                Arrays.asList("招考人数"), Arrays.asList("招录考生类别"));
        byte[] data = workbook(head, Arrays.asList(Arrays.asList("国家税务局", "300110001001", "一级行政执法员", "计算机", "本科", "不限", "2", "2026年应届高校毕业生")));
        List<Position> result = parser.parse(new ByteArrayInputStream(data), 2026);
        assertEquals(1, result.size());
        assertEquals("300110001001", result.get(0).getSourcePositionCode());
        assertEquals(2, result.get(0).getRecruitmentNumber());
        assertEquals("NATIONAL_OFFICIAL", result.get(0).getSourceType());
        assertTrue(result.get(0).getIsFreshOnly());
    }

    @Test
    void duplicateCodeFailsClosed() {
        List<List<String>> head = Arrays.asList(
                Arrays.asList("招录机关"), Arrays.asList("职位代码"), Arrays.asList("职位名称"),
                Arrays.asList("专业"), Arrays.asList("学历"), Arrays.asList("政治面貌"), Arrays.asList("招考人数"), Arrays.asList("招录考生类别"));
        List<String> row = Arrays.asList("部门", "123", "岗位", "计算机", "本科", "不限", "1", "不限");
        byte[] data = workbook(head, Arrays.asList(row, row));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new ByteArrayInputStream(data), 2026));
    }

    @Test
    void readsAllPositionSheets() {
        List<List<String>> head = Arrays.asList(
                Arrays.asList("招录机关"), Arrays.asList("职位代码"), Arrays.asList("职位名称"),
                Arrays.asList("专业"), Arrays.asList("学历"), Arrays.asList("政治面貌"),
                Arrays.asList("招考人数"), Arrays.asList("招录考生类别"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out).head(head).autoCloseStream(false).build();
        writer.write(Arrays.asList(Arrays.asList("机关甲", "101", "职位甲", "计算机", "本科", "不限", "1", "不限")), EasyExcel.writerSheet(0).build());
        writer.write(Arrays.asList(Arrays.asList("机关乙", "102", "职位乙", "计算机", "本科", "不限", "1", "不限")), EasyExcel.writerSheet(1).build());
        writer.finish();
        assertEquals(2, parser.parse(new ByteArrayInputStream(out.toByteArray()), 2026).size());
    }

    private byte[] workbook(List<List<String>> head, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).autoCloseStream(false).sheet().doWrite(rows);
        return out.toByteArray();
    }
}
