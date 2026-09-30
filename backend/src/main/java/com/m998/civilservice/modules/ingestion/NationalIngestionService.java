package com.m998.civilservice.modules.ingestion;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.m998.civilservice.modules.position.model.Position;
import com.m998.civilservice.modules.position.service.PositionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class NationalIngestionService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final PositionService positions;
    private final NationalIngestionWorker worker;
    private final boolean enabled;
    private final int configuredYear;

    public NationalIngestionService(JdbcTemplate jdbc, ObjectMapper mapper, PositionService positions,
            NationalIngestionWorker worker,
            @Value("${ingestion.national.enabled:false}") boolean enabled,
            @Value("${ingestion.national.year:2026}") int configuredYear) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.positions = positions;
        this.worker = worker;
        this.enabled = enabled;
        this.configuredYear = configuredYear;
    }

    @Scheduled(cron = "${ingestion.national.cron:0 30 3 * * *}")
    public void scheduledDiscovery() {
        if (enabled) launch(null, configuredYear);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        jdbc.update("UPDATE ingestion_run SET state='FAILED',message='服务重启导致采集中断',finished_at=NOW() WHERE state IN ('QUEUED','PROCESSING')");
        jdbc.update("UPDATE ingestion_run SET state='READY',message='服务重启，发布事务已回滚，请复核后重试' WHERE state='PUBLISHING'");
    }

    public synchronized long launch(String officialUrl, int year) {
        if (year < 2020 || year > 2100) throw new IllegalArgumentException("招录年份无效");
        Integer active = jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_run WHERE state IN ('QUEUED','PROCESSING','PUBLISHING')", Integer.class);
        if (active != null && active > 0) throw new IllegalStateException("已有采集或发布任务正在运行");
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO ingestion_run(`year`,source_url,state) VALUES(?,?,'QUEUED')", Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, year);
            statement.setString(2, officialUrl);
            return statement;
        }, key);
        long runId = key.getKey().longValue();
        worker.process(runId, officialUrl, year);
        return runId;
    }

    public List<Map<String, Object>> runs() {
        return jdbc.queryForList("SELECT id,year,source_url,document_id,state,message,candidate_count,created_at,finished_at,reviewed_by FROM ingestion_run ORDER BY id DESC LIMIT 50");
    }

    public Map<String, Object> candidates(long runId, int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) throw new IllegalArgumentException("分页参数无效");
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_candidate WHERE run_id=?", Integer.class, runId);
        Integer withdrawals = jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_candidate WHERE run_id=? AND change_type='WITHDRAW'", Integer.class, runId);
        List<Map<String, Object>> records = jdbc.queryForList("SELECT id,position_code,change_type,state,data_json,previous_json,validation_error FROM ingestion_candidate WHERE run_id=? ORDER BY id LIMIT ? OFFSET ?",
                runId, pageSize, (pageNum - 1) * pageSize);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", total);
        result.put("withdrawalCount", withdrawals);
        result.put("records", records);
        return result;
    }

    public void enqueuePublish(long runId, long reviewerId) {
        int updated = jdbc.update("UPDATE ingestion_run SET state='PUBLISHING',reviewed_by=?,message='正在发布' WHERE id=? AND state='READY'",
                reviewerId, runId);
        if (updated == 0) throw new IllegalStateException("该批次当前不可发布");
    }

    public void publicationFailed(long runId, String message) {
        String safeMessage = message == null ? "发布失败" : message.substring(0, Math.min(message.length(), 850));
        jdbc.update("UPDATE ingestion_run SET state='READY',message=? WHERE id=? AND state='PUBLISHING'",
                "发布失败：" + safeMessage, runId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int publish(long runId, long reviewerId) throws Exception {
        return publishInternal(runId, reviewerId, "READY");
    }

    @Transactional(rollbackFor = Exception.class)
    public int publishQueued(long runId) throws Exception {
        return publishInternal(runId, null, "PUBLISHING");
    }

    private int publishInternal(long runId, Long reviewerId, String expectedState) throws Exception {
        Map<String, Object> run = jdbc.queryForMap("SELECT * FROM ingestion_run WHERE id=? FOR UPDATE", runId);
        if (!expectedState.equals(run.get("state"))) throw new IllegalStateException("该批次当前不可发布");
        long effectiveReviewer = reviewerId == null ? ((Number) run.get("reviewed_by")).longValue() : reviewerId;
        int year = ((Number) run.get("year")).intValue();
        Integer newer = jdbc.queryForObject("SELECT COUNT(*) FROM ingestion_run WHERE `year`=? AND id>? AND state='PUBLISHED'", Integer.class, year, runId);
        if (newer != null && newer > 0) throw new IllegalStateException("已有更新批次发布，请重新采集");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ingestion_candidate WHERE run_id=? ORDER BY id", runId);
        if (rows.isEmpty()) throw new IllegalStateException("没有待发布差异");
        int count = 0;
        for (Map<String, Object> row : rows) {
            if (!"PENDING".equals(row.get("state")) || row.get("validation_error") != null)
                throw new IllegalStateException("存在未通过校验的候选岗位");
            String change = (String) row.get("change_type");
            Position candidate = mapper.readValue((String) row.get("data_json"), Position.class);
            Position before = positions.getOne(new LambdaQueryWrapper<Position>()
                    .eq(Position::getYear, year).eq(Position::getSourceType, "NATIONAL_OFFICIAL")
                    .eq(Position::getSourcePositionCode, row.get("position_code"))
                    .eq(Position::getStatus, 0), false);
            String beforeJson = before == null ? null : json(before);
            if ("NEW".equals(change)) {
                if (before != null) throw new IllegalStateException("职位代码已存在，需重新采集");
                candidate.setSourceDocumentId(((Number) run.get("document_id")).longValue());
                if (!positions.save(candidate)) throw new IllegalStateException("新增岗位失败，整批回滚");
            } else if ("UPDATE".equals(change) || "WITHDRAW".equals(change)) {
                if (before == null) throw new IllegalStateException("原岗位不存在，需重新采集");
                candidate.setId(before.getId());
                candidate.setSourceDocumentId(((Number) run.get("document_id")).longValue());
                if ("WITHDRAW".equals(change)) {
                    candidate = before;
                    candidate.setSourceDocumentId(((Number) run.get("document_id")).longValue());
                    candidate.setRecruitmentStatus("WITHDRAWN");
                }
                if (!positions.updateById(candidate)) throw new IllegalStateException("更新岗位失败，整批回滚");
            } else throw new IllegalStateException("未知变更类型");
            jdbc.update("INSERT INTO position_revision(position_id,run_id,change_type,before_json,after_json) VALUES(?,?,?,?,?)",
                    candidate.getId(), runId, change, beforeJson, json(candidate));
            jdbc.update("UPDATE ingestion_candidate SET state='PUBLISHED' WHERE id=?", row.get("id"));
            count++;
        }
        jdbc.update("UPDATE ingestion_run SET state='PUBLISHED',reviewed_by=?,message='发布完成',finished_at=NOW() WHERE id=?", effectiveReviewer, runId);
        return count;
    }

    @Transactional
    public void reject(long runId, long reviewerId) {
        int changed = jdbc.update("UPDATE ingestion_run SET state='REJECTED',reviewed_by=?,finished_at=NOW() WHERE id=? AND state='READY'", reviewerId, runId);
        if (changed == 0) throw new IllegalStateException("该批次当前不可驳回");
        jdbc.update("UPDATE ingestion_candidate SET state='REJECTED' WHERE run_id=?", runId);
    }

    private String json(Position position) throws JsonProcessingException {
        return mapper.writeValueAsString(position);
    }
}
