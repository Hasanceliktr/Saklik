// src/services/authService.ts
// import axios from 'axios'; // Eski axios importunu kaldır
import apiClient from './api';
import type {AuthResponse, LoginRequest, MessageResponse, RegisterRequest} from "../types/authTypes.ts"; // Yeni apiClient'ı import et


// API_URL'e burada artık gerek yok, apiClient'ın baseURL'i var.
// const API_URL = 'http://localhost:8080/api/auth/'; // KALDIRILDI

const register = async (userData: RegisterRequest): Promise<MessageResponse> => {
    // apiClient baseURL'i '/api' olduğu için, buraya sadece endpoint'in kalan kısmını yazıyoruz.
    const response = await apiClient.post<MessageResponse>('/auth/register', userData);
    return response.data;
};

const login = async (userData: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', userData);
    if (response.data.token) {
        localStorage.setItem('userToken', response.data.token);
        localStorage.setItem('userData', JSON.stringify({
            id: response.data.id,
            username: response.data.username,
            email: response.data.email,
        }));
        // Token'ı apiClient'ın varsayılan header'larına da ekleyebiliriz,
        // ama interceptor zaten her istekte ekleyeceği için bu şart değil.
        // apiClient.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;
    }
    return response.data;
};

const logout = () => {
    localStorage.removeItem('userToken');
    localStorage.removeItem('userData');
    // Token'ı apiClient'ın varsayılan header'larından da sil
    // delete apiClient.defaults.headers.common['Authorization']; // Eğer login'de set ettiyseniz
    // AuthContext zaten state'i güncelleyecektir.
};

const getCurrentUser = (): Partial<AuthResponse> & { token?: string } | null => { // Dönüş tipini biraz daha esnek yaptık
    const token = localStorage.getItem('userToken');
    const userDataString = localStorage.getItem('userData');
    if (token && userDataString) {
        try {
            const userData: { id: number; username: string; email: string } = JSON.parse(userDataString);
            return {
                token: token,
                type: 'Bearer', // Varsayılan
                id: userData.id,
                username: userData.username,
                email: userData.email,
            };
        } catch (e) {
            console.error("Error parsing user data from localStorage", e);
            return null;
        }
    }
    return null;
};

const authService = {
    register,
    login,
    logout,
    getCurrentUser,
};

export default authService;