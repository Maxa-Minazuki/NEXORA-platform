package com.nexora.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.auth.entity.User;
import com.nexora.auth.repository.UserRepository;
import com.nexora.shared.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Full application context on H2 (MySQL mode) with the real Flyway migrations and demo data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected UserRepository users;

    protected AuthenticatedUser principal(String username) {
        User u = users.findByUsernameIgnoreCase(username).orElseThrow();
        return new AuthenticatedUser(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), null, true);
    }

    protected ResultActions getAs(String username, String url) throws Exception {
        return mvc.perform(get(url).with(user(principal(username))));
    }

    protected ResultActions postAs(String username, String url, Object body) throws Exception {
        MockHttpServletRequestBuilder req = post(url).with(user(principal(username))).with(csrf());
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String s ? s : json.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    protected JsonNode read(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
