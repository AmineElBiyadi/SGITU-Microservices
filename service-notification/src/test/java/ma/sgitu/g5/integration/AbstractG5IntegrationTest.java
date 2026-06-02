package ma.sgitu.g5.integration;

import ma.sgitu.g5.dto.response.SendResultDTO;
import ma.sgitu.g5.provider.IEmailProvider;
import ma.sgitu.g5.provider.IPushProvider;
import ma.sgitu.g5.provider.ISMSProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractG5IntegrationTest {

    @MockBean
    protected IEmailProvider emailProvider;
    @MockBean
    protected ISMSProvider smsProvider;
    @MockBean
    protected IPushProvider pushProvider;

    @BeforeEach
    void mockExternalProviders() {
        SendResultDTO ok = new SendResultDTO();
        ok.setSuccess(true);
        ok.setProvider("TEST_MOCK");
        lenient().when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn(ok);
        lenient().when(smsProvider.send(anyString(), anyString())).thenReturn(ok);
        lenient().when(pushProvider.send(anyString(), anyString())).thenReturn(ok);
    }
}
