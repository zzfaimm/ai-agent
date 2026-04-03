package com.iverson.aiagent.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;


@Configuration
@MapperScan(basePackages = "com.iverson.aiagent.chatmemory.mapper", sqlSessionFactoryRef = "mysqlSqlSessionFactory")
public class MySqlDataSourceConfig {

    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }
    @Bean(name = "mysqlSqlSessionFactory")
    public SqlSessionFactory mysqlSqlSessionFactory(
            @Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {

        // 使用 MybatisSqlSessionFactoryBean，而不是 SqlSessionFactoryBean
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 可选：设置实体类包路径，便于别名
        factoryBean.setTypeAliasesPackage("com.iverson.aiagent.chatmemory.entity");

        // 如果你有 MyBatis-Plus 全局配置，可以在这里设置
        // GlobalConfig globalConfig = new GlobalConfig();
        // factoryBean.setGlobalConfig(globalConfig);

        return factoryBean.getObject();
    }

//    @Bean(name = "mysqlSqlSessionFactory")
//    public SqlSessionFactory mysqlSqlSessionFactory(
//            @Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
//
//        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//        factoryBean.setDataSource(dataSource);
//        return factoryBean.getObject();
//    }
}