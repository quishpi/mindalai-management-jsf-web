package ec.mindalai.managementjsf;

import ec.mindalai.managementjsf.client.TenantRestClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

class ClientTest {
    @Test void canMock(){ RestClient m=Mockito.mock(RestClient.class); TenantRestClient c=new TenantRestClient(m); assertNotNull(c); }
}
