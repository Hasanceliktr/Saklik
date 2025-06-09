// src/App.tsx
import React from 'react'; // useEffect'e gerek kalmadı, isLoading'i useAuth'tan alacağız
import { BrowserRouter as Router, Routes, Route, Navigate, Outlet, useLocation } from 'react-router-dom';
import RegisterPage from './pages/RegisterPage';

import { useAuth } from './contexts/AuthContext';
import { Spin, Row, Col } from 'antd';
import LoginPage from "./pages/LoginPages.tsx";
import DashboardPage from "./pages/DashboardPage.tsx"; // Yükleme göstergesi için Ant Design bileşenleri

// Korumalı Route'ları yönetecek bileşen (Daha önce oluşturmuştuk, kontrol edelim)
const ProtectedRoute: React.FC = () => {
    const { isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return (
            <Row justify="center" align="middle" style={{ minHeight: '100vh' }}>
                <Col>
                    <Spin size="large" tip="Oturum durumu kontrol ediliyor..." />
                </Col>
            </Row>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return <Outlet />;
};

// Herkese açık route'ları (login, register) yönetecek bileşen (Daha önce oluşturmuştuk)
const PublicRoute: React.FC = () => {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) {
        return (
            <Row justify="center" align="middle" style={{ minHeight: '100vh' }}>
                <Col>
                    <Spin size="large" tip="Yükleniyor..." />
                </Col>
            </Row>
        );
    }

    if (isAuthenticated) {
        return <Navigate to="/dashboard" replace />;
    }

    return <Outlet />;
};

// Ana sayfa için yönlendirici bileşen (Daha önce oluşturmuştuk)
const AuthRedirector: React.FC = () => {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) { // Eğer AuthContext hala yükleniyorsa, bir şey gösterme veya yükleme ekranı
        return (
            <Row justify="center" align="middle" style={{ minHeight: '100vh' }}>
                <Col>
                    <Spin size="large" tip="Yönlendiriliyor..." />
                </Col>
            </Row>
        );
    }
    return isAuthenticated ? <Navigate to="/dashboard" replace /> : <Navigate to="/login" replace />;
};


function App() {
    // AuthContext'in en üst seviyede yükleme durumunu burada ayrıca ele almaya gerek yok,
    // çünkü ProtectedRoute, PublicRoute ve AuthRedirector kendi içlerinde isLoading'i zaten handle ediyor.
    // const { isLoading } = useAuth();
    // if (isLoading) {
    //     return (
    //         <Row justify="center" align="middle" style={{ minHeight: '100vh' }}>
    //             <Col><Spin size="large" tip="Uygulama Yükleniyor..." /></Col>
    //         </Row>
    //     );
    // }

    return (
        <Router>
            <Routes>
                {/* Herkese Açık Route'lar */}
                <Route element={<PublicRoute />}>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                </Route>

                {/* Korumalı Route'lar */}
                <Route element={<ProtectedRoute />}>
                    <Route path="/dashboard" element={<DashboardPage />} /> {/* DASHBOARD ROUTE'U BURADA */}
                    {/* Diğer korumalı sayfalar buraya eklenecek */}
                    {/* Örnek: <Route path="/files" element={<FilesPage />} /> */}
                </Route>

                {/* Ana Sayfa Yönlendirmesi */}
                <Route path="/" element={<AuthRedirector />} />

                {/* Bulunamayan Sayfalar İçin (404) - Opsiyonel */}
                {/* <Route path="*" element={<div>Sayfa Bulunamadı!</div>} /> */}
                {/* Veya daha şık bir 404 sayfasına yönlendirme: */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default App;