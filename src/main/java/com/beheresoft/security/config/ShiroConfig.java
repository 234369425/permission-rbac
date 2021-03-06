package com.beheresoft.security.config;

import com.beheresoft.security.cache.SpringCacheManager;
import com.beheresoft.security.credentials.RetryLimitHashedCredentialsMatcher;
import com.beheresoft.security.filter.CustomFormAuthenticationFilter;
import com.beheresoft.security.realm.CasRealm;
import com.beheresoft.security.realm.LoginRealm;
import com.beheresoft.security.session.CustomWebSessionManager;
import com.google.gson.Gson;
import io.buji.pac4j.filter.CallbackFilter;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Aladi
 */
@Configuration
@Slf4j
@EqualsAndHashCode
public class ShiroConfig {

    private SystemConfig systemConfig;
    private SpringCacheManager cacheManager;
    private ApplicationContext applicationContext;

    public ShiroConfig(SystemConfig systemConfig, SpringCacheManager cacheManager, ApplicationContext applicationContext) {
        this.systemConfig = systemConfig;
        this.cacheManager = cacheManager;
        this.applicationContext = applicationContext;
    }

    /**
     * 配置shiro filter
     *
     * @param securityManager security manager
     * @param gson            json序列化工具
     * @return FilterFactoryBean
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilters(SecurityManager securityManager, CallbackFilter callbackFilter, Gson gson) {
        ShiroFilterFactoryBean filterFactoryBean = new ShiroFilterFactoryBean();
        filterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> filterChain = gson.fromJson(systemConfig.getFilterChain(), Map.class);
        filterFactoryBean.setFilterChainDefinitionMap(filterChain);
        filterFactoryBean.getFilters().put("authc", new CustomFormAuthenticationFilter());
        filterFactoryBean.getFilters().put("callbackFilter", callbackFilter);
        //filterFactoryBean.getFilters().put("");
        //org.pac4j.cas.config.CasConfiguration
        filterFactoryBean.setLoginUrl(systemConfig.getLoginUrl());
        filterFactoryBean.setUnauthorizedUrl(systemConfig.getUnauthorizedUrl());
        return filterFactoryBean;
    }

    @Bean
    public CacheManager cacheManager() {
        return cacheManager;
    }

    /**
     * 配置SecurityManager
     * 使用userRealm
     *
     * @param loginRealm               realm
     * @param hashedCredentialsMatcher matcher
     * @return SecurityManager
     */
    @Bean
    public DefaultSecurityManager securityManager(LoginRealm loginRealm,
                                                  CasRealm casRealm,
                                                  CustomWebSessionManager webSessionManager,
                                                  HashedCredentialsMatcher hashedCredentialsMatcher) {
        DefaultSecurityManager securityManager = new DefaultWebSecurityManager();
        CredentialsMatcher matcher = hashedCredentialsMatcher;
        SystemConfig.ShiroProperties properties = systemConfig.getShiro();

        //配置credentials matcher
        if (properties != null && properties.getCredentialsMatcher() != null) {
            Object o = this.applicationContext.getBean(properties.getCredentialsMatcher());
            if (o instanceof CredentialsMatcher) {
                matcher = (CredentialsMatcher) o;
            }
        }
        loginRealm.setCredentialsMatcher(matcher);

        CacheManager cacheManager = this.cacheManager;
        //配置cache manager
        if (properties != null && properties.getCacheManager() != null) {
            Object o = this.applicationContext.getBean(properties.getCacheManager());
            if (o instanceof CacheManager) {
                cacheManager = (CacheManager) o;
            }
        }
        Set<Realm> realms = new HashSet<>();
        realms.add(loginRealm);
        realms.add(casRealm);
        securityManager.setRealms(realms);
        securityManager.setCacheManager(cacheManager);
        securityManager.setSessionManager(webSessionManager);
        return securityManager;
    }

    /**
     * 验证密码哈希匹配
     *
     * @param hashedCredentialsMatcher 使用的Matcher
     * @return Matcher
     */
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher(RetryLimitHashedCredentialsMatcher hashedCredentialsMatcher) {
        hashedCredentialsMatcher.setHashAlgorithmName(systemConfig.getAlgorithmName());
        hashedCredentialsMatcher.setHashIterations(systemConfig.getHashIterations());
        hashedCredentialsMatcher.setStoredCredentialsHexEncoded(true);
        return hashedCredentialsMatcher;
    }

}
