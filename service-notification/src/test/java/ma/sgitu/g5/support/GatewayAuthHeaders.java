package ma.sgitu.g5.support;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public final class GatewayAuthHeaders {

    private GatewayAuthHeaders() {
    }

    public static MockHttpServletRequestBuilder withUser(MockHttpServletRequestBuilder builder) {
        return builder
                .header("X-User-Id", "test-user-001")
                .header("X-User-Email", "test@sgitu.ma")
                .header("X-Roles", "ROLE_USER")
                .header("X-Source-Group", "G10");
    }

    public static MockHttpServletRequestBuilder withAdmin(MockHttpServletRequestBuilder builder) {
        return builder
                .header("X-User-Id", "admin-001")
                .header("X-User-Email", "admin@sgitu.ma")
                .header("X-Roles", "ROLE_ADMIN")
                .header("X-Source-Group", "G10");
    }
}
