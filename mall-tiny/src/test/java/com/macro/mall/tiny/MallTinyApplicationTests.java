package com.macro.mall.tiny;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("需要真实 MySQL 数据库，CI 环境跳过")
@SpringBootTest
public class MallTinyApplicationTests {

    @Test
    public void contextLoads() {
    }

}
