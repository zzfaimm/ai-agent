package com.iverson.aiagent.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
@MapperScan(basePackages = "com.iverson.aiagent.rag.mapper", sqlSessionFactoryRef = "pgSqlSessionFactory")
public class PgVectorDataSourceConfig {

    @Bean(name = "pgDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.pgvector")
    public DataSource pgDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "pgSqlSessionFactory")
    public SqlSessionFactory pgSqlSessionFactory(
            @Qualifier("pgDataSource") DataSource dataSource) throws Exception {

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }
    @Bean(name = "pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate(
            @Qualifier("pgDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}