// src/pages/RegisterPage.tsx
import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Spin, Row, Col } from 'antd';
import { UserOutlined, MailOutlined, LockOutlined } from '@ant-design/icons';
import authService from '../services/authService'; // API servisimizi import et
import { useNavigate } from 'react-router-dom'; // Yönlendirme için

const { Title } = Typography;

const RegisterPage: React.FC = () => {
    const [form] = Form.useForm(); // Ant Design Form hook'u
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);
    const navigate = useNavigate(); // Yönlendirme hook'u

    const onFinish = async (values: any) => {
        setLoading(true);
        setError(null);
        setSuccess(null);
        console.log('Register form values:', values); // Formdan gelen değerler
        try {
            const response = await authService.register({
                username: values.username,
                email: values.email,
                password: values.password,
            });
            setSuccess(response.message + " Giriş sayfasına yönlendiriliyorsunuz...");
            form.resetFields(); // Formu temizle
            setTimeout(() => {
                navigate('/login'); // Başarılı kayıttan sonra login sayfasına yönlendir
            }, 2000); // 2 saniye sonra
        } catch (err: any) {
            // Axios hataları genellikle err.response.data içinde mesajı içerir
            const errorMessage = err.response?.data?.message || err.message || 'Kayıt sırasında bir hata oluştu.';
            setError(errorMessage);
            console.error('Register error:', err.response || err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Row justify="center" align="middle" style={{ minHeight: '100vh', background: '#f0f2f5' }}>
            <Col xs={22} sm={16} md={12} lg={8} xl={6}>
                <Card style={{ boxShadow: '0 4px 8px 0 rgba(0,0,0,0.2)' }}>
                    <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                        <Title level={2}>Saklık - Kayıt Ol</Title>
                    </div>
                    {error && <Alert message={error} type="error" showIcon closable style={{ marginBottom: '20px' }} onClose={() => setError(null)} />}
                    {success && <Alert message={success} type="success" showIcon style={{ marginBottom: '20px' }} />}
                    <Spin spinning={loading}>
                        <Form
                            form={form}
                            name="register"
                            onFinish={onFinish}
                            layout="vertical"
                            requiredMark={false}
                        >
                            <Form.Item
                                name="username"
                                label="Kullanıcı Adı"
                                rules={[{ required: true, message: 'Lütfen kullanıcı adınızı giriniz!' }]}
                            >
                                <Input prefix={<UserOutlined />} placeholder="Kullanıcı Adı" />
                            </Form.Item>

                            <Form.Item
                                name="email"
                                label="E-posta Adresi"
                                rules={[
                                    { required: true, message: 'Lütfen e-posta adresinizi giriniz!' },
                                    { type: 'email', message: 'Geçerli bir e-posta adresi giriniz!' }
                                ]}
                            >
                                <Input prefix={<MailOutlined />} placeholder="E-posta Adresi" type="email" />
                            </Form.Item>

                            <Form.Item
                                name="password"
                                label="Şifre"
                                rules={[{ required: true, message: 'Lütfen şifrenizi giriniz!' }]}
                                hasFeedback // Şifre tekrarı ile eşleşme kontrolü için (ileride eklenebilir)
                            >
                                <Input.Password prefix={<LockOutlined />} placeholder="Şifre" />
                            </Form.Item>

                            <Form.Item
                                name="confirm"
                                label="Şifre Tekrar"
                                dependencies={['password']} // 'password' alanına bağımlı
                                hasFeedback
                                rules={[
                                    { required: true, message: 'Lütfen şifrenizi tekrar giriniz!' },
                                    ({ getFieldValue }) => ({
                                        validator(_, value) {
                                            if (!value || getFieldValue('password') === value) {
                                                return Promise.resolve();
                                            }
                                            return Promise.reject(new Error('Şifreler eşleşmiyor!'));
                                        },
                                    }),
                                ]}
                            >
                                <Input.Password prefix={<LockOutlined />} placeholder="Şifre Tekrar" />
                            </Form.Item>

                            <Form.Item>
                                <Button type="primary" htmlType="submit" loading={loading} block>
                                    Kayıt Ol
                                </Button>
                            </Form.Item>
                            <div style={{ textAlign: 'center' }}>
                                Zaten bir hesabınız var mı? <a href="/login">Giriş Yapın</a>
                            </div>
                        </Form>
                    </Spin>
                </Card>
            </Col>
        </Row>
    );
};

export default RegisterPage;