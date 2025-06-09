
import authService from "../services/authService.ts";
import {createContext, type ReactNode, useContext, useEffect, useReducer} from "react";

interface AuthState {
    isAuthenticated: boolean;
    user: {
        id: number | null;
        username: string | null;
        email: string | null;
    } | null;
    token: string | null;
    isLoading: boolean;
}

type AuthAction =
    | { type: 'INITIALIZE'; payload: { isAuthenticated: boolean; user: AuthState['user']; token: string | null } }
    | { type: 'LOGIN_SUCCESS'; payload: { user: AuthState['user']; token: string } }
    | { type: 'LOGOUT' }
    | { type: 'SET_LOADING'; payload: boolean };

const initialState: AuthState = {
    isAuthenticated: false,
    user: null,
    token: null,
    isLoading: true, // Başlangıçta yükleniyor
};

// Reducer fonksiyonu
const authReducer = (state: AuthState, action: AuthAction): AuthState => {
    switch (action.type) {
        case 'INITIALIZE':
            return {
                ...state,
                isAuthenticated: action.payload.isAuthenticated,
                user: action.payload.user,
                token: action.payload.token,
                isLoading: false,
            };
        case 'LOGIN_SUCCESS':
            return {
                ...state,
                isAuthenticated: true,
                user: action.payload.user,
                token: action.payload.token,
                isLoading: false,
            };
        case 'LOGOUT':
            return {
                ...state,
                isAuthenticated: false,
                user: null,
                token: null,
                isLoading: false,
            };
        case 'SET_LOADING':
            return {
                ...state,
                isLoading: action.payload,
            };
        default:
            return state;
    }
};

// Context'i oluşturma
interface AuthContextType extends AuthState {
    login: (userData: Parameters<typeof authService.login>[0]) => Promise<void>; // authService.login parametre tipi
    logout: () => void;
    // register'a gerek yok, o sadece API'ye istek atıyor, state'i direkt değiştirmiyor.
}

// createContext için başlangıç değeri. undefined kullanmak yerine,
// useContext kullanırken null kontrolü yapmamızı gerektirmeyen bir yapı sağlayabiliriz.
// Ya da, başlangıçta undefined verip, Provider dışında kullanıldığında hata fırlatmasını sağlayabiliriz.
// Şimdilik, metodlar için boş fonksiyonlar ve state için initialState verelim.
const AuthContext = createContext<AuthContextType>({
    ...initialState,
    login: async () => { throw new Error("Login function not implemented or AuthProvider missing"); },
    logout: () => { throw new Error("Logout function not implemented or AuthProvider missing"); },
});

// Provider bileşeni
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
    const [state, dispatch] = useReducer(authReducer, initialState);

    // Uygulama ilk yüklendiğinde localStorage'dan token ve kullanıcı bilgilerini kontrol et
    useEffect(() => {
        const initializeAuth = () => {
            dispatch({ type: 'SET_LOADING', payload: true });
            const storedToken = localStorage.getItem('userToken');
            const storedUserDataString = localStorage.getItem('userData');

            if (storedToken && storedUserDataString) {
                try {
                    const storedUser: AuthState['user'] = JSON.parse(storedUserDataString);
                    if (storedUser && storedUser.id && storedUser.username) {
                        // Burada token'ın geçerliliğini backend'e sorarak da teyit edebiliriz (örn: /api/auth/validateToken endpoint'i)
                        // Şimdilik sadece varlığına güveniyoruz.
                        dispatch({
                            type: 'INITIALIZE',
                            payload: { isAuthenticated: true, user: storedUser, token: storedToken },
                        });
                        return;
                    }
                } catch (e) {
                    console.error("Error parsing stored user data during init:", e);
                    // Hata olursa veya veri bozuksa, temizle
                    localStorage.removeItem('userToken');
                    localStorage.removeItem('userData');
                }
            }
            dispatch({ type: 'INITIALIZE', payload: { isAuthenticated: false, user: null, token: null } });
        };
        initializeAuth();
    }, []);


    const login = async (loginData: Parameters<typeof authService.login>[0]) => {
        dispatch({ type: 'SET_LOADING', payload: true });
        try {
            const authResponse = await authService.login(loginData); // authService.login zaten localStorage'a yazıyor
            dispatch({
                type: 'LOGIN_SUCCESS',
                payload: {
                    user: { id: authResponse.id, username: authResponse.username, email: authResponse.email },
                    token: authResponse.token,
                },
            });
        } catch (error) {
            dispatch({ type: 'SET_LOADING', payload: false }); // Hata durumunda yüklemeyi bitir
            throw error; // Hatayı LoginPage'in yakalaması için tekrar fırlat
        }
    };

    const logout = () => {
        authService.logout(); // localStorage'ı temizler
        dispatch({ type: 'LOGOUT' });
        // İsteğe bağlı: Kullanıcıyı login sayfasına yönlendir
        // window.location.href = '/login'; // Veya useNavigate ile
    };

    return (
        <AuthContext.Provider value={{ ...state, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

// Context'i kullanmak için custom hook
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
