package com.m998.civilservice.modules.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NationalIngestionServiceTest {
    private JdbcTemplate jdbc;
    private PositionService positions;
    private NationalIngestionService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:ingestiontest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE ingestion_run(id BIGINT PRIMARY KEY, `year` INT, state VARCHAR(30), document_id BIGINT, reviewed_by BIGINT, message VARCHAR(1000), finished_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE ingestion_candidate(id BIGINT PRIMARY KEY, run_id BIGINT, position_code VARCHAR(100), change_type VARCHAR(20), state VARCHAR(20), data_json CLOB, previous_json CLOB, validation_error VARCHAR(1000))");
        jdbc.execute("CREATE TABLE position_revision(id BIGINT AUTO_INCREMENT PRIMARY KEY, position_id BIGINT, run_id BIGINT, change_type VARCHAR(20), before_json CLOB, after_json CLOB)");
        positions = mock(PositionService.class);
        mapper = new ObjectMapper();
        service = new NationalIngestionService(jdbc, mapper, positions, null, false, 2026);
    }

    @Test
    void publishRequiresReadyState() {
        jdbc.update("INSERT INTO ingestion_run(id,`year`,state,document_id) VALUES(1,2026,'FAILED',10)");
        assertThrows(IllegalStateException.class, () -> service.publish(1, 99));
        verifyNoInteractions(positions);
    }

    @Test
    void publishesOnlyAfterExplicitReview() throws Exception {
        Position candidate = new Position();
        candidate.setYear(2026);
        candidate.setSourceType("NATIONAL_OFFICIAL");
        candidate.setSourcePositionCode("123456");
        candidate.setDepartment("某部门");
        candidate.setPositionName("某职位");
        candidate.setStatus(0);
        candidate.setRecruitmentStatus("ACTIVE");
        jdbc.update("INSERT INTO ingestion_run(id,`year`,state,document_id) VALUES(1,2026,'READY',10)");
        jdbc.update("INSERT INTO ingestion_candidate(id,run_id,position_code,change_type,state,data_json) VALUES(1,1,'123456','NEW','PENDING',?)",
                mapper.writeValueAsString(candidate));
        when(positions.save(any(Position.class))).thenAnswer(invocation -> {
            ((Position) invocation.getArgument(0)).setId(55L);
            return true;
        });
        service.enqueuePublish(1, 99);
        assertEquals("PUBLISHING", jdbc.queryForObject("SELECT state FROM ingestion_run WHERE id=1", String.class));
        assertEquals(1, service.publishQueued(1));
        assertEquals("PUBLISHED", jdbc.queryForObject("SELECT state FROM ingestion_run WHERE id=1", String.class));
        assertEquals(99L, jdbc.queryForObject("SELECT reviewed_by FROM ingestion_run WHERE id=1", Long.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM position_revision WHERE position_id=55", Integer.class));
        verify(positions).save(any(Position.class));
    }
}
