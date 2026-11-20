package dev.agentflow.inbox.server.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controller methods take the caller as an {@link AuthenticatedClient} parameter. */
public class AuthenticatedClientResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == AuthenticatedClient.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object client = webRequest.getAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (client == null) {
            throw new IllegalStateException("no authenticated client on the request");
        }
        return client;
    }
}
