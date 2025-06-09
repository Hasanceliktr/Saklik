// src/types/authTypes.ts
export interface RegisterRequest {
    username?: string;
    email?: string;
    password?: string;
}

export interface LoginRequest {
    username?: string;
    password?: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    id: number;
    username: string;
    email: string;
    // roles?: string[]; // İleride
}

export interface MessageResponse {
    message: string;
}