package org.hasanceliktr.saklik.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor; // Lombok ile final alanlar için constructor
import org.hasanceliktr.saklik.security.CustomUserDetailsService; // Bizim UserDetailsService'imiz
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Eğer @RequiredArgsConstructor kullanmıyorsak
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component; // Bu filter'ı Spring component'i yapıyoruz
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter; // Her istek için bir kere çalışsın

import java.io.IOException;

@Component // Bu filter'ı Spring'in tanıması için @Component olarak işaretliyoruz
@RequiredArgsConstructor // Lombok: final JwtTokenProvider ve final CustomUserDetailsService için constructor üretir
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // Eğer @RequiredArgsConstructor kullanmıyorsanız, aşağıdaki gibi @Autowired ile constructor injection yapabilirsiniz:
    /*
    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }
    */

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Token'dan kullanıcı adını (veya email'i, CustomUserDetailsService nasıl ayarlandıysa) al
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // Kullanıcıyı veritabanından yükle
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // Authentication token oluştur
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,      // Principal olarak UserDetails nesnesi
                        null,             // Credentials (JWT kullandığımız için gerek yok)
                        userDetails.getAuthorities()); // Yetkiler

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext'e authentication nesnesini set et
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // logger.debug("User '{}' authenticated successfully.", username); // Başarılı auth logu
            }
        } catch (Exception ex) {
            // Hata durumunda kullanıcıyı authenticate etme (SecurityContext'e null set etmeye gerek yok, zaten null'dır)
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response); // Filter zincirinde bir sonraki adıma geç
    }

    /**
     * HTTP Request'in "Authorization" header'ından JWT'yi çeker.
     * @param request Gelen HTTP isteği
     * @return JWT string'i veya bulunamazsa null.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " (7 karakter) sonrasını al
        }
        return null;
    }
}