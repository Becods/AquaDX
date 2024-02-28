package icu.samnyan.aqua.sega.diva.filter;

import icu.samnyan.aqua.sega.general.filter.CompressRequestWrapper;
import icu.samnyan.aqua.sega.general.filter.CompressResponseWrapper;
import icu.samnyan.aqua.sega.util.Compression;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

/**
 * @author samnyan (privateamusement@protonmail.com)
 */
@Component
public class DivaCompressionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DivaCompressionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String encoding = request.getHeader("pragma");
        byte[] reqSrc = request.getInputStream().readAllBytes();

        byte[] reqResult;
        if (encoding != null && encoding.equals("DFI")) {
            reqResult = Base64.getMimeDecoder().decode(reqSrc);
            reqResult = Compression.decompress(reqResult);
        } else {
            reqResult = reqSrc;
        }

        CompressRequestWrapper requestWrapper = new CompressRequestWrapper(request, reqResult);
        CompressResponseWrapper responseWrapper = new CompressResponseWrapper(response);

        filterChain.doFilter(requestWrapper, responseWrapper);
        byte[] respSrc = responseWrapper.toByteArray();
        byte[] respResult = Compression.compress(respSrc);
        respResult = Base64.getMimeEncoder().encode(respResult);


        response.setContentLength(respResult.length);
        response.setHeader("pragma", "DFI");

        try {
            response.getOutputStream().write(respResult);
        } catch (EofException e) {
            logger.warn("- EOF: Client closed connection");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/g/diva");
    }
}
