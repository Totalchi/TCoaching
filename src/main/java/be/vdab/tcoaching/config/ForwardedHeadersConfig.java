package be.vdab.tcoaching.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@ConditionalOnProperty(name = "security.rate-limit.use-forwarded-headers", havingValue = "true")
public class ForwardedHeadersConfig {
    @Bean
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean =
                new FilterRegistrationBean<>(new ForwardedHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
