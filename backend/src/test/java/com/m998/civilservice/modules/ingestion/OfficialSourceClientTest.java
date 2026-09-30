package com.m998.civilservice.modules.ingestion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class OfficialSourceClientTest {
    private final OfficialSourceClient client = new OfficialSourceClient("bm.scs.gov.cn,scs.gov.cn", "https://bm.scs.gov.cn/kl2026");

    @Test
    void acceptsOnlyAllowlistedHttpsHost() {
        assertEquals("bm.scs.gov.cn", client.validate("https://bm.scs.gov.cn/kl2026/a.xlsx").getHost());
        assertThrows(IllegalArgumentException.class, () -> client.validate("http://bm.scs.gov.cn/a.xlsx"));
        assertThrows(IllegalArgumentException.class, () -> client.validate("https://bm.scs.gov.cn.evil.example/a.xlsx"));
        assertThrows(IllegalArgumentException.class, () -> client.validate("https://127.0.0.1/a.xlsx"));
        assertThrows(IllegalArgumentException.class, () -> client.validate("https://bm.scs.gov.cn:8080/a.xlsx"));
    }

    @Test
    void extractsSingleWorkbookButRejectsAmbiguousZip() throws Exception {
        byte[] workbook = new byte[]{'P', 'K', 3, 4, 1};
        assertArrayEquals(workbook, client.extractWorkbook("https://bm.scs.gov.cn/a.zip", zip(1, workbook)));
        assertThrows(IllegalArgumentException.class,
                () -> client.extractWorkbook("https://bm.scs.gov.cn/a.zip", zip(2, workbook)));
    }

    private byte[] zip(int excelFiles, byte[] workbook) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < excelFiles; i++) {
                zip.putNextEntry(new ZipEntry("position" + i + ".xlsx"));
                zip.write(workbook);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
