package com.m998.civilservice.modules.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Restricted downloader: HTTPS and an explicit official-host allowlist on every redirect. */
@Component
public class OfficialSourceClient {
    private static final Pattern LINKS = Pattern.compile("(?is)<a\\b[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>");
    private final Set<String> hosts;
    private final String seedUrl;

    public OfficialSourceClient(@Value("${ingestion.national.allowed-hosts}") String allowedHosts,
            @Value("${ingestion.national.seed-url}") String seedUrl) {
        this.hosts = new HashSet<>();
        Arrays.stream(allowedHosts.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(hosts::add);
        this.seedUrl = seedUrl;
    }

    public String discover(int year) throws Exception {
        URI base = validate(seedUrl);
        if (isSupportedAttachment(base.getPath())) return base.toString();
        byte[] page = fetch(base.toString(), 2_000_000);
        String found = findDocumentLink(base, new String(page, StandardCharsets.UTF_8), year);
        if (found == null) found = findDocumentLink(base, new String(page, Charset.forName("GB18030")), year);
        if (found != null) return found;
        throw new IllegalStateException("官方入口未发现 " + year + " 年 Excel/ZIP 职位表；可在管理端提交官方附件直链");
    }

    private String findDocumentLink(URI base, String html, int year) {
        Matcher matcher = LINKS.matcher(html);
        while (matcher.find()) {
            String label = matcher.group(2).replaceAll("<[^>]*>", "").replace("&nbsp;", "");
            URI link = base.resolve(matcher.group(1).replace("&amp;", "&"));
            if (!isSupportedAttachment(link.getPath())) continue;
            if (!(label.contains("职位") || label.contains("招考简章") || label.contains("岗位"))) continue;
            if (!label.contains(String.valueOf(year)) && !link.toString().contains(String.valueOf(year))) continue;
            try { return validate(link.toString()).toString(); }
            catch (IllegalArgumentException ignored) { /* Keep looking for an allowlisted official attachment. */ }
        }
        return null;
    }

    public byte[] download(String url) throws Exception {
        if (!isSupportedAttachment(validate(url).getPath())) throw new IllegalArgumentException("仅接受官方 Excel 或 ZIP 附件");
        byte[] data = fetch(url, 30_000_000);
        boolean xls = data.length > 8 && (data[0] & 0xff) == 0xd0 && (data[1] & 0xff) == 0xcf;
        boolean xlsx = data.length > 4 && data[0] == 'P' && data[1] == 'K';
        if (!xls && !xlsx) throw new IllegalArgumentException("下载结果不是 Excel/ZIP 文件");
        return data;
    }

    public byte[] extractWorkbook(String url, byte[] data) throws Exception {
        if (!validate(url).getPath().toLowerCase().endsWith(".zip")) return data;
        byte[] workbook = null;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(data), Charset.forName("GBK"))) {
            ZipEntry entry;
            int entries = 0;
            long totalExpanded = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 100) throw new IllegalArgumentException("ZIP 条目过多");
                if (entry.isDirectory()) continue;
                boolean excel = isExcel(entry.getName());
                if (excel && workbook != null) throw new IllegalArgumentException("ZIP 含多个 Excel 文件，需人工选择官方完整表");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    totalExpanded += count;
                    if (totalExpanded > 60_000_000) throw new IllegalArgumentException("ZIP 解压后总大小超过限制");
                    if (excel) {
                        if (out.size() + count > 40_000_000) throw new IllegalArgumentException("解压后的 Excel 超过大小限制");
                        out.write(buffer, 0, count);
                    }
                }
                if (excel) workbook = out.toByteArray();
            }
        }
        if (workbook == null) throw new IllegalArgumentException("ZIP 中没有 Excel 职位表");
        boolean xls = workbook.length > 8 && (workbook[0] & 0xff) == 0xd0 && (workbook[1] & 0xff) == 0xcf;
        boolean xlsx = workbook.length > 4 && workbook[0] == 'P' && workbook[1] == 'K';
        if (!xls && !xlsx) throw new IllegalArgumentException("ZIP 中的文件不是有效 Excel");
        return workbook;
    }

    URI validate(String url) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !hosts.contains(uri.getHost().toLowerCase()) || uri.getUserInfo() != null
                || uri.getPort() != -1 || uri.getFragment() != null) {
            throw new IllegalArgumentException("仅允许配置的官方 HTTPS 域名");
        }
        return uri;
    }

    private boolean isExcel(String path) {
        return path != null && (path.toLowerCase().endsWith(".xlsx") || path.toLowerCase().endsWith(".xls"));
    }

    private boolean isSupportedAttachment(String path) {
        return isExcel(path) || path != null && path.toLowerCase().endsWith(".zip");
    }

    private byte[] fetch(String url, int maxBytes) throws Exception {
        URI current = validate(url);
        for (int redirect = 0; redirect < 5; redirect++) {
            HttpURLConnection connection = (HttpURLConnection) current.toURL().openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "CivilServiceOfficialIngestion/1.0");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IllegalStateException("官方响应缺少重定向地址");
                    current = validate(current.resolve(location).toString());
                    continue;
                }
                if (status != 200) throw new IllegalStateException("官方来源 HTTP " + status);
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (out.size() + count > maxBytes) throw new IllegalStateException("官方附件超过大小限制");
                        out.write(buffer, 0, count);
                    }
                    return out.toByteArray();
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IllegalStateException("官方来源重定向过多");
    }
}
