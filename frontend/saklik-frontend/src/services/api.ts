// src/services/api.ts

// Backend API'mizin temel URL'i
// Bu, authService.ts'deki API_URL ile aynı olabilir veya daha genel bir /api olabilir.
// Eğer tüm API endpoint'lerimiz /api altında ise:
import axios, {type InternalAxiosRequestConfig} from "axios";

const API_BASE_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
    },
});

// Request Interceptor: Her istek gönderilmeden ÖNCE çalışır
apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // AuthContext'ten veya localStorage'dan token'ı al
        // ÖNEMLİ: Interceptor'lar React hook'larını (useAuth gibi) doğrudan kullanamaz.
        // Bu yüzden token'ı localStorage'dan okumamız veya Context'e erişmenin
        // hook olmayan bir yolunu bulmamız (veya state management store'u import etmek) gerekir.
        // Şimdilik en basit yol localStorage.
        const token = localStorage.getItem('userToken');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        // İstek hatası durumunda bir şeyler yap
        return Promise.reject(error);
    }
);

// Response Interceptor (Opsiyonel - Hata yönetimi için eklenebilir)
// apiClient.interceptors.response.use(
//     (response) => {
//         // Başarılı response'larda bir şeyler yap
//         return response;
//     },
//     (error) => {
//         // Hata response'larında bir şeyler yap
//         // Örneğin, 401 (Unauthorized) hatası alırsak kullanıcıyı otomatik logout yapıp login'e yönlendirebiliriz.
//         if (error.response && error.response.status === 401) {
//             // authService.logout(); // authService'i buraya import etmek gerekecek
//             // window.location.href = '/login';
//             console.error('Unauthorized! Logging out...');
//         }
//         return Promise.reject(error);
//     }
// );

export default apiClient;