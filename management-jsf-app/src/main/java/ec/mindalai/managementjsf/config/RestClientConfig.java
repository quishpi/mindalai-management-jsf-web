package ec.mindalai.managementjsf.config;

import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestClientConfig {
    @Bean public RestClient restClient(AppProperties props){
        SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeout()); f.setReadTimeout(props.getTimeout());
        return RestClient.builder().baseUrl(props.getBaseUrl()).requestFactory(f).build();
    }
}
