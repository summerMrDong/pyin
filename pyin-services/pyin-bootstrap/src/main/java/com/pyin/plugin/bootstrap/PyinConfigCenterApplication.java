package com.pyin.plugin.bootstrap;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.pyin")
@MapperScan(value = {"com.pyin.plugin"},
        markerInterface = BaseMapper.class)
public class PyinConfigCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PyinConfigCenterApplication.class, args);
    }
}
