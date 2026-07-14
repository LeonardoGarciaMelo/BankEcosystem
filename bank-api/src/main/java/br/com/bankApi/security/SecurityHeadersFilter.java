package br.com.bankApi.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Adds standard security headers to every HTTP response.
 * <p>
 * Unlike CORS (which is only relevant when browser JavaScript from another
 * origin consumes this API), these headers protect against attacks that don't
 * require a frontend at all — they harden how ANY client (including a browser
 * navigating directly, or an iframe embedding this API's responses) is allowed
 * to interpret and display what this server sends back.
 */
@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        // Prevents MIME-sniffing: browser won't try to "guess" a different content
        // type than what the server declared (e.g. treating a JSON response as HTML/JS).
        responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");

        // Prevents this API's responses from being embedded in an <iframe> on another
        // site (clickjacking protection). DENY = never allowed, from any origin.
        responseContext.getHeaders().add("X-Frame-Options", "DENY");

        // Forces browsers to only ever connect via HTTPS to this host for the next
        // year, even if the user types "http://" — mitigates protocol-downgrade /
        // SSL-stripping attacks. Harmless to send over plain HTTP in dev (browsers
        // ignore this header unless the connection is already HTTPS).
        responseContext.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Legacy XSS filter header. Mostly superseded by Content-Security-Policy,
        // but still recommended for older browser compatibility.
        responseContext.getHeaders().add("X-XSS-Protection", "1; mode=block");

        // Controls how much referrer information is sent when navigating away from
        // this API's responses — reduces accidental leakage of internal URLs/tokens
        // that might appear in query strings.
        responseContext.getHeaders().add("Referrer-Policy", "no-referrer");
    }
}