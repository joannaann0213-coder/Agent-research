package dev.agentflow.agent.server.auth;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controller methods take the caller as a {@link Caller} parameter. */
public class CallerResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == Caller.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object caller = webRequest.getAttribute(Caller.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (caller == null) {
            throw new IllegalStateException("no caller on the request");
        }
        return caller;
    }
}
