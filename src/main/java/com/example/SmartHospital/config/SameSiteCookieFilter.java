package com.example.SmartHospital.config;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SameSiteCookieFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(httpServletResponse) {
            @Override
            public void addCookie(Cookie cookie) {
                cookie.setSecure(true);
                cookie.setAttribute("SameSite", "None");
                super.addCookie(cookie);
            }

            @Override
            public void setHeader(String name, String value) {
                if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name)) {
                    value = appendSameSiteAndSecure(value);
                }
                super.setHeader(name, value);
            }

            @Override
            public void addHeader(String name, String value) {
                if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name)) {
                    value = appendSameSiteAndSecure(value);
                }
                super.addHeader(name, value);
            }

            private String appendSameSiteAndSecure(String value) {
                if (!value.toLowerCase().contains("samesite")) {
                    value += "; SameSite=None";
                }
                if (!value.toLowerCase().contains("secure")) {
                    value += "; Secure";
                }
                return value;
            }
        };

        chain.doFilter(request, responseWrapper);
    }
}