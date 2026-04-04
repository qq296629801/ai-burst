package com.aiburst.mag.slice;

import com.aiburst.mag.testsupport.MagMethodSecurityTestConfig;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * MAG REST 切片测试专用：不启数据源/MyBatis/Redis/Flyway，仅加载 {@code com.aiburst.mag.api} 下控制器。
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        MybatisAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class
})
@ComponentScan(basePackages = "com.aiburst.mag.api")
@Import(MagMethodSecurityTestConfig.class)
public class MagApiControllersSliceApplication {
}
