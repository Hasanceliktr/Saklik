// src/pages/LoginPage.tsx
import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message as AntMessage, Spin, Row, Col } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
// authService'i doğrudan kullanmayacağız, useAuth hook'u üzerinden login fonksiyonunu alacağız
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext'; // useAuth hook'unu import et

const { Title } = Typography;

interface LoginFormValues {
    username?: string;
    password?: string;
}

const LoginPage: React.FC = () => {
    const navigate = useNavigate();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const { login, isAuthenticated } = useAuth(); // AuthContext'ten login fonksiyonunu ve isAuthenticated state'ini al

    // Eğer kullanıcı zaten giriş yapmışsa, login sayfasına gelmemeli, dashboard'a yönlendirilmeli
    // Bu kontrolü App.tsx'te genel bir yönlendirme ile de yapabiliriz.
    // useEffect(() => {
    //     if (isAuthenticated) {
    //         navigate('/dashboard');
    //     }
    // }, [isAuthenticated, navigate]);

    const onFinish = async (values: LoginFormValues) => {
        console.log('Login form values:', values);
        setLoading(true);
        try {
            // Context'ten gelen login fonksiyonunu çağır
            await login({
                username: values.username,
                password: values.password,
            });

            AntMessage.success('Başarıyla giriş yaptınız! Ana sayfaya yönlendiriliyorsunuz...', 2);
            // Yönlendirme artık AuthContext veya App.tsx'teki merkezi bir mantıkla yapılabilir.
            // AuthContext'teki login başarılı olduğunda isAuthenticated true olacak ve App.tsx'teki
            // korumalı route'lar veya yönlendirmeler devreye girecek.
            // Şimdilik, login sonrası yönlendirme için navigate('/dashboard') veya navigate('/') kullanabiliriz.
            // En iyi pratik, App.tsx'in isAuthenticated durumuna göre yönlendirme yapmasıdır.
            // Bu yüzden buradaki navigate'i kaldırabilir veya geçici olarak bırakabiliriz.
            // AuthContext'teki login'in başarılı olması state'i güncelleyeceği için
            // App.tsx'teki useEffect veya Route'lar bu değişikliği yakalayıp yönlendirebilir.
            // Şimdilik, App.tsx'in bunu halledeceğini varsayarak buradaki navigate'i yoruma alalım.
            // setTimeout(() => {
            // navigate('/dashboard');
            // }, 100); // Çok kısa bir süre sonra (state güncellemesi yansısın diye)
            // Ya da App.tsx'e bırakalım

        } catch (error: any) {
            console.error('Login Hatası (LoginPage):', error.response?.data || error.message || error);
            let errorMessage = 'Giriş sırasında bir sunucu hatası oluştu.';
            // AuthContext'teki login fonksiyonu hatayı tekrar fırlattığı için buraya düşer.
            // Orada API'den gelen mesajı almış olmalıyız.
            if (error.response && error.response.data && error.response.data.message) {
                errorMessage = error.response.data.message;
            } else if (error.message) { // Eğer error.message varsa, onu kullanalım
                errorMessage = error.message;
            }
            AntMessage.error(errorMessage, 5);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Row justify="center" align="middle" style={{ minHeight: '100vh', background: '#f0f2f5' }}>
            <Col xs={22} sm={16} md={12} lg={8} xl={6}>
                <Card style={{ boxShadow: '0 4px 12px 0 rgba(0,0,0,0.1)', borderRadius: '8px' }}>
                    <div style={{ textAlign: 'center', marginBottom: '30px' }}>
                        <Title level={2} style={{ margin: 0 }}>Giriş Yap</Title>
                        <Typography.Text type="secondary">Saklık hesabınıza erişin.</Typography.Text>
                    </div>
                    <Spin spinning={loading} tip="Giriş Yapılıyor...">
                        <Form
                            form={form}
                            name="login"
                            onFinish={onFinish}
                            layout="vertical"
                            requiredMark="optional"
                        >
                            <Form.Item
                                name="username"
                                label="Kullanıcı Adı veya E-posta"
                                rules={[{ required: true, message: 'Lütfen kullanıcı adınızı veya e-postanızı girin!' }]}
                            >
                                <Input prefix={<UserOutlined />} placeholder="Kullanıcı adı / E-posta" size="large" />
                            </Form.Item>

                            <Form.Item
                                name="password"
                                label="Şifre"
                                rules={[{ required: true, message: 'Lütfen şifrenizi girin!' }]}
                            >
                                <Input.Password prefix={<LockOutlined />} placeholder="Şifre" size="large" />
                            </Form.Item>

                            <Form.Item style={{marginTop: '24px'}}>
                                <Button type="primary" htmlType="submit" block loading={loading} size="large">
                                    Giriş Yap
                                </Button>
                            </Form.Item>
                            <div style={{ textAlign: 'center', marginTop: '16px' }}>
                                Hesabınız yok mu? <a onClick={() => navigate('/register')} style={{fontWeight: 'bold'}}>Kayıt Olun</a>
                            </div>
                        </Form>
                    </Spin>
                </Card>
            </Col>
        </Row>
    );
};

export default LoginPage;