package com.m998.civilservice.modules.ingestion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NationalIngestionWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(NationalIngestionWorker.class);
    private final JdbcTemplate jdbc;
    private final OfficialSourceClient source;
    private final NationalPositionParser parser;
    private final PositionService positions;
    private final ObjectMapper mapper;
    private final Path archiveDirectory;

    public NationalIngestionWorker(JdbcTemplate jdbc, OfficialSourceClient source, NationalPositionParser parser,
            PositionService positions, ObjectMapper mapper,
            @Value("${ingestion.national.archive-dir}") String archiveDirectory) {
        this.jdbc = jdbc;
        this.source = source;
        this.parser = parser;
        this.positions = positions;
        this.mapper = mapper;
        this.archiveDirectory = Paths.get(archiveDirectory).toAbsolutePath().normalize();
    }

    @Async
    public void process(long runId, String url, int year) {
        try {
            jdbc.update("UPDATE ingestion_run SET state='PROCESSING' WHERE id=?", runId);
            String documentUrl = url == null || url.trim().isEmpty() ? source.discover(year) : source.validate(url).toString();
            byte[] data = source.download(documentUrl);
            String hash = sha256(data);
            List<Map<String, Object>> existingDocument = jdbc.queryForList("SELECT id FROM ingestion_document WHERE sha256=?", hash);
            Integer completed = jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_run WHERE `year`=? AND document_id=? AND state IN ('READY','PUBLISHED','NO_CHANGES')",
                    Integer.class, year, existingDocument.isEmpty() ? -1L : existingDocument.get(0).get("id"));
            if (completed != null && completed > 0) {
                jdbc.update("UPDATE ingestion_run SET source_url=?,document_id=?,state='UNCHANGED',message='文档哈希未变',finished_at=NOW() WHERE id=?",
                        documentUrl, existingDocument.get(0).get("id"), runId);
                return;
            }
            List<Position> parsed = parser.parse(new ByteArrayInputStream(source.extractWorkbook(documentUrl, data)), year);
            List<Position> previous = positions.list(new LambdaQueryWrapper<Position>()
                    .eq(Position::getYear, year).eq(Position::getSourceType, "NATIONAL_OFFICIAL")
                    .eq(Position::getStatus, 0));
            Map<String, Position> previousByCode = new HashMap<>();
            previous.forEach(p -> previousByCode.put(p.getSourcePositionCode(), p));
            if (!previous.isEmpty() && parsed.size() < previous.size() * 0.8)
                throw new IllegalArgumentException("新表格职位数低于现有官方岗位的 80%，疑似非完整职位表");
            long documentId;
            if (existingDocument.isEmpty()) {
                Files.createDirectories(archiveDirectory);
                String extension = documentUrl.toLowerCase().endsWith(".zip") ? ".zip"
                        : documentUrl.toLowerCase().endsWith(".xls") ? ".xls" : ".xlsx";
                Path archive = archiveDirectory.resolve(hash + extension);
                Files.write(archive, data);
                GeneratedKeyHolder key = new GeneratedKeyHolder();
                jdbc.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO ingestion_document(source_url,sha256,file_path) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, documentUrl);
                    statement.setString(2, hash);
                    statement.setString(3, archive.toString());
                    return statement;
                }, key);
                documentId = key.getKey().longValue();
            } else {
                documentId = ((Number) existingDocument.get(0).get("id")).longValue();
            }
            jdbc.update("UPDATE ingestion_run SET source_url=?,document_id=? WHERE id=?", documentUrl, documentId, runId);
            Set<String> seen = new HashSet<>();
            int changes = 0;
            for (Position candidate : parsed) {
                String code = candidate.getSourcePositionCode();
                seen.add(code);
                Position before = previousByCode.get(code);
                String change = before == null ? "NEW" : changed(before, candidate) ? "UPDATE" : null;
                if (change != null) {
                    insertCandidate(runId, code, change, candidate, before);
                    changes++;
                }
            }
            for (Position old : previous) {
                if (!seen.contains(old.getSourcePositionCode()) && "ACTIVE".equals(old.getRecruitmentStatus())) {
                    insertCandidate(runId, old.getSourcePositionCode(), "WITHDRAW", old, old);
                    changes++;
                }
            }
            jdbc.update("UPDATE ingestion_run SET state=?,candidate_count=?,message=?,finished_at=NOW() WHERE id=?",
                    changes == 0 ? "NO_CHANGES" : "READY", changes,
                    changes == 0 ? "无岗位差异" : "待人工审核发布", runId);
        } catch (Exception e) {
            LOGGER.warn("National ingestion run {} failed: {}", runId, e.toString());
            jdbc.update("UPDATE ingestion_run SET state='FAILED',message=?,finished_at=NOW() WHERE id=?",
                    e.getMessage() == null ? "采集失败" : e.getMessage().substring(0, Math.min(900, e.getMessage().length())), runId);
        }
    }

    private void insertCandidate(long runId, String code, String change, Position candidate, Position before) throws Exception {
        jdbc.update("INSERT INTO ingestion_candidate(run_id,position_code,change_type,data_json,previous_json) VALUES(?,?,?,?,?)",
                runId, code, change, mapper.writeValueAsString(candidate), before == null ? null : mapper.writeValueAsString(before));
    }

    private boolean changed(Position before, Position after) {
        return !safe(before.getDepartment()).equals(safe(after.getDepartment()))
                || !safe(before.getPositionName()).equals(safe(after.getPositionName()))
                || !safe(before.getMajorRequired()).equals(safe(after.getMajorRequired()))
                || !safe(before.getEducationRequired()).equals(safe(after.getEducationRequired()))
                || !safe(before.getPoliticalStatusRequired()).equals(safe(after.getPoliticalStatusRequired()))
                || !safe(before.getIsFreshOnly()).equals(safe(after.getIsFreshOnly()))
                || !safe(before.getRecruitmentNumber()).equals(safe(after.getRecruitmentNumber()))
                || !"ACTIVE".equals(before.getRecruitmentStatus());
    }

    private String safe(Object value) { return value == null ? "" : value.toString(); }

    private String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder text = new StringBuilder();
        for (byte value : digest) text.append(String.format("%02x", value & 0xff));
        return text.toString();
    }
}
