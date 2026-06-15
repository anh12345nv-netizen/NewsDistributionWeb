package com.newsdistribution.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfig {

    /* ─── DB A: WinForm Thanhnien (JdbcTemplate ONLY) ─── */
    @Bean("dsPropsA")
    @ConfigurationProperties("datasource-a")
    public DataSourceProperties dsPropsA() { return new DataSourceProperties(); }

    @Bean("dataSourceA")
    public DataSource dataSourceA(@Qualifier("dsPropsA") DataSourceProperties p) {
        return p.initializeDataSourceBuilder().build();
    }

    @Bean("jdbcA")
    public JdbcTemplate jdbcA(@Qualifier("dataSourceA") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    /* ─── DB B: NewsDistributionWeb (JPA @Primary) ─── */
    @Primary
    @Bean("dsPropsB")
    @ConfigurationProperties("datasource-b")
    public DataSourceProperties dsPropsB() { return new DataSourceProperties(); }

    @Primary
    @Bean("dataSourceB")
    public DataSource dataSourceB(@Qualifier("dsPropsB") DataSourceProperties p) {
        return p.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean("jdbcB")
    public JdbcTemplate jdbcB(@Qualifier("dataSourceB") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Primary
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSourceB") DataSource ds) {
        var f = new LocalContainerEntityManagerFactoryBean();
        f.setDataSource(ds);
        f.setPackagesToScan("com.newsdistribution.entity");
        f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        var p = new Properties();
        p.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        p.put("hibernate.hbm2ddl.auto", "update");
        f.setJpaProperties(p);
        return f;
    }

    @Primary
    @Bean("transactionManager")
    public JpaTransactionManager transactionManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    /* ─── DB C: NewsDistributionMaster (JdbcTemplate ONLY) ─── */
    @Bean("dsPropsC")
    @ConfigurationProperties("datasource-c")
    public DataSourceProperties dsPropsC() { return new DataSourceProperties(); }

    @Bean("dataSourceC")
    public DataSource dataSourceC(@Qualifier("dsPropsC") DataSourceProperties p) {
        return p.initializeDataSourceBuilder().build();
    }

    @Bean("jdbcC")
    public JdbcTemplate jdbcC(@Qualifier("dataSourceC") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
