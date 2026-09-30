package com.m998.civilservice.modules.ingestion;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NationalIngestionWorkerTest {
    @TempDir Path archive;

    @Test
    void stagesDifferenceAndTreatsSameDocumentAsUnchanged() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:workertest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE", "sa", ""));
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE ingestion_run(id BIGINT PRIMARY KEY, `year` INT, source_url VARCHAR(2048), document_id BIGINT, state VARCHAR(30), message VARCHAR(1000), candidate_count INT, finished_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE ingestion_document(id BIGINT AUTO_INCREMENT PRIMARY KEY, source_url VARCHAR(2048), sha256 CHAR(64), file_path VARCHAR(1024))");
        jdbc.execute("CREATE TABLE ingestion_candidate(id BIGINT AUTO_INCREMENT PRIMARY KEY, run_id BIGINT, position_code VARCHAR(100), change_type VARCHAR(20), data_json CLOB, previous_json CLOB)");
        jdbc.update("INSERT INTO ingestion_run(id,`year`,state) VALUES(1,2026,'QUEUED')");
        jdbc.update("INSERT INTO ingestion_run(id,`year`,state) VALUES(2,2026,'QUEUED')");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(Arrays.asList(
                Collections.singletonList("招录机关"), Collections.singletonList("职位代码"),
                Collections.singletonList("职位名称"), Collections.singletonList("专业"),
                Collections.singletonList("学历"), Collections.singletonList("政治面貌"),
                Collections.singletonList("招考人数"), Collections.singletonList("招录考生类别")))
                .autoCloseStream(false).sheet().doWrite(Collections.singletonList(
                        Arrays.asList("某机关", "123", "某岗位", "计算机", "本科", "不限", "1", "不限")));
        String url = "https://bm.scs.gov.cn/kl2026/positions.xlsx";
        OfficialSourceClient source = mock(OfficialSourceClient.class);
        when(source.validate(url)).thenReturn(URI.create(url));
        when(source.download(url)).thenReturn(out.toByteArray());
        when(source.extractWorkbook(eq(url), any(byte[].class))).thenReturn(out.toByteArray());
        PositionService positions = mock(PositionService.class);
        when(positions.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.<Position>emptyList());
        NationalPositionParser parser = new NationalPositionParser(new NationalHeaderResolver(new ObjectMapper(), "", "", ""));
        NationalIngestionWorker worker = new NationalIngestionWorker(jdbc, source, parser, positions, new ObjectMapper(), archive.toString());

        worker.process(1, url, 2026);
        assertEquals("READY", jdbc.queryForObject("SELECT state FROM ingestion_run WHERE id=1", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_candidate WHERE run_id=1", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_document", Integer.class));

        worker.process(2, url, 2026);
        assertEquals("UNCHANGED", jdbc.queryForObject("SELECT state FROM ingestion_run WHERE id=2", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_document", Integer.class));
    }
}
