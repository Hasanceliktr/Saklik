// src/main.tsx

import React from 'react';
import ReactDOM from 'react-dom/client';
import AppRootComponent from './App.tsx'; // Senin App.tsx'teki ana bileşeninin adı
// Eğer App.tsx'teki bileşenin adı "App" ise, burası da "App" olmalı
import { AuthProvider } from './contexts/AuthContext.tsx';
import { ConfigProvider, App as AntdApp } from 'antd'; // AntdApp'i import et (App as AntdApp)
import trTR from 'antd/locale/tr_TR'; // Türkçe için (opsiyonel)

import 'antd/dist/reset.css'; // Ant Design stilleri
import './index.css';         // Kendi genel stillerin (varsa)

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');

const root = ReactDOM.createRoot(rootElement);

root.render(
    <React.StrictMode>
        <ConfigProvider locale={trTR}> {/* Genel AntD konfigürasyonu (dil vb.) */}
            <AuthProvider> {/* Bizim Auth context provider'ımız */}
                <AntdApp> {/* ANT DESIGN'IN KENDİ APP SARMALAYICISI */}
                    <AppRootComponent /> {/* Bizim ana uygulama bileşenimiz (App.tsx'ten gelen) */}
                </AntdApp>
            </AuthProvider>
        </ConfigProvider>
    </React.StrictMode>
);