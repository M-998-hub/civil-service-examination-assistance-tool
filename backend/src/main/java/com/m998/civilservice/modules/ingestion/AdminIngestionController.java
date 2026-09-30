package com.m998.civilservice.modules.ingestion;

import com.m998.civilservice.common.api.CommonResult;
import com.m998.civilservice.domain.AdminUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ingestion")
public class AdminIngestionController {
    private final NationalIngestionService service;
    private final NationalPublishingWorker publisher;

    public AdminIngestionController(NationalIngestionService service, NationalPublishingWorker publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAuthority('官方数据采集')")
    public CommonResult<Long> launch(@RequestBody Map<String, Object> request) {
        Object yearValue = request.get("year");
        if (!(yearValue instanceof Number)) return CommonResult.validateFailed("请选择招录年份");
        try {
            return CommonResult.success(service.launch((String) request.get("officialUrl"), ((Number) yearValue).intValue()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return CommonResult.failed(e.getMessage());
        }
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('官方数据采集')")
    public CommonResult<List<Map<String, Object>>> runs() { return CommonResult.success(service.runs()); }

    @GetMapping("/runs/{id}/candidates")
    @PreAuthorize("hasAuthority('官方数据采集')")
    public CommonResult<Map<String, Object>> candidates(@PathVariable long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "100") int pageSize) {
        try {
            return CommonResult.success(service.candidates(id, pageNum, pageSize));
        } catch (IllegalArgumentException e) {
            return CommonResult.validateFailed(e.getMessage());
        }
    }

    @PostMapping("/runs/{id}/publish")
    @PreAuthorize("hasAuthority('官方数据发布')")
    public CommonResult<Long> publish(@PathVariable long id, Authentication authentication) {
        try {
            service.enqueuePublish(id, reviewerId(authentication));
            publisher.publishAsync(id);
            return CommonResult.success(id);
        } catch (Exception e) {
            return CommonResult.failed(e.getMessage());
        }
    }

    @PostMapping("/runs/{id}/reject")
    @PreAuthorize("hasAuthority('官方数据发布')")
    public CommonResult<Void> reject(@PathVariable long id, Authentication authentication) {
        try {
            service.reject(id, reviewerId(authentication));
            return CommonResult.success(null);
        } catch (IllegalStateException e) {
            return CommonResult.failed(e.getMessage());
        }
    }

    private long reviewerId(Authentication authentication) {
        return ((AdminUserDetails) authentication.getPrincipal()).getUmsAdmin().getId();
    }
}
